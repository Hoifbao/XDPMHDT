package com.smartheritage.backend.service;

import com.smartheritage.backend.dto.RecordVisitRequest;
import com.smartheritage.backend.entity.Artifact;
import com.smartheritage.backend.entity.User;
import com.smartheritage.backend.entity.VisitHistory;
import com.smartheritage.backend.repository.ArtifactRepository;
import com.smartheritage.backend.repository.VisitHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VisitHistoryServiceTest {
    @Mock VisitHistoryRepository visitHistoryRepository;
    @Mock ArtifactRepository artifactRepository;
    @InjectMocks VisitHistoryService visitHistoryService;

    @Test
    void duplicateClientEventIdIsNotInsertedAgain() {
        User user = User.builder().id(10L).username("visitor_an").build();
        RecordVisitRequest request = new RecordVisitRequest();
        request.setArtifactId(1L);
        request.setDurationSeconds(10);
        request.setSource("MANUAL");
        request.setClientEventId("550e8400-e29b-41d4-a716-446655440000");
        when(visitHistoryRepository.existsByUser_IdAndClientEventId(10L, request.getClientEventId())).thenReturn(true);

        assertFalse(visitHistoryService.record(user, request));
        verify(visitHistoryRepository, never()).save(any());
        verifyNoInteractions(artifactRepository);
    }

    /** Bảo W3: không gửi durationSeconds thì lưu NULL (không phải 0) để AVG thống kê không bị kéo xuống. */
    @Test
    void missingDurationIsStoredAsNull() {
        User user = User.builder().id(10L).username("visitor_an").build();
        RecordVisitRequest request = new RecordVisitRequest();
        request.setArtifactId(1L);
        request.setSource("BEACON");
        request.setClientEventId("550e8400-e29b-41d4-a716-446655440001");
        when(visitHistoryRepository.existsByUser_IdAndClientEventId(10L, request.getClientEventId())).thenReturn(false);
        when(artifactRepository.findById(1L)).thenReturn(Optional.of(Artifact.builder().id(1L).status("ACTIVE").build()));

        assertTrue(visitHistoryService.record(user, request));

        ArgumentCaptor<VisitHistory> saved = ArgumentCaptor.forClass(VisitHistory.class);
        verify(visitHistoryRepository).save(saved.capture());
        assertNull(saved.getValue().getDurationSeconds());
    }
}
