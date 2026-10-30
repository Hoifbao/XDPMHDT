package com.smartheritage.backend.service;

import com.smartheritage.backend.entity.Artifact;
import com.smartheritage.backend.entity.Favorite;
import com.smartheritage.backend.entity.User;
import com.smartheritage.backend.repository.ArtifactRepository;
import com.smartheritage.backend.repository.FavoriteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {
    @Mock FavoriteRepository favoriteRepository;
    @Mock ArtifactRepository artifactRepository;
    @InjectMocks FavoriteService favoriteService;

    @Test
    void addFavoriteIsIdempotentWhenLikeAlreadyExists() {
        User user = User.builder().id(10L).username("visitor_an").build();
        Artifact artifact = Artifact.builder().id(1L).status("ACTIVE").build();
        when(artifactRepository.findById(1L)).thenReturn(Optional.of(artifact));
        when(favoriteRepository.existsById(any())).thenReturn(true);

        favoriteService.add(user, 1L);

        verify(favoriteRepository, never()).save(any(Favorite.class));
    }

    @Test
    void addFavoritePersistsOneLikeWhenMissing() {
        User user = User.builder().id(10L).username("visitor_an").build();
        Artifact artifact = Artifact.builder().id(1L).status("ACTIVE").build();
        when(artifactRepository.findById(1L)).thenReturn(Optional.of(artifact));
        when(favoriteRepository.existsById(any())).thenReturn(false);

        favoriteService.add(user, 1L);

        verify(favoriteRepository, times(1)).save(any(Favorite.class));
    }
}
