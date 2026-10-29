package com.smartheritage.backend.controller;

import com.smartheritage.backend.dto.HeritageSiteRequest;
import com.smartheritage.backend.dto.HeritageSiteResponse;
import com.smartheritage.backend.dto.NearbyHeritageSiteResponse;
import com.smartheritage.backend.service.HeritageSiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller cho Di tích (Heritage Sites).
 * Base URL: /api/heritage-sites
 */
@RestController
@RequestMapping("/api/heritage-sites")
@RequiredArgsConstructor
public class HeritageSiteController {

    private final HeritageSiteService service;

    /** GET /api/heritage-sites – Lấy danh sách tất cả di tích. */
    @GetMapping
    public ResponseEntity<List<HeritageSiteResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    /**
     * GET /api/heritage-sites/nearby?lat=&amp;lng=&amp;radius=
     * Tìm các di tích có tọa độ trong bán kính (km), gần nhất xếp trước.
     *
     * @param lat    vĩ độ WGS-84 (-90 đến 90), bắt buộc
     * @param lng    kinh độ WGS-84 (-180 đến 180), bắt buộc
     * @param radius bán kính tìm kiếm km, tuỳ chọn (mặc định 10, tối đa 100)
     */
    @GetMapping("/nearby")
    public ResponseEntity<?> getNearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "10") double radius) {
        try {
            List<NearbyHeritageSiteResponse> nearby = service.getNearby(lat, lng, radius);
            return ResponseEntity.ok(nearby);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** GET /api/heritage-sites/{id} – Lấy chi tiết một di tích. */
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(service.getById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    /** POST /api/heritage-sites – Tạo mới di tích. */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody HeritageSiteRequest request) {
        try {
            HeritageSiteResponse created = service.create(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    /** PUT /api/heritage-sites/{id} – Cập nhật di tích. */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @RequestBody HeritageSiteRequest request) {
        try {
            return ResponseEntity.ok(service.update(id, request));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    /** DELETE /api/heritage-sites/{id} – Xóa di tích. */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.ok(Map.of("message", "Xóa di tích thành công."));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        }
    }
}
