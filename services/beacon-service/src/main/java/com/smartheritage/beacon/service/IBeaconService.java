package com.smartheritage.beacon.service;

import com.smartheritage.beacon.client.ArtifactClient;
import com.smartheritage.beacon.client.SiteClient;
import com.smartheritage.beacon.dto.IBeaconRequest;
import com.smartheritage.beacon.dto.IBeaconResponse;
import com.smartheritage.beacon.entity.IBeacon;
import com.smartheritage.beacon.repository.IBeaconRepository;
import com.smartheritage.common.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Logic iBeacon của beacon-service - chuyển từ Backend cũ (Khoa, Sprint 3).
 *
 * Điểm khác khi tách service: bảng ibeacons NO với site_db.heritage_sites và content_db.artifacts,
 * nên "site" và "artifact" kiểm tra/lấy tên qua REST (SiteClient / ArtifactClient),
 * còn features như Cache để tránh gọi lặp lại khi hiển thị danh sách.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class IBeaconService {

    private static final String DUPLICATE_MESSAGE = "iBeacon với UUID/Major/Minor này đã tồn tại";
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}$");
    private static final Set<String> VALID_STATUSES = Set.of("ACTIVE", "INACTIVE");

    private final IBeaconRepository beaconRepository;
    private final SiteClient siteClient;
    private final ArtifactClient artifactClient;

    // ──────────────────────────── READ ────────────────────────────

    /** Lấy tất cả beacon, hoặc lọc theo di tích nếu có siteId */
    @Transactional(readOnly = true)
    public List<IBeaconResponse> getBeacons(Long siteId) {
        List<IBeacon> beacons = siteId != null
                ? beaconRepository.findBySiteId(siteId)
                : beaconRepository.findAll();
        return beacons.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public IBeaconResponse getBeaconById(Long id) {
        return toResponse(findOrThrow(id));
    }

    /** Danh sách beacon ACTIVE để Mobile tải về và đối chiếu với tín hiệu quét được */
    @Transactional(readOnly = true)
    public List<IBeaconResponse> getRegistry() {
        return beaconRepository.findByStatus("ACTIVE")
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ──────────────────────────── CREATE ──────────────────────────

    public IBeaconResponse createBeacon(IBeaconRequest request) {
        String uuid = normalizeAndValidate(request);

        SiteClient.Site site = siteClient.requireSite(request.getSiteId());
        ArtifactClient.Artifact artifact = checkArtifactOfSite(request.getArtifactId(), site.id());

        if (beaconRepository.existsByUuidAndMajorAndMinor(uuid, request.getMajor(), request.getMinor())) {
            throw ApiException.badRequest(DUPLICATE_MESSAGE);
        }

        IBeacon beacon = IBeacon.builder().build();
        applyFields(beacon, request, uuid, site, artifact);
        beaconRepository.save(beacon);
        return toResponse(beacon);
    }

    // ──────────────────────────── UPDATE ──────────────────────────

    public IBeaconResponse updateBeacon(Long id, IBeaconRequest request) {
        IBeacon beacon = findOrThrow(id);
        String uuid = normalizeAndValidate(request);
        SiteClient.Site site = siteClient.requireSite(request.getSiteId());
        ArtifactClient.Artifact artifact = checkArtifactOfSite(request.getArtifactId(), site.id());

        beaconRepository.findByUuidAndMajorAndMinor(uuid, request.getMajor(), request.getMinor())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw ApiException.badRequest(DUPLICATE_MESSAGE);
                });

        applyFields(beacon, request, uuid, site, artifact);
        beaconRepository.save(beacon);
        return toResponse(beacon);
    }

    // ──────────────────────────── ASSIGN ──────────────────────────

    /** Gán hiện vật cho beacon. artifactId = null để bỏ gán. */
    public IBeaconResponse assignArtifact(Long id, Long artifactId) {
        IBeacon beacon = findOrThrow(id);
        ArtifactClient.Artifact artifact = checkArtifactOfSite(artifactId, beacon.getSiteId());
        beacon.setArtifactId(artifact != null ? artifact.id() : null);
        beaconRepository.save(beacon);
        return toResponse(beacon);
    }

    // ──────────────────────────── DELETE ──────────────────────────

    public void deleteBeacon(Long id) {
        beaconRepository.delete(findOrThrow(id));
    }

    // ─────────────────────── PRIVATE HELPERS ──────────────────────

    private IBeacon findOrThrow(Long id) {
        return beaconRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy iBeacon với ID: " + id));
    }

    private String normalizeAndValidate(IBeaconRequest request) {
        if (request.getUuid() == null || !UUID_PATTERN.matcher(request.getUuid().trim()).matches()) {
            throw ApiException.badRequest("UUID không đúng định dạng");
        }
        validateRange("Major", request.getMajor());
        validateRange("Minor", request.getMinor());
        if (request.getSiteId() == null) {
            throw ApiException.badRequest("Phải chọn di tích (siteId) cho iBeacon");
        }
        if (request.getBatteryLevel() != null
                && (request.getBatteryLevel() < 0 || request.getBatteryLevel() > 100)) {
            throw ApiException.badRequest("Pin (batteryLevel) phải nằm trong khoảng 0–100");
        }
        if (request.getStatus() != null && !VALID_STATUSES.contains(request.getStatus())) {
            throw ApiException.badRequest("Trạng thái chỉ nhận ACTIVE hoặc INACTIVE");
        }
        return request.getUuid().trim().toUpperCase();
    }

    private void validateRange(String field, Integer value) {
        if (value == null || value < 0 || value > 65535) {
            throw ApiException.badRequest(field + " phải nằm trong khoảng 0–65535");
        }
    }

    /** Kiểm tra hiện vật tồn tại (content-service) và thuộc cùng di tích với beacon (400 nếu khác). */
    private ArtifactClient.Artifact checkArtifactOfSite(Long artifactId, Long siteId) {
        if (artifactId == null) {
            return null;
        }
        ArtifactClient.Artifact artifact = artifactClient.requireArtifact(artifactId);
        if (artifact.siteId() != null && !artifact.siteId().equals(siteId)) {
            throw ApiException.badRequest(
                    "Hiện vật ID " + artifact.id() + " không thuộc di tích ID " + siteId);
        }
        return artifact;
    }

    private void applyFields(IBeacon beacon, IBeaconRequest request, String uuid,
                             SiteClient.Site site, ArtifactClient.Artifact artifact) {
        beacon.setUuid(uuid);
        beacon.setMajor(request.getMajor());
        beacon.setMinor(request.getMinor());
        beacon.setSiteId(request.getSiteId());
        beacon.setArtifactId(artifact != null ? artifact.id() : null);
        beacon.setPosX(request.getPosX());
        beacon.setPosY(request.getPosY());
        beacon.setBatteryLevel(request.getBatteryLevel());
        beacon.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");
    }

    private IBeaconResponse toResponse(IBeacon beacon) {
        return IBeaconResponse.builder()
                .id(beacon.getId())
                .uuid(beacon.getUuid())
                .major(beacon.getMajor())
                .minor(beacon.getMinor())
                .siteId(beacon.getSiteId())
                .siteName(siteClient.findName(beacon.getSiteId()).orElse(null))
                .artifactId(beacon.getArtifactId())
                .artifactName(artifactClient.findName(beacon.getArtifactId()).orElse(null))
                .posX(beacon.getPosX())
                .posY(beacon.getPosY())
                .batteryLevel(beacon.getBatteryLevel())
                .status(beacon.getStatus())
                .createdAt(beacon.getCreatedAt())
                .updatedAt(beacon.getUpdatedAt())
                .build();
    }
}
