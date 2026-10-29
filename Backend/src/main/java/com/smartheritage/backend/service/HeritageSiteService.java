package com.smartheritage.backend.service;

import com.smartheritage.backend.dto.HeritageSiteRequest;
import com.smartheritage.backend.dto.HeritageSiteResponse;
import com.smartheritage.backend.dto.NearbyHeritageSiteResponse;
import com.smartheritage.backend.entity.HeritageSite;
import com.smartheritage.backend.repository.HeritageSiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HeritageSiteService {

    private static final double EARTH_RADIUS_KM = 6371.0088;

    private final HeritageSiteRepository repository;

    /** Lấy toàn bộ danh sách di tích. */
    public List<HeritageSiteResponse> getAll() {
        return repository.findAll()
                .stream()
                .map(HeritageSiteResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * Lấy các di tích trong bán kính tính từ tọa độ người dùng, gần nhất trước.
     *
     * @param lat      vĩ độ -90..90
     * @param lng      kinh độ -180..180
     * @param radiusKm bán kính km, > 0 và ≤ 100
     */
    public List<NearbyHeritageSiteResponse> getNearby(double lat, double lng, double radiusKm) {
        validateCoordinate(lat, lng, "Tọa độ tìm kiếm");
        if (!Double.isFinite(radiusKm) || radiusKm <= 0) {
            throw new IllegalArgumentException("Bán kính tìm kiếm phải lớn hơn 0 km.");
        }
        if (radiusKm > 100) {
            throw new IllegalArgumentException("Bán kính tìm kiếm tối đa là 100 km.");
        }

        return repository.findAll().stream()
                .filter(site -> site.getLatitude() != null && site.getLongitude() != null)
                .map(site -> {
                    double rawDist = haversineKm(lat, lng,
                            site.getLatitude().doubleValue(), site.getLongitude().doubleValue());
                    double distKm = BigDecimal.valueOf(rawDist)
                            .setScale(2, RoundingMode.HALF_UP)
                            .doubleValue();
                    return new NearbyHeritageSiteResponse(HeritageSiteResponse.from(site), distKm);
                })
                .filter(site -> site.getDistanceKm() <= radiusKm)
                .sorted(Comparator.comparingDouble(NearbyHeritageSiteResponse::getDistanceKm))
                .collect(Collectors.toList());
    }

    /** Lấy chi tiết một di tích theo id. */
    public HeritageSiteResponse getById(Long id) {
        HeritageSite site = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy di tích với id: " + id));
        return HeritageSiteResponse.from(site);
    }

    /** Tạo mới di tích. */
    public HeritageSiteResponse create(HeritageSiteRequest request) {
        validateCoordinatePair(request.getLatitude(), request.getLongitude());
        if (request.getName() == null || request.getName().isBlank()) {
            throw new RuntimeException("Tên di tích không được để trống.");
        }
        if (repository.existsByName(request.getName())) {
            throw new RuntimeException("Di tích '" + request.getName() + "' đã tồn tại.");
        }

        HeritageSite site = HeritageSite.builder()
                .name(request.getName())
                .location(request.getLocation())
                .mapImageUrl(request.getMapImageUrl())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();

        return HeritageSiteResponse.from(repository.save(site));
    }

    /** Cập nhật thông tin di tích. */
    public HeritageSiteResponse update(Long id, HeritageSiteRequest request) {
        HeritageSite site = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy di tích với id: " + id));
        BigDecimal latitude = Boolean.TRUE.equals(request.getClearCoordinates())
                ? null : request.getLatitude() != null ? request.getLatitude() : site.getLatitude();
        BigDecimal longitude = Boolean.TRUE.equals(request.getClearCoordinates())
                ? null : request.getLongitude() != null ? request.getLongitude() : site.getLongitude();
        validateCoordinatePair(latitude, longitude);

        if (request.getName() != null && !request.getName().isBlank()) {
            // Kiểm tra tên trùng với di tích khác (không phải chính nó)
            boolean nameTaken = repository.findByNameContainingIgnoreCase(request.getName())
                    .stream()
                    .anyMatch(s -> !s.getId().equals(id));
            if (nameTaken) {
                throw new RuntimeException("Tên '" + request.getName() + "' đã được sử dụng bởi di tích khác.");
            }
            site.setName(request.getName());
        }

        if (request.getLocation() != null)    site.setLocation(request.getLocation());
        if (request.getMapImageUrl() != null) site.setMapImageUrl(request.getMapImageUrl());
        if (Boolean.TRUE.equals(request.getClearCoordinates())) {
            site.setLatitude(null);
            site.setLongitude(null);
        } else {
            if (request.getLatitude() != null)  site.setLatitude(request.getLatitude());
            if (request.getLongitude() != null) site.setLongitude(request.getLongitude());
        }

        return HeritageSiteResponse.from(repository.save(site));
    }

    private void validateCoordinatePair(BigDecimal latitude, BigDecimal longitude) {
        if ((latitude == null) != (longitude == null)) {
            throw new IllegalArgumentException("Vĩ độ và kinh độ phải được cung cấp cùng nhau.");
        }
        if (latitude != null) {
            validateCoordinate(latitude.doubleValue(), longitude.doubleValue(), "Tọa độ di tích");
        }
    }

    private void validateCoordinate(double latitude, double longitude, String label) {
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException(label + ": vĩ độ phải nằm trong khoảng -90 đến 90.");
        }
        if (!Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException(label + ": kinh độ phải nằm trong khoảng -180 đến 180.");
        }
    }

    private double haversineKm(double latitude1, double longitude1, double latitude2, double longitude2) {
        double lat1 = Math.toRadians(latitude1);
        double lat2 = Math.toRadians(latitude2);
        double deltaLat = Math.toRadians(latitude2 - latitude1);
        double deltaLon = Math.toRadians(longitude2 - longitude1);
        double sinHalfLat = Math.sin(deltaLat / 2);
        double sinHalfLon = Math.sin(deltaLon / 2);
        double haversine = sinHalfLat * sinHalfLat
                + Math.cos(lat1) * Math.cos(lat2) * sinHalfLon * sinHalfLon;
        double centralAngle = 2 * Math.atan2(Math.sqrt(Math.min(1, haversine)),
                Math.sqrt(Math.max(0, 1 - haversine)));
        return EARTH_RADIUS_KM * centralAngle;
    }

    /** Xóa di tích theo id. */
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy di tích với id: " + id);
        }
        repository.deleteById(id);
    }
}
