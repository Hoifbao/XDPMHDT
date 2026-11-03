package com.smartheritage.personalization.service;

import com.smartheritage.personalization.client.ArtifactClient;
import com.smartheritage.personalization.client.ArtifactClient.ArtifactSnapshot;
import com.smartheritage.personalization.dto.FavoriteResponse;
import com.smartheritage.personalization.entity.Favorite;
import com.smartheritage.personalization.entity.FavoriteId;
import com.smartheritage.personalization.repository.FavoriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FavoriteService {
    private final FavoriteRepository favoriteRepository;
    private final ArtifactClient artifactClient;

    @Transactional(readOnly = true)
    public Page<FavoriteResponse> list(Long userId, Pageable pageable) {
        Page<Favorite> page = favoriteRepository.findByIdUserIdOrderBySavedAtDesc(userId, pageable);
        Map<Long, ArtifactSnapshot> artifacts = artifactClient.getMany(
                page.getContent().stream().map(f -> f.getId().getArtifactId()).toList());
        return page.map(f -> toResponse(f, artifacts.get(f.getId().getArtifactId())));
    }

    /** Idempotent: PK (user_id, artifact_id) và catch race-condition cùng bảo đảm chỉ một dòng. */
    @Transactional
    public void add(Long userId, Long artifactId) {
        artifactClient.requireActive(artifactId);
        FavoriteId id = new FavoriteId(userId, artifactId);
        if (favoriteRepository.existsById(id)) return;
        try {
            favoriteRepository.saveAndFlush(Favorite.builder()
                    .id(id)
                    .savedAt(LocalDateTime.now())
                    .build());
        } catch (DataIntegrityViolationException ignored) {
            // Hai request đồng thời có thể cùng qua exists; composite PK vẫn giữ idempotent.
        }
    }

    @Transactional
    public void remove(Long userId, Long artifactId) {
        favoriteRepository.deleteById(new FavoriteId(userId, artifactId));
    }

    @Transactional(readOnly = true)
    public boolean exists(Long userId, Long artifactId) {
        return favoriteRepository.existsById(new FavoriteId(userId, artifactId));
    }

    private FavoriteResponse toResponse(Favorite favorite, ArtifactSnapshot artifact) {
        Long artifactId = favorite.getId().getArtifactId();
        return new FavoriteResponse(
                artifactId,
                artifact == null || artifact.name() == null ? "Hiện vật #" + artifactId : artifact.name(),
                artifact == null ? null : artifact.thumbnailUrl(),
                artifact == null ? null : artifact.siteId(),
                artifact == null ? null : artifact.siteName(),
                favorite.getSavedAt());
    }
}
