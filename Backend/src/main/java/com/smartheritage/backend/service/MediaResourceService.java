package com.smartheritage.backend.service;

import com.smartheritage.backend.dto.MediaResourceRequest;
import com.smartheritage.backend.dto.MediaResourceResponse;
import com.smartheritage.backend.entity.MediaResource;
import com.smartheritage.backend.repository.ArtifactRepository;
import com.smartheritage.backend.repository.MediaResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Nghiệp vụ tài nguyên đa phương tiện (bảng media_resources) của hiện vật. */
@Service
@RequiredArgsConstructor
public class MediaResourceService {

    private static final Set<String> MEDIA_TYPES = Set.of("IMAGE", "AUDIO", "VIDEO");

    private final MediaResourceRepository mediaResourceRepository;
    private final ArtifactRepository artifactRepository;

    @Transactional(readOnly = true)
    public List<MediaResourceResponse> getByArtifact(Long artifactId) {
        requireArtifact(artifactId);
        return mediaResourceRepository.findByArtifactId(artifactId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public MediaResourceResponse create(Long artifactId, MediaResourceRequest request) {
        requireArtifact(artifactId);

        String type = request.getMediaType() == null
                ? "" : request.getMediaType().trim().toUpperCase(Locale.ROOT);
        if (!MEDIA_TYPES.contains(type)) {
            throw new IllegalArgumentException("mediaType phải là IMAGE, AUDIO hoặc VIDEO");
        }
        if (request.getFileUrl() == null || request.getFileUrl().isBlank()) {
            throw new IllegalArgumentException("fileUrl không được để trống");
        }

        MediaResource saved = mediaResourceRepository.save(MediaResource.builder()
                .artifactId(artifactId)
                .mediaType(type)
                .fileUrl(request.getFileUrl().trim())
                .caption(blankToNull(request.getCaption()))
                .build());
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long mediaId) {
        if (!mediaResourceRepository.existsById(mediaId)) {
            throw new IllegalArgumentException("Không tìm thấy tài nguyên với ID: " + mediaId);
        }
        mediaResourceRepository.deleteById(mediaId);
    }

    private void requireArtifact(Long artifactId) {
        if (artifactId == null || !artifactRepository.existsById(artifactId)) {
            throw new IllegalArgumentException("Không tìm thấy hiện vật với ID: " + artifactId);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private MediaResourceResponse toResponse(MediaResource media) {
        return MediaResourceResponse.builder()
                .id(media.getId())
                .artifactId(media.getArtifactId())
                .mediaType(media.getMediaType())
                .fileUrl(media.getFileUrl())
                .caption(media.getCaption())
                .build();
    }
}
