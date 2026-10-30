package com.smartheritage.backend.service;

import com.smartheritage.backend.dto.FavoriteResponse;
import com.smartheritage.backend.entity.Artifact;
import com.smartheritage.backend.entity.Favorite;
import com.smartheritage.backend.entity.FavoriteId;
import com.smartheritage.backend.entity.User;
import com.smartheritage.backend.repository.ArtifactRepository;
import com.smartheritage.backend.repository.FavoriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class FavoriteService {
    private final FavoriteRepository favoriteRepository;
    private final ArtifactRepository artifactRepository;

    @Transactional(readOnly = true)
    public Page<FavoriteResponse> list(Long userId, Pageable pageable) {
        return favoriteRepository.findResponsesByUserId(userId, pageable);
    }

    /** Idempotent: repeated add requests keep one row because likes has a composite PK. */
    @Transactional
    public void add(User user, Long artifactId) {
        Artifact artifact = artifactRepository.findById(artifactId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hiện vật."));
        if (!"ACTIVE".equalsIgnoreCase(artifact.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Hiện vật hiện không khả dụng.");
        }
        FavoriteId id = new FavoriteId(user.getId(), artifactId);
        if (favoriteRepository.existsById(id)) return;

        Favorite favorite = new Favorite();
        favorite.setId(id);
        favorite.setUser(user);
        favorite.setArtifact(artifact);
        favorite.setSavedAt(LocalDateTime.now());
        favoriteRepository.save(favorite);
    }

    /** Idempotent: deleting an absent row is still successful. */
    @Transactional
    public void remove(Long userId, Long artifactId) {
        favoriteRepository.deleteById(new FavoriteId(userId, artifactId));
    }

    @Transactional(readOnly = true)
    public boolean exists(Long userId, Long artifactId) {
        return favoriteRepository.existsByUser_IdAndArtifact_Id(userId, artifactId);
    }
}
