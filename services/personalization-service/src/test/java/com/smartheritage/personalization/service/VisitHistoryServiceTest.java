package com.smartheritage.personalization.service;

import com.smartheritage.personalization.client.ArtifactClient;
import com.smartheritage.personalization.dto.RecordVisitRequest;
import com.smartheritage.personalization.entity.VisitHistory;
import com.smartheritage.personalization.repository.VisitHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VisitHistoryServiceTest {
    @Mock VisitHistoryRepository visitHistoryRepository;
    @Mock ArtifactClient artifactClient;
    @InjectMocks VisitHistoryService visitHistoryService;

    @Test
    void duplicateClientEventIdIsNotInsertedAgain() {
        RecordVisitRequest request = request("550e8400-e29b-41d4-a716-446655440000");
        when(visitHistoryRepository.existsByUserIdAndClientEventId(2L, request.getClientEventId())).thenReturn(true);
        assertFalse(visitHistoryService.record(2L, request));
        verify(visitHistoryRepository, never()).save(any());
        verifyNoInteractions(artifactClient);
    }

    @Test
    void missingDurationIsStoredAsNull() {
        RecordVisitRequest request = request("550e8400-e29b-41d4-a716-446655440001");
        when(visitHistoryRepository.existsByUserIdAndClientEventId(2L, request.getClientEventId())).thenReturn(false);
        assertTrue(visitHistoryService.record(2L, request));
        ArgumentCaptor<VisitHistory> saved = ArgumentCaptor.forClass(VisitHistory.class);
        verify(visitHistoryRepository).save(saved.capture());
        verify(artifactClient).requireActive(1L);
        assertNull(saved.getValue().getDurationSeconds());
    }

    @Test
    void detectedAtUtcIsConvertedToServerTime() {
        RecordVisitRequest request = request("550e8400-e29b-41d4-a716-446655440002");
        OffsetDateTime sentUtc = OffsetDateTime.of(2026, 10, 30, 2, 0, 0, 0, ZoneOffset.UTC);
        request.setDetectedAt(sentUtc);
        when(visitHistoryRepository.existsByUserIdAndClientEventId(2L, request.getClientEventId())).thenReturn(false);
        visitHistoryService.record(2L, request);
        ArgumentCaptor<VisitHistory> saved = ArgumentCaptor.forClass(VisitHistory.class);
        verify(visitHistoryRepository).save(saved.capture());
        assertEquals(sentUtc.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime(), saved.getValue().getDetectedAt());
    }

    private static RecordVisitRequest request(String eventId) {
        RecordVisitRequest request = new RecordVisitRequest();
        request.setArtifactId(1L);
        request.setSource("BEACON");
        request.setClientEventId(eventId);
        return request;
    }
}
