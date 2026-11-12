package com.smartheritage.content;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartheritage.common.events.EventEnvelope;
import com.smartheritage.common.events.Events;
import com.smartheritage.content.client.SiteClient;
import com.smartheritage.content.dto.ArtifactRequest;
import com.smartheritage.content.events.*;
import com.smartheritage.content.repository.*;
import com.smartheritage.content.service.ArtifactService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Real transport + common EventPublisher + H2 outbox; only broker responses are simulated. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:confirmed;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.events.retry-delay-ms=3600000", "app.events.confirm-timeout-ms=20"
})
class ArtifactConfirmedDeliveryTest {
    @Autowired ArtifactService service;
    @Autowired ArtifactEventDelivery delivery;
    @Autowired ArtifactEventOutboxRepository outbox;
    @Autowired ArtifactRepository artifacts;
    @Autowired ArtifactContentRepository contents;
    @Autowired MediaResourceRepository media;
    @Autowired ObjectMapper json;
    @MockitoBean RabbitTemplate rabbit;
    @MockitoBean SiteClient sites;

    @BeforeEach
    void clean() {
        outbox.deleteAll(); media.deleteAll(); contents.deleteAll(); artifacts.deleteAll();
        reset(rabbit, sites);
        when(sites.requireSite(1L)).thenReturn(new SiteClient.Site(1L, "Museum"));
        when(sites.findName(anyLong())).thenReturn(Optional.of("Museum"));
    }

    private void answer(boolean ack, boolean returned) {
        doAnswer(call -> {
            CorrelationData correlation = call.getArgument(3);
            if (returned) correlation.setReturned(new ReturnedMessage(new Message(new byte[0], new MessageProperties()),
                    312, "NO_ROUTE", Events.EXCHANGE, RabbitArtifactEventTransport.ROUTING_KEY));
            correlation.getFuture().complete(new CorrelationData.Confirm(ack, ack ? null : "nack"));
            return null;
        }).when(rabbit).convertAndSend(eq(Events.EXCHANGE), eq("artifact.updated"), any(Object.class), any(CorrelationData.class));
    }

    private ArtifactEventOutbox create() throws Exception {
        service.createArtifact(json.readValue(ArtifactApiTest.VI, ArtifactRequest.class));
        return outbox.findAll().get(0);
    }

    @Test
    void ackPublishesCommonEnvelopeWithStoredIdentityAndPayload() throws Exception {
        answer(true, false);
        var entry = create();
        var sent = ArgumentCaptor.forClass(Object.class);
        verify(rabbit).convertAndSend(eq("sh.events"), eq("artifact.updated"), sent.capture(), any(CorrelationData.class));
        assertThat(sent.getValue()).isInstanceOf(EventEnvelope.class);
        EventEnvelope<?> envelope = (EventEnvelope<?>) sent.getValue();
        assertThat(envelope.eventId().toString()).isEqualTo(entry.getEventId());
        assertThat(envelope.occurredAt()).isEqualTo(entry.getOccurredAt());
        assertThat(envelope.type()).isEqualTo("ArtifactUpdated");
        assertThat(envelope.version()).isEqualTo(EventEnvelope.CURRENT_VERSION);
        assertThat(envelope.data()).isEqualTo(json.readValue(entry.getPayload(), ArtifactUpdated.class));
        assertThat(entry.getPublishedAt()).isNotNull();
    }

    @Test
    void nackKeepsCommittedEntryPendingUntilConfirmedRetry() throws Exception {
        answer(false, false);
        var entry = create();
        assertThat(entry.getPublishedAt()).isNull();
        assertThat(artifacts.count()).isEqualTo(1);
        answer(true, false);
        delivery.deliver(entry.getEventId());
        var sent = ArgumentCaptor.forClass(Object.class);
        verify(rabbit, times(2)).convertAndSend(eq("sh.events"), eq("artifact.updated"), sent.capture(), any(CorrelationData.class));
        assertThat(sent.getAllValues().get(0)).isEqualTo(sent.getAllValues().get(1));
        assertThat(outbox.findById(entry.getEventId()).orElseThrow().getPublishedAt()).isNotNull();
    }

    @Test
    void returnedMessageKeepsEntryPendingEvenWithAck() throws Exception {
        answer(true, true);
        assertThat(create().getPublishedAt()).isNull();
    }

    @Test
    void confirmTimeoutKeepsEntryPending() throws Exception {
        assertThat(create().getPublishedAt()).isNull();
    }
}
