package com.smartheritage.beacon.service;

import com.smartheritage.beacon.client.ArtifactClient;
import com.smartheritage.beacon.events.ArtifactUpdatedData;
import com.smartheritage.common.events.EventEnvelope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test nhận ArtifactUpdated -> hủy cache registry - Khoa, W6.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RegistryEventListenerTest {

    @Mock ProcessedEvents processedEvents;
    @Mock ArtifactClient artifactClient;

    private RegistryEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new RegistryEventListener(processedEvents, artifactClient);
        when(processedEvents.markIfNew(any(UUID.class), anyString())).thenReturn(true);
    }

    private EventEnvelope<ArtifactUpdatedData> event(Long artifactId) {
        return EventEnvelope.of(UUID.randomUUID(), "artifact.updated", Instant.now(),
                new ArtifactUpdatedData(artifactId, 1L));
    }

    @Test
    void invalidatesCacheForChangedArtifact() {
        listener.onArtifactUpdated(event(9L));

        verify(artifactClient).invalidate(9L);
    }

    @Test
    void duplicateEventIsSkipped() {
        when(processedEvents.markIfNew(any(UUID.class), anyString())).thenReturn(false);

        listener.onArtifactUpdated(event(9L));

        verify(artifactClient, never()).invalidate(any());
    }

    @Test
    void missingArtifactIdGoesToDlq() {
        EventEnvelope<ArtifactUpdatedData> broken = EventEnvelope.of(UUID.randomUUID(),
                "artifact.updated", Instant.now(), new ArtifactUpdatedData(null, 1L));

        assertThatThrownBy(() -> listener.onArtifactUpdated(broken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("thiếu data.artifactId");
        verify(artifactClient, never()).invalidate(any());
    }
}
