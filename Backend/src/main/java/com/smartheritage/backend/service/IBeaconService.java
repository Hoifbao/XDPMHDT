package com.smartheritage.backend.service;

import com.smartheritage.backend.dto.IBeaconRequest;
import com.smartheritage.backend.dto.IBeaconResponse;
import com.smartheritage.backend.entity.Artifact;
import com.smartheritage.backend.entity.IBeacon;
import com.smartheritage.backend.repository.ArtifactRepository;
import com.smartheritage.backend.repository.IBeaconRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class IBeaconService {

    private static final String DUPLICATE_MESSAGE = "iBeacon với UUID/Major/Minor này đã tồn tại";
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}$");

    private final IBeaconRepository beaconRepository;
    private final ArtifactRepository artifactRepository;

    // ──────────────────────────── READ ────────────────────────────

    @Transactional(readOnly = true)
    public List<IBeaconResponse> getAllBeacons() {
        return beaconRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public IBeaconResponse getBeaconById(Long id) {
        return toResponse(findOrThrow(id));
    }

    // ──────────────────────────── CREATE ───────────────────────────

    public IBeaconResponse createBeacon(IBeaconRequest request) {
        String uuid = normalizeAndValidate(request);

        if (beaconRepository.existsByUuidAndMajorAndMinor(uuid, request.getMajor(), request.getMinor())) {
            throw new IllegalArgumentException(DUPLICATE_MESSAGE);
        }

        IBeacon beacon = IBeacon.builder().build();
        applyFields(beacon, request, uuid);
        beaconRepository.save(beacon);
        return toResponse(beacon);
    }

    // ──────────────────────────── UPDATE ───────────────────────────

    public IBeaconResponse updateBeacon(Long id, IBeaconRequest request) {
        IBeacon beacon = findOrThrow(id);
        String uuid = normalizeAndValidate(request);

        beaconRepository.findByUuidAndMajorAndMinor(uuid, request.getMajor(), request.getMinor())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException(DUPLICATE_MESSAGE);
                });

        applyFields(beacon, request, uuid);
        beaconRepository.save(beacon);
        return toResponse(beacon);
    }

    // ──────────────────────────── DELETE ───────────────────────────

    public void deleteBeacon(Long id) {
        beaconRepository.delete(findOrThrow(id));
    }

    // ─────────────────────── PRIVATE HELPERS ───────────────────────

    private IBeacon findOrThrow(Long id) {
        return beaconRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Không tìm thấy iBeacon với ID: " + id));
    }

    private String normalizeAndValidate(IBeaconRequest request) {
        if (request.getUuid() == null || !UUID_PATTERN.matcher(request.getUuid().trim()).matches()) {
            throw new IllegalArgumentException("UUID không đúng định dạng");
        }
        validateRange("Major", request.getMajor());
        validateRange("Minor", request.getMinor());
        return request.getUuid().trim().toUpperCase();
    }

    private void validateRange(String field, Integer value) {
        if (value == null || value < 0 || value > 65535) {
            throw new IllegalArgumentException(field + " phải nằm trong khoảng 0–65535");
        }
    }

    private void applyFields(IBeacon beacon, IBeaconRequest request, String uuid) {
        beacon.setUuid(uuid);
        beacon.setMajor(request.getMajor());
        beacon.setMinor(request.getMinor());
        beacon.setArtifact(findArtifact(request.getArtifactId()));
        beacon.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");
    }

    private Artifact findArtifact(Long artifactId) {
        if (artifactId == null) {
            return null;
        }
        return artifactRepository.findById(artifactId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy hiện vật với ID: " + artifactId));
    }

    private IBeaconResponse toResponse(IBeacon beacon) {
        Artifact artifact = beacon.getArtifact();
        return IBeaconResponse.builder()
                .id(beacon.getId())
                .uuid(beacon.getUuid())
                .major(beacon.getMajor())
                .minor(beacon.getMinor())
                .artifactId(artifact != null ? artifact.getId() : null)
                .artifactName(artifact != null ? artifact.getName() : null)
                .status(beacon.getStatus())
                .createdAt(beacon.getCreatedAt())
                .updatedAt(beacon.getUpdatedAt())
                .build();
    }
}
