package com.smartheritage.content;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartheritage.common.events.Events;
import com.smartheritage.content.client.SiteClient;
import com.smartheritage.content.dto.ArtifactRequest;
import com.smartheritage.content.events.ArtifactEventOutboxRepository;
import com.smartheritage.content.service.ArtifactService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/** Opt-in real RabbitMQ acceptance; H2 and a private temporary queue keep shared data untouched. */
@EnabledIfEnvironmentVariable(named = "SM_RUN_RABBIT_TESTS", matches = "true")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:rabbit-it;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
        "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.events.retry-delay-ms=3600000"
})
class ArtifactRabbitIT {
    @Autowired ArtifactService service;
    @Autowired ArtifactEventOutboxRepository outbox;
    @Autowired RabbitTemplate rabbit;
    @Autowired ObjectMapper json;
    @MockitoBean SiteClient sites;
    private RabbitAdmin admin;
    private String queue;

    @BeforeEach
    void bindTemporaryQueue() {
        admin = new RabbitAdmin(rabbit.getConnectionFactory());
        queue = "xa.w6.acceptance." + UUID.randomUUID();
        admin.declareExchange(new TopicExchange(Events.EXCHANGE, true, false));
        admin.declareQueue(new Queue(queue, false, true, true));
        admin.declareBinding(new Binding(queue, Binding.DestinationType.QUEUE,
                Events.EXCHANGE, "artifact.updated", null));
        when(sites.requireSite(1L)).thenReturn(new SiteClient.Site(1L, "Museum"));
        when(sites.findName(anyLong())).thenReturn(Optional.of("Museum"));
    }

    @AfterEach
    void deleteTemporaryQueue() {
        if (admin != null && queue != null) admin.deleteQueue(queue);
    }

    @Test
    void committedMutationReachesBrokerAndMarksOutboxOnlyAfterConfirm() throws Exception {
        var artifact = service.createArtifact(json.readValue(ArtifactApiTest.VI, ArtifactRequest.class));
        Message message = rabbit.receive(queue, 5000);
        assertThat(message).isNotNull();
        var envelope = json.readTree(message.getBody());
        var entry = outbox.findAll().get(0);
        assertThat(envelope.path("eventId").asText()).isEqualTo(entry.getEventId());
        assertThat(envelope.path("type").asText()).isEqualTo("ArtifactUpdated");
        assertThat(envelope.path("version").asInt()).isEqualTo(1);
        assertThat(envelope.path("occurredAt").asText()).isNotBlank();
        assertThat(envelope.path("data").path("artifactId").asLong()).isEqualTo(artifact.getId());
        assertThat(envelope.path("data").path("siteId").asLong()).isEqualTo(1);
        assertThat(envelope.path("data").path("changedFields").toString()).contains("created");
        assertThat(message.getMessageProperties().getHeaders()).doesNotContainKey("__TypeId__");
        assertThat(entry.getPublishedAt()).isNotNull();
    }
}
