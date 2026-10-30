package com.smartheritage.content.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartheritage.common.web.ApiException;
import com.smartheritage.content.client.SiteClient;
import com.smartheritage.content.dto.*;
import com.smartheritage.content.entity.Artifact;
import com.smartheritage.content.entity.ArtifactContent;
import com.smartheritage.content.entity.MediaResource;
import com.smartheritage.content.repository.ArtifactContentRepository;
import com.smartheritage.content.repository.ArtifactRepository;
import com.smartheritage.content.repository.MediaResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Nghiệp vụ hiện vật + nội dung đa ngôn ngữ (Xuân Anh, chuyển từ Backend cũ sang content-service - Sprint 4).
 * Khác Backend cũ: kiểm tra siteId qua {@link SiteClient} (site-service) thay cho HeritageSiteRepository;
 * lỗi trả bằng ApiException / IllegalArgumentException, GlobalExceptionHandler đổi thành JSON đúng mã HTTP.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ArtifactService {

    static final int MAX_BATCH = 100;
    private static final Set<String> LANGUAGES = Set.of("vi", "en");
    private static final Set<String> STATUSES = Set.of("ACTIVE", "INACTIVE");
    private static final Set<String> MEDIA_TYPES = Set.of("IMAGE", "AUDIO", "VIDEO");
    private static final ObjectMapper JSON = new ObjectMapper();

    private final ArtifactRepository artifactRepository;
    private final ArtifactContentRepository contentRepository;
    private final MediaResourceRepository mediaRepository;
    private final SiteClient siteClient;

    // ──────────────────────────── READ ────────────────────────────

    @Transactional(readOnly = true)
    public List<ArtifactSummaryResponse> getAllArtifacts() {
        return artifactRepository.findAll().stream().map(this::toSummary).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ArtifactSummaryResponse> getArtifactsBySite(Long siteId) {
        return artifactRepository.findBySiteId(siteId).stream().map(this::toSummary).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ArtifactSummaryResponse> searchArtifacts(String keyword) {
        return artifactRepository.searchByName(keyword).stream().map(this::toSummary).collect(Collectors.toList());
    }

    /**
     * Lấy nhiều hiện vật một lần (Phú dùng cho Yêu thích/Lịch sử).
     * Trả đúng thứ tự id được yêu cầu, bỏ qua id không tồn tại, bỏ id trùng; tối đa 100 id.
     */
    @Transactional(readOnly = true)
    public List<ArtifactSummaryResponse> getBatch(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Long> distinct = ids.stream().filter(Objects::nonNull).distinct().toList();
        if (distinct.size() > MAX_BATCH) {
            throw new IllegalArgumentException("Tối đa " + MAX_BATCH + " id mỗi lần gọi");
        }
        Map<Long, Artifact> byId = artifactRepository.findAllById(distinct).stream()
                .collect(Collectors.toMap(Artifact::getId, Function.identity()));
        return distinct.stream().map(byId::get).filter(Objects::nonNull).map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public ArtifactResponse getArtifactById(Long id) {
        return toFullResponse(findOrThrow(id));
    }

    /** [Cũ] tìm theo cột beacon_uuid; luồng chuẩn là /api/ibeacons/registry -> /api/artifacts/{id} */
    @Transactional(readOnly = true)
    public ArtifactResponse getArtifactByBeaconUuid(String beaconUuid) {
        Artifact artifact = artifactRepository.findByBeaconUuid(beaconUuid)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hiện vật với Beacon UUID: " + beaconUuid));
        return toFullResponse(artifact);
    }

    // ──────────────────────────── CREATE ───────────────────────────

    public ArtifactResponse createArtifact(ArtifactRequest request) {
        validate(request, true);
        // Kiểm tra trùng trước khi gọi site-service để không tốn 1 lần gọi mạng khi dữ liệu đã sai
        if (request.getArtifactCode() != null && artifactRepository.existsByArtifactCode(request.getArtifactCode())) {
            throw new IllegalArgumentException("Mã hiện vật đã tồn tại: " + request.getArtifactCode());
        }
        if (request.getBeaconUuid() != null && artifactRepository.existsByBeaconUuid(request.getBeaconUuid())) {
            throw new IllegalArgumentException("Beacon UUID đã được gán cho hiện vật khác: " + request.getBeaconUuid());
        }
        SiteClient.Site site = siteClient.requireSite(siteId(request));

        Artifact artifact = Artifact.builder()
                .siteId(site.id())
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

    public ArtifactResponse updateArtifact(Long id, ArtifactRequest request) {
        validate(request, false);
        Artifact artifact = findOrThrow(id);

        if (siteId(request) != null && !siteId(request).equals(artifact.getSiteId())) {
            artifact.setSiteId(siteClient.requireSite(siteId(request)).id());
        }
        if (request.getName() != null) artifact.setName(request.getName());
        if (request.getDescription() != null) artifact.setDescription(request.getDescription());
        if (request.getThumbnailUrl() != null) artifact.setThumbnailUrl(request.getThumbnailUrl());
        if (request.getPeriod() != null) artifact.setPeriod(request.getPeriod());
        if (request.getMaterial() != null) artifact.setMaterial(request.getMaterial());
        if (request.getDimensions() != null) artifact.setDimensions(request.getDimensions());
        if (request.getStatus() != null) artifact.setStatus(request.getStatus());

        if (request.getArtifactCode() != null && !request.getArtifactCode().equals(artifact.getArtifactCode())) {
            if (artifactRepository.existsByArtifactCode(request.getArtifactCode())) {
                throw new IllegalArgumentException("Mã hiện vật đã tồn tại: " + request.getArtifactCode());
            }
            artifact.setArtifactCode(request.getArtifactCode().isBlank() ? null : request.getArtifactCode());
        }
        if (request.getBeaconUuid() != null && !request.getBeaconUuid().equals(artifact.getBeaconUuid())) {
            if (artifactRepository.existsByBeaconUuid(request.getBeaconUuid())) {
                throw new IllegalArgumentException("Beacon UUID đã được gán cho hiện vật khác: " + request.getBeaconUuid());
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

    public void deleteArtifact(Long id) {
        Artifact artifact = findOrThrow(id);
        mediaRepository.deleteAll(mediaRepository.findByArtifactId(id));
        contentRepository.deleteByArtifactId(id);
        artifactRepository.delete(artifact);
    }

    // ──────────── ARTIFACT CONTENT (đa ngôn ngữ) ───────────────────

    public ArtifactContentResponse upsertContent(Long artifactId, ArtifactContentRequest request) {
        validateContent(request);
        Artifact artifact = findOrThrow(artifactId);

        ArtifactContent content = contentRepository
                .findByArtifactIdAndLanguageCode(artifactId, request.getLanguageCode())
                .orElse(ArtifactContent.builder().artifact(artifact).languageCode(request.getLanguageCode()).build());

        if (request.getTitle() != null) content.setTitle(request.getTitle());
        if (request.getDescription() != null) content.setDescription(request.getDescription());
        if (request.getAudioUrl() != null) content.setAudioUrl(request.getAudioUrl());
        if (request.getVideoUrl() != null) content.setVideoUrl(request.getVideoUrl());
        if (request.getImageUrls() != null) content.setImageUrls(request.getImageUrls());
        if (request.getTranscript() != null) content.setTranscript(request.getTranscript());

        contentRepository.save(content);
        return toContentResponse(content);
    }

    @Transactional(readOnly = true)
    public List<ArtifactContentResponse> getContents(Long artifactId) {
        findOrThrow(artifactId);
        return contentRepository.findByArtifactId(artifactId).stream()
                .map(this::toContentResponse).collect(Collectors.toList());
    }

    /** Nội dung theo ngôn ngữ; không có thì trả bản tiếng Việt. */
    @Transactional(readOnly = true)
    public ArtifactContentResponse getContentByLanguage(Long artifactId, String languageCode) {
        String normalized = language(languageCode);
        findOrThrow(artifactId);
        return contentRepository.findByArtifactIdAndLanguageCode(artifactId, normalized)
                .or(() -> contentRepository.findByArtifactIdAndLanguageCode(artifactId, "vi"))
                .map(this::toContentResponse)
                .orElseThrow(() -> ApiException.notFound("Chưa có nội dung cho ngôn ngữ: " + languageCode));
    }

    public void deleteContent(Long contentId) {
        ArtifactContent content = contentRepository.findById(contentId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy nội dung với ID: " + contentId));
        if ("vi".equals(content.getLanguageCode())) {
            throw new IllegalArgumentException("Không được xóa bản tiếng Việt (bắt buộc)");
        }
        contentRepository.delete(content);
    }

    /** Gắn nội dung theo ngôn ngữ vào response chi tiết: requestedLanguage, languageCode, fallback, content. */
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

    // ─────────────────────── PRIVATE HELPERS ───────────────────────

    private Long siteId(ArtifactRequest r) {
        if (r.getSiteId() != null && r.getLocationId() != null && !r.getSiteId().equals(r.getLocationId())) {
            throw new IllegalArgumentException("siteId và locationId không khớp");
        }
        return r.getSiteId() != null ? r.getSiteId() : r.getLocationId();
    }

    private String language(String lang) {
        String value = lang == null ? "vi" : lang.trim().toLowerCase(Locale.ROOT);
        if (!LANGUAGES.contains(value)) throw new IllegalArgumentException("lang phải là vi hoặc en");
        return value;
    }

    private void url(String value) {
        if (value == null || value.isBlank()) return;
        try {
            URI uri = URI.create(value);
            if (!Set.of("http", "https").contains(uri.getScheme()) || uri.getHost() == null) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("URL media phải dùng http hoặc https");
        }
    }

    private void validateContent(ArtifactContentRequest c) {
        if (c == null || c.getLanguageCode() == null) throw new IllegalArgumentException("languageCode là bắt buộc");
        max(c.getAudioUrl(), 1000);
        max(c.getVideoUrl(), 1000);
        c.setLanguageCode(language(c.getLanguageCode()));
        if (c.getTitle() == null || c.getTitle().isBlank() || c.getTitle().length() > 200
                || c.getDescription() == null || c.getDescription().isBlank()) {
            throw new IllegalArgumentException("Bản dịch phải có tiêu đề (tối đa 200 ký tự) và mô tả");
        }
        url(c.getAudioUrl());
        url(c.getVideoUrl());
        if (c.getImageUrls() != null && !c.getImageUrls().isBlank()) {
            try {
                var images = JSON.readTree(c.getImageUrls());
                if (!images.isArray()) throw new IllegalArgumentException();
                for (var image : images) {
                    if (!image.isTextual()) throw new IllegalArgumentException();
                    url(image.asText());
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("imageUrls phải là chuỗi JSON chứa các URL http(s)");
            }
        }
    }

    private void validate(ArtifactRequest r, boolean create) {
        max(r.getArtifactCode(), 50);
        max(r.getBeaconUuid(), 100);
        max(r.getPeriod(), 100);
        max(r.getMaterial(), 200);
        max(r.getDimensions(), 200);
        max(r.getThumbnailUrl(), 1000);
        if ((create || r.getName() != null) && (r.getName() == null || r.getName().isBlank() || r.getName().length() > 200)) {
            throw new IllegalArgumentException("name là bắt buộc (tối đa 200 ký tự)");
        }
        if (r.getStatus() != null && !STATUSES.contains(r.getStatus())) {
            throw new IllegalArgumentException("status phải là ACTIVE hoặc INACTIVE");
        }
        url(r.getThumbnailUrl());

        var languages = new HashSet<String>();
        if (r.getContents() != null) for (var c : r.getContents()) {
            validateContent(c);
            if (!languages.add(c.getLanguageCode())) throw new IllegalArgumentException("Trùng ngôn ngữ trong contents");
        }
        if ((create || r.getContents() != null) && !languages.contains("vi")) {
            throw new IllegalArgumentException("Bắt buộc có bản tiếng Việt");
        }
        if (create && siteId(r) == null) throw new IllegalArgumentException("siteId là bắt buộc");
        if (r.getMedia() != null) for (var m : r.getMedia()) {
            if (m == null || m.getMediaType() == null || !MEDIA_TYPES.contains(m.getMediaType())
                    || m.getFileUrl() == null || m.getFileUrl().isBlank()) {
                throw new IllegalArgumentException("Media cần mediaType (IMAGE, AUDIO, VIDEO) và fileUrl");
            }
            url(m.getFileUrl());
            if (m.getFileUrl().length() > 500 || (m.getCaption() != null && m.getCaption().length() > 255)) {
                throw new IllegalArgumentException("Trường media quá dài");
            }
        }
    }

    private void saveMedia(Long id, List<MediaResource> media) {
        if (media == null) return;
        mediaRepository.deleteAll(mediaRepository.findByArtifactId(id));
        for (var m : media) {
            m.setId(null);
            m.setArtifactId(id);
        }
        mediaRepository.saveAll(media);
    }

    private void max(String value, int limit) {
        if (value != null && value.length() > limit) {
            throw new IllegalArgumentException("Trường vượt quá " + limit + " ký tự");
        }
    }

    private Artifact findOrThrow(Long id) {
        return artifactRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy hiện vật với ID: " + id));
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
                .locationName(siteClient.findName(a.getSiteId()).orElse(null))
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
        List<ArtifactContentResponse> contents = contentRepository.findByArtifactId(a.getId()).stream()
                .map(this::toContentResponse).collect(Collectors.toList());
        return ArtifactResponse.builder()
                .id(a.getId())
                .siteId(a.getSiteId()).locationId(a.getSiteId())
                .locationName(siteClient.findName(a.getSiteId()).orElse(null))
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
