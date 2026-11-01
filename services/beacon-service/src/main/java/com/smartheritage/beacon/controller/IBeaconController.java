package com.smartheritage.beacon.controller;

import com.smartheritage.beacon.dto.IBeaconAssignRequest;
import com.smartheritage.beacon.dto.IBeaconRequest;
import com.smartheritage.beacon.dto.IBeaconResponse;
import com.smartheritage.beacon.service.IBeaconService;
import com.smartheritage.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller – iBeacon của beacon-service (giữ nguyên hợp đồng API với Backend cũ).
 *
 * Base URL: /api/ibeacons (qua Gateway http://localhost:8080; cổng riêng 8084 để test).
 * Gateway kiểm tra JWT/RBAC: GET công khai, POST/PUT/DELETE chỉ ADMIN, gắn X-User-* vào header.
 *
 * Endpoints:
 *  GET    /api/ibeacons            – Danh sách tất cả iBeacon (?siteId=1 để lọc theo di tích)
 *  GET    /api/ibeacons/{id}       – Chi tiết iBeacon
 *  GET    /api/ibeacons/registry   – Danh sách beacon ACTIVE cho Mobile
 *  POST   /api/ibeacons            – Tạo iBeacon mới
 *  POST   /api/ibeacons/{id}/assign – Gán (artifactId) hoặc bỏ gán (artifactId = null)
 *  PUT    /api/ibeacons/{id}       – Cập nhật iBeacon
 *  DELETE /api/ibeacons/{id}       – Xóa iBeacon
 */
@Tag(name = "4. iBeacon", description = "CRUD beacon, registry cho Mobile, gán hiện vật (Khoa)")
@RestController
@RequestMapping("/api/ibeacons")
@RequiredArgsConstructor
public class IBeaconController {

    private final IBeaconService beaconService;

    @Operation(summary = "Danh sách iBeacon", description = "?siteId=1 để lọc theo di tích")
    @GetMapping
    public ResponseEntity<ApiResponse<List<IBeaconResponse>>> getBeacons(
            @RequestParam(required = false) Long siteId) {
        return ResponseEntity.ok(ApiResponse.ok("Thành công", beaconService.getBeacons(siteId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<IBeaconResponse>> getBeaconById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Thành công", beaconService.getBeaconById(id)));
    }

    @Operation(summary = "Danh sách beacon ACTIVE (uuid, major, minor, artifactId) để Mobile đối chiếu khi quét")
    @GetMapping("/registry")
    public ResponseEntity<ApiResponse<List<IBeaconResponse>>> getRegistry() {
        return ResponseEntity.ok(ApiResponse.ok("Thành công", beaconService.getRegistry()));
    }

    @Operation(summary = "Gán hiện vật cho beacon (artifactId = null để bỏ gán); hiện vật phải cùng di tích")
    @PostMapping("/{id}/assign")
    public ResponseEntity<ApiResponse<IBeaconResponse>> assignArtifact(
            @PathVariable Long id,
            @RequestBody IBeaconAssignRequest request) {
        IBeaconResponse response = beaconService.assignArtifact(id, request.getArtifactId());
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật gán hiện vật thành công", response));
    }

    @Operation(summary = "Tạo iBeacon mới", description = "siteId bắt buộc (kiểm tra qua site-service); uuid định dạng chuẩn")
    @PostMapping
    public ResponseEntity<ApiResponse<IBeaconResponse>> createBeacon(@RequestBody IBeaconRequest request) {
        IBeaconResponse response = beaconService.createBeacon(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo iBeacon thành công", response));
    }

    @Operation(summary = "Cập nhật iBeacon")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<IBeaconResponse>> updateBeacon(
            @PathVariable Long id,
            @RequestBody IBeaconRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật iBeacon thành công", beaconService.updateBeacon(id, request)));
    }

    @Operation(summary = "Xóa iBeacon")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBeacon(@PathVariable Long id) {
        beaconService.deleteBeacon(id);
        return ResponseEntity.ok(ApiResponse.ok("Xóa iBeacon thành công", null));
    }
}
