package com.smartheritage.content.service;

import com.smartheritage.content.dto.*;
import com.smartheritage.content.entity.Artifact;
import com.smartheritage.content.entity.ArtifactContent;

import com.smartheritage.content.repository.ArtifactContentRepository;
import com.smartheritage.content.repository.ArtifactRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ArtifactService {

    private final ArtifactRepository artifactRepository;
    private final ArtifactContentRepository contentRepository;
    private final SiteClient siteClient;
    private final com.smartheritage.content.repository.MediaResourceRepository mediaRepository;

    // ──────────────────────────── READ ────────────────────────────

    /** Lấy tất cả hiện vật (tóm tắt, không kèm contents) */
    @Transactional(readOnly = true)
    public List<ArtifactSummaryResponse> getAllArtifacts() {
        return artifactRepository.findAll()
                .stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    /** Lấy hiện vật theo địa điểm */
    @Transactional(readOnly = true)
    public List<ArtifactSummaryResponse> getArtifactsByLocation(Long locationId) {
        return artifactRepository.findBySiteId(locationId)
                .stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    /** Lấy chi tiết một hiện vật (kèm contents đa ngôn ngữ) */
    @Transactional(readOnly = true)
    public ArtifactResponse getArtifactById(Long id) {
        Artifact artifact = findOrThrow(id);
        return toFullResponse(artifact);
    }

    /** Lấy hiện vật theo beacon UUID (mobile app dùng khi quét BLE) */
    @Transactional(readOnly = true)
    public ArtifactResponse getArtifactByBeaconUuid(String beaconUuid) {
        Artifact artifact = artifactRepository.findByBeaconUuid(beaconUuid)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,
                        "Không tìm thấy hiện vật với Beacon UUID: " + beaconUuid));
        return toFullResponse(artifact);
    }

    /** Tìm kiếm theo tên */
    @Transactional(readOnly = true)
    public List<ArtifactSummaryResponse> searchArtifacts(String keyword) {
        return artifactRepository.searchByName(keyword)
                .stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    // ──────────────────────────── CREATE ───────────────────────────

    /** Tạo mới hiện vật (có thể kèm contents đa ngôn ngữ) */
    public ArtifactResponse createArtifact(ArtifactRequest request) {
        validate(request, true);
        SiteClient.Site site = siteClient.requireSite(siteId(request));

        if (request.getArtifactCode() != null
                && artifactRepository.existsByArtifactCode(request.getArtifactCode())) {
            throw new IllegalArgumentException(
                    "Mã hiện vật đã tồn tại: " + request.getArtifactCode());
        }

        if (request.getBeaconUuid() != null
                && artifactRepository.existsByBeaconUuid(request.getBeaconUuid())) {
            throw new IllegalArgumentException(
                    "Beacon UUID đã được gán cho hiện vật khác: " + request.getBeaconUuid());
        }

        Artifact artifact = Artifact.builder()
                .siteId(site.id()).siteName(site.name())
                .name(request.getName())
                .artifactCode(request.getArtifactCode())
                .description(request.getDescription())
                .thumbnailUrl(request.getThumbnailUrl())
                .period(request.getPeriod())
                .material(request.getMaterial())
                .dimensions(request.getDimensions())
                .beaconUuid(request.getBeaconUuid())
                .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
                .build();

        artifactRepository.save(artifact);

        saveMedia(artifact.getId(), request.getMedia());

        // Lưu kèm contents nếu có
        if (request.getContents() != null && !request.getContents().isEmpty()) {
            List<ArtifactContent> contents = request.getContents().stream()
                    .map(c -> buildContent(c, artifact))
                    .collect(Collectors.toList());
            contentRepository.saveAll(contents);
            artifact.getContents().addAll(contents);
        }

        return toFullResponse(artifact);
    }

    // ──────────────────────────── UPDATE ───────────────────────────

    /** Cập nhật thông tin hiện vật */
    public ArtifactResponse updateArtifact(Long id, ArtifactRequest request) {
        validate(request, false);
        Artifact artifact = findOrThrow(id);

        if (siteId(request) != null) {
            SiteClient.Site site = siteClient.requireSite(siteId(request));
            artifact.setSiteId(site.id());
            artifact.setSiteName(site.name());
        }

        if (request.getName() != null) artifact.setName(request.getName());
        if (request.getDescription() != null) artifact.setDescription(request.getDescription());
        if (request.getThumbnailUrl() != null) artifact.setThumbnailUrl(request.getThumbnailUrl());
        if (request.getPeriod() != null) artifact.setPeriod(request.getPeriod());
        if (request.getMaterial() != null) artifact.setMaterial(request.getMaterial());
        if (request.getDimensions() != null) artifact.setDimensions(request.getDimensions());
        if (request.getStatus() != null) artifact.setStatus(request.getStatus());

        // Mã hiện vật — kiểm tra trùng trừ chính nó
        if (request.getArtifactCode() != null
                && !request.getArtifactCode().equals(artifact.getArtifactCode())) {
            if (artifactRepository.existsByArtifactCode(request.getArtifactCode())) {
                throw new IllegalArgumentException(
                        "Mã hiện vật đã tồn tại: " + request.getArtifactCode());
            }
            artifact.setArtifactCode(request.getArtifactCode().isBlank() ? null : request.getArtifactCode());
        }

        // Beacon UUID — kiểm tra trùng trừ chính nó
        if (request.getBeaconUuid() != null
                && !request.getBeaconUuid().equals(artifact.getBeaconUuid())) {
            if (artifactRepository.existsByBeaconUuid(request.getBeaconUuid())) {
                throw new IllegalArgumentException(
                        "Beacon UUID đã được gán cho hiện vật khác: " + request.getBeaconUuid());
            }
            artifact.setBeaconUuid(request.getBeaconUuid().isBlank() ? null : request.getBeaconUuid());
        }

        artifactRepository.save(artifact);
        if (request.getContents() != null) {
            var languages = request.getContents().stream().map(ArtifactContentRequest::getLanguageCode).toList();
            contentRepository.deleteAll(contentRepository.findByArtifactId(id).stream()
                    .filter(c -> !languages.contains(c.getLanguageCode())).toList());
            for (ArtifactContentRequest c : request.getContents()) upsertContent(id, c);
        }
        saveMedia(artifact.getId(), request.getMedia());
        return toFullResponse(artifact);
    }

    // ──────────────────────────── DELETE ───────────────────────────

    /** Xóa hiện vật và toàn bộ nội dung liên quan */
    public void deleteArtifact(Long id) {
        Artifact artifact = findOrThrow(id);
        mediaRepository.deleteAll(mediaRepository.findByArtifactId(id));
        contentRepository.deleteByArtifactId(id);
        artifactRepository.delete(artifact);
    }

    // ──────────── ARTIFACT CONTENT (đa ngôn ngữ) ───────────────────

    /** Thêm hoặc cập nhật nội dung theo ngôn ngữ */
    public ArtifactContentResponse upsertContent(Long artifactId, ArtifactContentRequest request) {
        validateContent(request);
        Artifact artifact = findOrThrow(artifactId);

        ArtifactContent content = contentRepository
                .findByArtifactIdAndLanguageCode(artifactId, request.getLanguageCode())
                .orElse(ArtifactContent.builder()
                        .artifact(artifact)
                        .languageCode(request.getLanguageCode())
                        .build());

        if (request.getTitle() != null) content.setTitle(request.getTitle());
        if (request.getDescription() != null) content.setDescription(request.getDescription());
        if (request.getAudioUrl() != null) content.setAudioUrl(request.getAudioUrl());
        if (request.getVideoUrl() != null) content.setVideoUrl(request.getVideoUrl());
        if (request.getImageUrls() != null) content.setImageUrls(request.getImageUrls());
        if (request.getTranscript() != null) content.setTranscript(request.getTranscript());

        contentRepository.save(content);
        return toContentResponse(content);
    }

    /** Lấy danh sách nội dung theo ngôn ngữ của một hiện vật */
    @Transactional(readOnly = true)
    public List<ArtifactContentResponse> getContents(Long artifactId) {
        findOrThrow(artifactId); // validate artifact tồn tại
        return contentRepository.findByArtifactId(artifactId)
                .stream()
                .map(this::toContentResponse)
                .collect(Collectors.toList());
    }

    /** Lấy nội dung theo ngôn ngữ cụ thể (fallback về tiếng Việt nếu không có) */
    @Transactional(readOnly = true)
    public ArtifactContentResponse getContentByLanguage(Long artifactId, String languageCode) {
        String normalizedLanguage = language(languageCode);
        findOrThrow(artifactId);
        return contentRepository
                .findByArtifactIdAndLanguageCode(artifactId, normalizedLanguage)
                .or(() -> contentRepository.findByArtifactIdAndLanguageCode(artifactId, "vi"))
                .map(this::toContentResponse)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Chưa có nội dung cho ngôn ngữ: " + languageCode));
    }

    /** Xóa một bản nội dung ngôn ngữ */
    public void deleteContent(Long contentId) {
        ArtifactContent content = contentRepository.findById(contentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy nội dung với ID: " + contentId));
        if ("vi".equals(content.getLanguageCode())) throw new IllegalArgumentException("Vietnamese content is required");
        contentRepository.delete(content);
    }

    // ─────────────────────── PRIVATE HELPERS ───────────────────────

    public ArtifactResponse localize(ArtifactResponse response, String lang) {
        String requested = language(lang);
        ArtifactContentResponse selected = getContentByLanguage(response.getId(), requested);
        response.setRequestedLanguage(requested);
        response.setLanguageCode(selected.getLanguageCode());
        response.setFallback(!requested.equals(selected.getLanguageCode()));
        response.setContent(selected);
        response.setName(selected.getTitle());
        response.setDescription(selected.getDescription());
        return response;
    }
    private Long siteId(ArtifactRequest r) {
        if (r.getSiteId() != null && r.getLocationId() != null && !r.getSiteId().equals(r.getLocationId()))
            throw new IllegalArgumentException("siteId and locationId disagree");
        return r.getSiteId() != null ? r.getSiteId() : r.getLocationId();
    }
    private String language(String lang) {
        String value = lang == null ? "vi" : lang.trim().toLowerCase(java.util.Locale.ROOT);
        if (!java.util.Set.of("vi", "en").contains(value)) throw new IllegalArgumentException("lang must be vi or en");
        return value;
    }
    private void url(String value) {
        if (value == null || value.isBlank()) return;
        try {
            var uri = java.net.URI.create(value);
            if (!java.util.Set.of("http", "https").contains(uri.getScheme()) || uri.getHost() == null) throw new IllegalArgumentException();
        } catch (IllegalArgumentException e) { throw new IllegalArgumentException("Media URL must use http or https"); }
    }
    private void validateContent(ArtifactContentRequest c) {
        if (c == null || c.getLanguageCode() == null) throw new IllegalArgumentException("languageCode is required");
        max(c.getAudioUrl(), 1000); max(c.getVideoUrl(), 1000);
        c.setLanguageCode(language(c.getLanguageCode()));
        if (c.getTitle() == null || c.getTitle().isBlank() || c.getTitle().length() > 200 || c.getDescription() == null || c.getDescription().isBlank())
            throw new IllegalArgumentException("Translation title and description are required");
        url(c.getAudioUrl()); url(c.getVideoUrl());
        if (c.getImageUrls() != null && !c.getImageUrls().isBlank()) {
            try {
                var images = new com.fasterxml.jackson.databind.ObjectMapper().readTree(c.getImageUrls());
                if (!images.isArray()) throw new IllegalArgumentException();
                for (var image : images) { if (!image.isTextual()) throw new IllegalArgumentException(); url(image.asText()); }
            } catch (Exception e) { throw new IllegalArgumentException("imageUrls must be a JSON string containing HTTP(S) URLs"); }
        }
    }
    private void validate(ArtifactRequest r, boolean create) {
        max(r.getArtifactCode(), 50); max(r.getBeaconUuid(), 100); max(r.getPeriod(), 100);
        max(r.getMaterial(), 200); max(r.getDimensions(), 200); max(r.getThumbnailUrl(), 1000);
        if ((create || r.getName() != null) && (r.getName() == null || r.getName().isBlank() || r.getName().length() > 200)) throw new IllegalArgumentException("name is required (max 200)");
        if (r.getStatus() != null && !java.util.Set.of("ACTIVE", "INACTIVE").contains(r.getStatus())) throw new IllegalArgumentException("Invalid status");
        url(r.getThumbnailUrl());
        if ((create || r.getContents() != null) && (r.getContents() == null || r.getContents().stream().noneMatch(c -> "vi".equalsIgnoreCase(c.getLanguageCode())))) throw new IllegalArgumentException("Vietnamese translation is required");
        var languages = new java.util.HashSet<String>();
        if (r.getContents() != null) for (var c : r.getContents()) {
            validateContent(c);
            if (!languages.add(c.getLanguageCode())) throw new IllegalArgumentException("Duplicate language");
        }
        if (r.getMedia() != null) for (var m : r.getMedia()) {
            if (m.getMediaType() == null || !java.util.Set.of("IMAGE", "AUDIO", "VIDEO").contains(m.getMediaType()) || m.getFileUrl() == null || m.getFileUrl().isBlank()) throw new IllegalArgumentException("Media type and URL required");
            url(m.getFileUrl());
            if (m.getFileUrl().length() > 500 || (m.getCaption() != null && m.getCaption().length() > 255)) throw new IllegalArgumentException("Media field too long");
        }
    }
    private void saveMedia(Long id, List<com.smartheritage.content.entity.MediaResource> media) {
        if (media == null) return;
        mediaRepository.deleteAll(mediaRepository.findByArtifactId(id));
        for (var m : media) { m.setId(null); m.setArtifactId(id); }
        mediaRepository.saveAll(media);
    }

    private void max(String value, int limit) {
        if (value != null && value.length() > limit) throw new IllegalArgumentException("Field exceeds max length " + limit);
    }

    private Artifact findOrThrow(Long id) {
        return artifactRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,
                        "Không tìm thấy hiện vật với ID: " + id));
    }

    private ArtifactContent buildContent(ArtifactContentRequest req, Artifact artifact) {
        return ArtifactContent.builder()
                .artifact(artifact)
                .languageCode(req.getLanguageCode())
                .title(req.getTitle())
                .description(req.getDescription())
                .audioUrl(req.getAudioUrl())
                .videoUrl(req.getVideoUrl())
                .imageUrls(req.getImageUrls())
                .transcript(req.getTranscript())
                .build();
    }

    private ArtifactSummaryResponse toSummary(Artifact a) {
        return ArtifactSummaryResponse.builder()
                .id(a.getId())
                .siteId(a.getSiteId()).locationId(a.getSiteId())
                .locationName(a.getSiteName())
                .name(a.getName())
                .artifactCode(a.getArtifactCode())
                .thumbnailUrl(a.getThumbnailUrl())
                .period(a.getPeriod())
                .material(a.getMaterial())
                .beaconUuid(a.getBeaconUuid())
                .status(a.getStatus())
                .build();
    }

    private ArtifactResponse toFullResponse(Artifact a) {
        List<ArtifactContentResponse> contents = contentRepository
                .findByArtifactId(a.getId())
                .stream()
                .map(this::toContentResponse)
                .collect(Collectors.toList());

        return ArtifactResponse.builder()
                .id(a.getId())
                .siteId(a.getSiteId()).locationId(a.getSiteId())
                .locationName(a.getSiteName())
                .name(a.getName())
                .artifactCode(a.getArtifactCode())
                .description(a.getDescription())
                .thumbnailUrl(a.getThumbnailUrl())
                .period(a.getPeriod())
                .material(a.getMaterial())
                .dimensions(a.getDimensions())
                .beaconUuid(a.getBeaconUuid())
                .status(a.getStatus())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .media(mediaRepository.findByArtifactId(a.getId()))
                .contents(contents)
                .build();
    }

    private ArtifactContentResponse toContentResponse(ArtifactContent c) {
        return ArtifactContentResponse.builder()
                .id(c.getId())
                .artifactId(c.getArtifact().getId())
                .languageCode(c.getLanguageCode())
                .title(c.getTitle())
                .description(c.getDescription())
                .audioUrl(c.getAudioUrl())
                .videoUrl(c.getVideoUrl())
                .imageUrls(c.getImageUrls())
                .transcript(c.getTranscript())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
