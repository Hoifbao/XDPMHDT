package com.smartheritage.backend.service;

import com.smartheritage.backend.dto.*;
import com.smartheritage.backend.entity.Artifact;
import com.smartheritage.backend.entity.ArtifactContent;
import com.smartheritage.backend.entity.HeritageSite;
import com.smartheritage.backend.repository.ArtifactContentRepository;
import com.smartheritage.backend.repository.ArtifactRepository;
import com.smartheritage.backend.repository.HeritageSiteRepository;
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
    private final HeritageSiteRepository siteRepository;

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
                .orElseThrow(() -> new IllegalArgumentException(
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
        HeritageSite site = siteRepository.findById(request.getLocationId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy di tích với ID: " + request.getLocationId()));

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
                .site(site)
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
        Artifact artifact = findOrThrow(id);

        if (request.getLocationId() != null) {
            HeritageSite site = siteRepository.findById(request.getLocationId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Không tìm thấy di tích với ID: " + request.getLocationId()));
            artifact.setSite(site);
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
            artifact.setArtifactCode(request.getArtifactCode());
        }

        // Beacon UUID — kiểm tra trùng trừ chính nó
        if (request.getBeaconUuid() != null
                && !request.getBeaconUuid().equals(artifact.getBeaconUuid())) {
            if (artifactRepository.existsByBeaconUuid(request.getBeaconUuid())) {
                throw new IllegalArgumentException(
                        "Beacon UUID đã được gán cho hiện vật khác: " + request.getBeaconUuid());
            }
            artifact.setBeaconUuid(request.getBeaconUuid());
        }

        artifactRepository.save(artifact);
        return toFullResponse(artifact);
    }

    // ──────────────────────────── DELETE ───────────────────────────

    /** Xóa hiện vật và toàn bộ nội dung liên quan */
    public void deleteArtifact(Long id) {
        Artifact artifact = findOrThrow(id);
        contentRepository.deleteByArtifactId(id);
        artifactRepository.delete(artifact);
    }

    // ──────────── ARTIFACT CONTENT (đa ngôn ngữ) ───────────────────

    /** Thêm hoặc cập nhật nội dung theo ngôn ngữ */
    public ArtifactContentResponse upsertContent(Long artifactId, ArtifactContentRequest request) {
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
        findOrThrow(artifactId);
        return contentRepository
                .findByArtifactIdAndLanguageCode(artifactId, languageCode)
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
        contentRepository.delete(content);
    }

    // ─────────────────────── PRIVATE HELPERS ───────────────────────

    private Artifact findOrThrow(Long id) {
        return artifactRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
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
                .locationId(a.getSite().getId())
                .locationName(a.getSite().getName())
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
                .locationId(a.getSite().getId())
                .locationName(a.getSite().getName())
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
