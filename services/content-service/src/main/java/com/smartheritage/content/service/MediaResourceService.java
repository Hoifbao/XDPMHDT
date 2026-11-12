package com.smartheritage.content.service;

import com.smartheritage.common.web.ApiException;
import com.smartheritage.content.dto.MediaResourceRequest;
import com.smartheritage.content.dto.MediaResourceResponse;
import com.smartheritage.content.entity.MediaResource;
import com.smartheritage.content.entity.Artifact;
import com.smartheritage.content.events.ArtifactChanges;
import com.smartheritage.content.repository.ArtifactRepository;
import com.smartheritage.content.repository.MediaResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Ảnh/audio/video của hiện vật (bảng media_resources), chuyển từ Backend cũ. */
@Service
@RequiredArgsConstructor
public class MediaResourceService {

    private static final Set<String> MEDIA_TYPES = Set.of("IMAGE", "AUDIO", "VIDEO");

    private final MediaResourceRepository mediaResourceRepository;
    private final ArtifactRepository artifactRepository;
    private final ArtifactChanges changes;

    @Transactional(readOnly = true)
    public List<MediaResourceResponse> getByArtifact(Long artifactId) {
        requireArtifact(artifactId);
        return mediaResourceRepository.findByArtifactId(artifactId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public MediaResourceResponse create(Long artifactId, MediaResourceRequest request) {
        Artifact artifact = requireArtifact(artifactId);
        String type = request.getMediaType() == null ? "" : request.getMediaType().trim().toUpperCase(Locale.ROOT);
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
                .caption(request.getCaption() == null || request.getCaption().isBlank() ? null : request.getCaption().trim())
                .build());
        changes.record(artifact, List.of("media"));
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long mediaId) {
        MediaResource resource = mediaResourceRepository.findById(mediaId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy tài nguyên với ID: " + mediaId));
        Artifact artifact = requireArtifact(resource.getArtifactId());
        mediaResourceRepository.delete(resource);
        changes.record(artifact, List.of("media"));
    }

    private Artifact requireArtifact(Long artifactId) {
        if (artifactId == null) {
            throw ApiException.notFound("Không tìm thấy hiện vật với ID: " + artifactId);
        }
        return artifactRepository.findById(artifactId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hiện vật với ID: " + artifactId));
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
