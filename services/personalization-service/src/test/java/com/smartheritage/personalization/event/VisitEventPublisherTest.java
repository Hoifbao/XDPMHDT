package com.smartheritage.personalization.event;

import com.smartheritage.common.events.EventPublisher;
import com.smartheritage.personalization.entity.VisitHistory;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class VisitEventPublisherTest {
    @Test
    void visitRecordedMatchesAnalyticsPayloadAndRoutingKey() {
        EventPublisher shared = mock(EventPublisher.class);
        VisitEventPublisher publisher = new VisitEventPublisher(shared, new SimpleMeterRegistry());
        LocalDateTime vietnamDate = LocalDateTime.of(2026, 11, 12, 20, 30);
        VisitHistory visit = VisitHistory.builder()
                .id(42L).userId(7L).artifactId(3L)
                .detectedAt(vietnamDate).durationSeconds(75)
                .source("BEACON").clientEventId("uuid").build();

        publisher.publishRecorded(visit);

        ArgumentCaptor<VisitEventData> capture = ArgumentCaptor.forClass(VisitEventData.class);
        verify(shared).publishAfterCommit(eq("visit.recorded"), eq("VisitRecorded"), capture.capture());
        VisitEventData data = capture.getValue();
        assertEquals(42L, data.visitId());
        assertEquals(3L, data.artifactId());
        assertEquals(vietnamDate, data.detectedAt());
    }
}
