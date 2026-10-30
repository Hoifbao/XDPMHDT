package com.smartheritage.backend.service;

import com.smartheritage.backend.dto.RecordVisitRequest;
import com.smartheritage.backend.entity.User;
import com.smartheritage.backend.repository.ArtifactRepository;
import com.smartheritage.backend.repository.VisitHistoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
