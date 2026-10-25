package com.smartheritage.backend.service;

import com.smartheritage.backend.dto.FavoriteResponse;
import com.smartheritage.backend.dto.VisitHistoryResponse;
import com.smartheritage.backend.dto.VisitRequest;
import com.smartheritage.backend.entity.*;
import com.smartheritage.backend.repository.ArtifactRepository;
import com.smartheritage.backend.repository.FavoriteRepository;
import com.smartheritage.backend.repository.UserRepository;
import com.smartheritage.backend.repository.VisitHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Cá nhân hoá (Phú): hiện vật yêu thích (bảng likes) và lịch sử tham quan (bảng visits).
 *
 * Tuần 2 chưa có JWT nên userId truyền qua URL.
 * Tuần 3 khi có JWT (Bảo) sẽ đổi sang lấy user từ token (/api/me/...).
 */
@Service
@RequiredArgsConstructor
public class PersonalizationService {

    private final FavoriteRepository favoriteRepository;
    private final VisitHistoryRepository visitHistoryRepository;
    private final UserRepository userRepository;
    private final ArtifactRepository artifactRepository;

    // ───────── Yêu thích ─────────

    @Transactional(readOnly = true)
    public List<FavoriteResponse> getFavorites(Long userId) {
        requireUser(userId);
        return favoriteRepository.findByUser_IdOrderBySavedAtDesc(userId).stream()
                .map(this::toFavoriteResponse)
                .toList();
    }

    /** Thêm yêu thích; gọi lại nhiều lần không tạo dòng trùng. */
    @Transactional
    public FavoriteResponse addFavorite(Long userId, Long artifactId) {
        FavoriteId id = new FavoriteId(userId, artifactId);
        Favorite favorite = favoriteRepository.findById(id).orElseGet(() ->
                favoriteRepository.save(Favorite.builder()
                        .id(id)
                        .user(requireUser(userId))
                        .artifact(requireArtifact(artifactId))
                        .build()));
        return toFavoriteResponse(favorite);
    }

    /** Bỏ yêu thích; xoá hiện vật chưa thích cũng coi là thành công. */
    @Transactional
    public void removeFavorite(Long userId, Long artifactId) {
        FavoriteId id = new FavoriteId(userId, artifactId);
        if (favoriteRepository.existsById(id)) {
            favoriteRepository.deleteById(id);
        }
    }

    @Transactional(readOnly = true)
    public boolean isFavorite(Long userId, Long artifactId) {
        return favoriteRepository.existsById(new FavoriteId(userId, artifactId));
    }

    // ───────── Lịch sử tham quan ─────────

    @Transactional(readOnly = true)
    public List<VisitHistoryResponse> getVisits(Long userId) {
        requireUser(userId);
        return visitHistoryRepository.findByUser_IdOrderByDetectedAtDesc(userId).stream()
                .map(this::toVisitResponse)
                .toList();
    }

    @Transactional
    public VisitHistoryResponse recordVisit(Long userId, VisitRequest request) {
        if (request.getArtifactId() == null) {
            throw new IllegalArgumentException("artifactId không được để trống");
        }
        int duration = request.getDurationSeconds() == null ? 0 : request.getDurationSeconds();
        if (duration < 0) {
            throw new IllegalArgumentException("durationSeconds phải >= 0");
        }

        VisitHistory visit = visitHistoryRepository.save(VisitHistory.builder()
                .user(requireUser(userId))
                .artifact(requireArtifact(request.getArtifactId()))
                .detectedAt(request.getDetectedAt() != null ? request.getDetectedAt() : LocalDateTime.now())
                .durationSeconds(duration)
                .build());
        return toVisitResponse(visit);
    }

    // ───────── Hàm phụ ─────────

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với ID: " + userId));
    }

    private Artifact requireArtifact(Long artifactId) {
        return artifactRepository.findById(artifactId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hiện vật với ID: " + artifactId));
    }

    private FavoriteResponse toFavoriteResponse(Favorite favorite) {
        Artifact artifact = favorite.getArtifact();
        HeritageSite site = artifact.getSite();
        return FavoriteResponse.builder()
                .artifactId(artifact.getId())
                .artifactName(artifact.getName())
                .thumbnailUrl(artifact.getThumbnailUrl())
                .siteId(site != null ? site.getId() : null)
                .siteName(site != null ? site.getName() : null)
                .savedAt(favorite.getSavedAt())
                .build();
    }

    private VisitHistoryResponse toVisitResponse(VisitHistory visit) {
        Artifact artifact = visit.getArtifact();
        HeritageSite site = artifact.getSite();
        return VisitHistoryResponse.builder()
                .id(visit.getId())
                .artifactId(artifact.getId())
                .artifactName(artifact.getName())
                .thumbnailUrl(artifact.getThumbnailUrl())
                .siteName(site != null ? site.getName() : null)
                .detectedAt(visit.getDetectedAt())
                .durationSeconds(visit.getDurationSeconds())
                .build();
    }
}
