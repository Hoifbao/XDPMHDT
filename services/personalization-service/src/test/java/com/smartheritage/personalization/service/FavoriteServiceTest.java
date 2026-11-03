package com.smartheritage.personalization.service;

import com.smartheritage.personalization.client.ArtifactClient;
import com.smartheritage.personalization.entity.FavoriteId;
import com.smartheritage.personalization.repository.FavoriteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {
    @Mock FavoriteRepository favoriteRepository;
    @Mock ArtifactClient artifactClient;
    @InjectMocks FavoriteService favoriteService;

    @Test
    void repeatedAddDoesNotInsertSecondRow() {
        when(favoriteRepository.existsById(new FavoriteId(2L, 1L))).thenReturn(true);
        favoriteService.add(2L, 1L);
        verify(artifactClient).requireActive(1L);
        verify(favoriteRepository, never()).saveAndFlush(any());
    }
}
