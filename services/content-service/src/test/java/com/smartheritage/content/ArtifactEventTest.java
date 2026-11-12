package com.smartheritage.content;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartheritage.content.client.SiteClient;
import com.smartheritage.content.dto.*;
import com.smartheritage.content.events.*;
import com.smartheritage.content.repository.*;
import com.smartheritage.content.service.ArtifactService;
import com.smartheritage.content.service.MediaResourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:events;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop", "app.events.retry-delay-ms=3600000"
})
class ArtifactEventTest {
    @Autowired ArtifactService service;
    @Autowired MediaResourceService mediaService;
    @Autowired ArtifactEventOutboxRepository outbox;
    @Autowired ArtifactEventDelivery delivery;
    @Autowired ArtifactRepository artifacts;
    @Autowired ArtifactContentRepository contents;
    @Autowired MediaResourceRepository media;
    @Autowired ObjectMapper json;
    @Autowired PlatformTransactionManager transactions;
    @MockitoBean ArtifactEventTransport transport;
    @MockitoBean SiteClient sites;

    @BeforeEach
    void clean() {
        outbox.deleteAll();
        media.deleteAll();
        contents.deleteAll();
        artifacts.deleteAll();
        reset(transport, sites);
        when(sites.requireSite(anyLong())).thenAnswer(call -> new SiteClient.Site(call.getArgument(0), "Museum"));
        when(sites.findName(anyLong())).thenReturn(Optional.of("Museum"));
    }

    ArtifactResponse create() throws Exception {
        return service.createArtifact(json.readValue(ArtifactApiTest.VI, ArtifactRequest.class));
    }

    @Test
    void sendsOnlyAfterCommitAndUsesCommittedArtifact() {
        doAnswer(call -> {
            ArtifactUpdated data = call.getArgument(2);
            assertTrue(artifacts.existsById(data.artifactId()));
            return null;
        }).when(transport).send(anyString(), any(), any());
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            try { create(); } catch (Exception e) { throw new RuntimeException(e); }
            assertEquals(1, outbox.count());
            verifyNoInteractions(transport);
        });
        var payload = ArgumentCaptor.forClass(ArtifactUpdated.class);
        verify(transport).send(anyString(), any(Instant.class), payload.capture());
        assertEquals(1L, payload.getValue().siteId());
        assertTrue(payload.getValue().changedFields().contains("created"));
        var entry = outbox.findAll().get(0);
        assertDoesNotThrow(() -> UUID.fromString(entry.getEventId()));
        assertNotNull(entry.getPublishedAt());
    }

    @Test
    void rollbackRemovesArtifactAndOutboxAndDoesNotSend() {
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            try { create(); } catch (Exception e) { throw new RuntimeException(e); }
            tx.setRollbackOnly();
        });
        assertEquals(0, artifacts.count());
        assertEquals(0, outbox.count());
        verifyNoInteractions(transport);
    }

    @Test
    void updateReportsActualFieldsAndNoOpProducesNoEvent() throws Exception {
        long id = create().getId();
        reset(transport);
        var edit = new ArtifactRequest();
        edit.setName("New name");
        edit.setBeaconUuid("beacon-w6");
        service.updateArtifact(id, edit);
        var payload = ArgumentCaptor.forClass(ArtifactUpdated.class);
        verify(transport).send(anyString(), any(), payload.capture());
        assertEquals(java.util.List.of("name", "beaconUuid"), payload.getValue().changedFields());
        reset(transport);
        service.updateArtifact(id, edit);
        verifyNoInteractions(transport);
        assertEquals(2, outbox.count());
    }

    @Test
    void replacingTranslationsAndMediaProducesOneEvent() throws Exception {
        long id = create().getId();
        reset(transport);
        var request = json.readValue("""
                {"contents":[{"languageCode":"vi","title":"Changed","description":"Changed"}],"media":[]}
                """, ArtifactRequest.class);
        service.updateArtifact(id, request);
        var payload = ArgumentCaptor.forClass(ArtifactUpdated.class);
        verify(transport).send(anyString(), any(), payload.capture());
        assertEquals(java.util.List.of("contents", "media"), payload.getValue().changedFields());
    }

    @Test
    void standaloneTranslationAndMediaMutationsAndDeleteAreCovered() throws Exception {
        long id = create().getId();
        reset(transport);
        var translation = json.readValue("""
                {"languageCode":"en","title":"English","description":"English description"}
                """, ArtifactContentRequest.class);
        long translationId = service.upsertContent(id, translation).getId();
        service.deleteContent(translationId);
        var request = json.readValue("""
                {"mediaType":"IMAGE","fileUrl":"https://example.com/image.png"}
                """, MediaResourceRequest.class);
        long mediaId = mediaService.create(id, request).getId();
        mediaService.delete(mediaId);
        service.deleteArtifact(id);
        var payload = ArgumentCaptor.forClass(ArtifactUpdated.class);
        verify(transport, times(5)).send(anyString(), any(), payload.capture());
        assertEquals(java.util.List.of("contents"), payload.getAllValues().get(0).changedFields());
        assertEquals(java.util.List.of("contents"), payload.getAllValues().get(1).changedFields());
        assertEquals(java.util.List.of("media"), payload.getAllValues().get(2).changedFields());
        assertEquals(java.util.List.of("media"), payload.getAllValues().get(3).changedFields());
        assertTrue(payload.getAllValues().get(4).changedFields().contains("deleted"));
        assertFalse(artifacts.existsById(id));
        assertEquals(6, outbox.count());
    }

    @Test
    void failedDeliveryRemainsPendingAndRetryKeepsEventId() throws Exception {
        doThrow(new IllegalStateException("broker unavailable")).when(transport).send(anyString(), any(), any());
        long artifactId = create().getId();
        assertTrue(artifacts.existsById(artifactId));
        var entry = outbox.findAll().get(0);
        assertNull(entry.getPublishedAt());
        doNothing().when(transport).send(anyString(), any(), any());
        delivery.deliver(entry.getEventId());
        delivery.deliver(entry.getEventId()); // Already published: do not send again.
        verify(transport, times(2)).send(eq(entry.getEventId()), eq(entry.getOccurredAt()), any());
        assertNotNull(outbox.findById(entry.getEventId()).orElseThrow().getPublishedAt());
    }
}
