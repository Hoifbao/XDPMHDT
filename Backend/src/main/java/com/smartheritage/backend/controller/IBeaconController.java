package com.smartheritage.backend.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import com.smartheritage.backend.dto.*;
import com.smartheritage.backend.service.IBeaconService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;
import java.util.List;

/**
 * REST Controller – iBeacon CRUD
 *
 * Base URL: /api/ibeacons
 *
 * Endpoints:
 *  GET    /api/ibeacons        – Danh sách tất cả iBeacon (?siteId=1 để lọc theo di tích)
 *  GET    /api/ibeacons/{id}   – Chi tiết iBeacon
 *  GET    /api/ibeacons/registry – Danh sách beacon ACTIVE cho Mobile
 *  POST   /api/ibeacons        – Tạo iBeacon mới
 *  POST   /api/ibeacons/{id}/assign – Gán (artifactId) hoặc bỏ gán (artifactId = null)
 *  PUT    /api/ibeacons/{id}   – Cập nhật iBeacon
 *  DELETE /api/ibeacons/{id}   – Xóa iBeacon
 */
@Tag(name = "4. iBeacon", description = "CRUD beacon, registry cho Mobile, gán hiện vật (Khoa)")
@RestController
@RequestMapping("/api/ibeacons")
@RequiredArgsConstructor
public class IBeaconController {

    private final IBeaconService beaconService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<IBeaconResponse>>> getBeacons(
            @RequestParam(required = false) Long siteId) {
        return ResponseEntity.ok(ApiResponse.ok("Thành công", beaconService.getBeacons(siteId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<IBeaconResponse>> getBeaconById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Thành công", beaconService.getBeaconById(id)));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    @Operation(summary = "Danh sách beacon ACTIVE (uuid, major, minor, artifactId) để Mobile đối chiếu khi quét")
    @GetMapping("/registry")
    public ResponseEntity<ApiResponse<List<IBeaconResponse>>> getRegistry() {
        return ResponseEntity.ok(ApiResponse.ok("Thành công", beaconService.getRegistry()));
    }

    @Operation(summary = "ADMIN gán hiện vật cho beacon (artifactId = null để bỏ gán); hiện vật phải cùng di tích")
    @PostMapping("/{id}/assign")
    public ResponseEntity<ApiResponse<IBeaconResponse>> assignArtifact(
            @PathVariable Long id,
            @RequestBody IBeaconAssignRequest request) {
        try {
            IBeaconResponse response = beaconService.assignArtifact(id, request.getArtifactId());
            return ResponseEntity.ok(ApiResponse.ok("Cập nhật gán hiện vật thành công", response));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<ApiResponse<IBeaconResponse>> createBeacon(@RequestBody IBeaconRequest request) {
        try {
            IBeaconResponse response = beaconService.createBeacon(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok("Tạo iBeacon thành công", response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<IBeaconResponse>> updateBeacon(
            @PathVariable Long id,
            @RequestBody IBeaconRequest request) {
        try {
            IBeaconResponse response = beaconService.updateBeacon(id, request);
            return ResponseEntity.ok(ApiResponse.ok("Cập nhật iBeacon thành công", response));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBeacon(@PathVariable Long id) {
        try {
            beaconService.deleteBeacon(id);
            return ResponseEntity.ok(ApiResponse.ok("Xóa iBeacon thành công", null));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }
}
