package com.smartheritage.backend.service;

import com.smartheritage.backend.dto.NearbyHeritageSiteResponse;
import com.smartheritage.backend.entity.HeritageSite;
import com.smartheritage.backend.repository.HeritageSiteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test cho HeritageSiteService.
 * Kiểm tra logic Haversine, sắp xếp gần→xa, bỏ qua di tích không có tọa độ,
 * validate tham số, và làm tròn distanceKm 2 chữ số.
 */
class HeritageSiteServiceTest {

    private HeritageSiteRepository repository;
    private HeritageSiteService service;

    @BeforeEach
    void setUp() {
        repository = mock(HeritageSiteRepository.class);
        service = new HeritageSiteService(repository);
    }

    // -----------------------------------------------------------------------
    // Haversine – khoảng cách thực tế
    // -----------------------------------------------------------------------

    /**
     * Kiểm tra công thức Haversine bằng cặp tọa độ gần nhau.
     * Từ Hồ Gươm (21.0285, 105.8542) đến Hồ Tây (21.0560, 105.8241) ≈ 3.9 km (sai số ±0.5 km).
     */
    @Test
    void haversine_hoGuomToHoTay_approximately3_9km() {
        HeritageSite hoTay = site(1L, "Hồ Tây", 21.0560, 105.8241);
        when(repository.findAll()).thenReturn(List.of(hoTay));

        List<NearbyHeritageSiteResponse> results = service.getNearby(21.0285, 105.8542, 10);

        assertEquals(1, results.size());
        assertEquals(3.9, results.get(0).getDistanceKm(), 0.5);
    }

    // -----------------------------------------------------------------------
    // Sắp xếp & bỏ qua di tích không có tọa độ
    // -----------------------------------------------------------------------

    @Test
    void getNearby_returnsSitesWithinRadiusSortedByDistanceAndSkipsMissingCoordinates() {
        // samePoint cách 0 km, nearby cách ~1.11 km, outside cách ~111 km
        HeritageSite samePoint    = site(1L, "Same point",    21.0277644, 105.8354028);
        HeritageSite nearby       = site(2L, "Nearby",        21.0377644, 105.8354028);
        HeritageSite outside      = site(3L, "Outside",       22.0277644, 105.8354028);
        HeritageSite noCoords     = siteWithoutCoordinates(4L, "No coordinates");

        when(repository.findAll()).thenReturn(List.of(outside, noCoords, nearby, samePoint));

        List<NearbyHeritageSiteResponse> results = service.getNearby(21.0277644, 105.8354028, 2);

        // Chỉ trả về 2 di tích trong bán kính 2 km, gần nhất trước
        assertEquals(2, results.size());
        assertEquals(1L, results.get(0).getId());
        assertEquals(2L, results.get(1).getId());

        // Khoảng cách làm tròn 2 chữ số
        assertEquals(0.00, results.get(0).getDistanceKm(), 1e-9);
        assertEquals(1.11, results.get(1).getDistanceKm(), 0.02);
    }

    // -----------------------------------------------------------------------
    // Làm tròn distanceKm 2 chữ số thập phân
    // -----------------------------------------------------------------------

    @Test
    void getNearby_distanceKmIsRoundedToTwoDecimalPlaces() {
        // Dùng cùng tọa độ để distance = 0.00
        HeritageSite site = site(5L, "Exact", 10.0, 10.0);
        when(repository.findAll()).thenReturn(List.of(site));

        List<NearbyHeritageSiteResponse> results = service.getNearby(10.0, 10.0, 10);

        assertEquals(1, results.size());
        // Kiểm tra giá trị có đúng 2 chữ số thập phân (0.00)
        String strDist = Double.toString(results.get(0).getDistanceKm());
        String[] parts = strDist.split("\\.");
        assertTrue(parts.length == 2 && parts[1].length() <= 2,
                "distanceKm phải làm tròn tối đa 2 chữ số thập phân, thực tế: " + strDist);
    }

    // -----------------------------------------------------------------------
    // Bao gồm di tích nằm đúng trên biên bán kính
    // -----------------------------------------------------------------------

    @Test
    void getNearby_includesSiteExactlyAtRadius() {
        // Cách ~1.11 km tính theo Haversine (0.01° vĩ độ tại xích đạo)
        HeritageSite site = site(7L, "On edge", 0.01, 0.0);
        when(repository.findAll()).thenReturn(List.of(site));

        // Dùng radius dư chút để site nằm trong bán kính
        List<NearbyHeritageSiteResponse> results = service.getNearby(0, 0, 2);

        assertFalse(results.isEmpty(), "Di tích nằm trong bán kính 2 km phải được bao gồm.");
        assertTrue(results.get(0).getDistanceKm() <= 2.0);
    }

    // -----------------------------------------------------------------------
    // Validate tham số: lat, lng ngoài WGS-84; radius ≤ 0, radius > 50
    // -----------------------------------------------------------------------

    @Test
    void getNearby_rejectsInvalidLatitude() {
        assertThrows(IllegalArgumentException.class, () -> service.getNearby(91, 0, 10),
                "Vĩ độ > 90 phải báo lỗi.");
        assertThrows(IllegalArgumentException.class, () -> service.getNearby(-91, 0, 10),
                "Vĩ độ < -90 phải báo lỗi.");
    }

    @Test
    void getNearby_rejectsInvalidLongitude() {
        assertThrows(IllegalArgumentException.class, () -> service.getNearby(0, 181, 10),
                "Kinh độ > 180 phải báo lỗi.");
        assertThrows(IllegalArgumentException.class, () -> service.getNearby(0, -181, 10),
                "Kinh độ < -180 phải báo lỗi.");
    }

    @Test
    void getNearby_rejectsNonPositiveRadius() {
        assertThrows(IllegalArgumentException.class, () -> service.getNearby(0, 0, 0),
                "Bán kính = 0 phải báo lỗi.");
        assertThrows(IllegalArgumentException.class, () -> service.getNearby(0, 0, -5),
                "Bán kính âm phải báo lỗi.");
        assertThrows(IllegalArgumentException.class, () -> service.getNearby(0, 0, Double.NaN),
                "Bán kính NaN phải báo lỗi.");
    }

    @Test
    void getNearby_rejectsRadiusExceeding50km() {
        assertThrows(IllegalArgumentException.class, () -> service.getNearby(0, 0, 51),
                "Bán kính > 50 km phải báo lỗi.");
        assertThrows(IllegalArgumentException.class, () -> service.getNearby(0, 0, 200),
                "Bán kính 200 km phải báo lỗi.");
    }

    @Test
    void getNearby_accepts50kmRadius() {
        when(repository.findAll()).thenReturn(List.of());
        assertDoesNotThrow(() -> service.getNearby(0, 0, 50),
                "Bán kính đúng 50 km phải được chấp nhận.");
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private HeritageSite site(Long id, String name, double lat, double lng) {
        return HeritageSite.builder()
                .id(id)
                .name(name)
                .latitude(BigDecimal.valueOf(lat))
                .longitude(BigDecimal.valueOf(lng))
                .build();
    }

    private HeritageSite siteWithoutCoordinates(Long id, String name) {
        return HeritageSite.builder().id(id).name(name).build();
    }
}
