package com.smartheritage.beacon.controller;

import com.smartheritage.beacon.dto.IBeaconAssignRequest;
import com.smartheritage.beacon.dto.IBeaconBatteryRequest;
import com.smartheritage.beacon.dto.IBeaconPositionRequest;
import com.smartheritage.beacon.dto.IBeaconRequest;
import com.smartheritage.beacon.dto.IBeaconResponse;
import com.smartheritage.beacon.service.BatteryAlertService;
import com.smartheritage.beacon.service.IBeaconService;
import com.smartheritage.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
 *  PUT    /api/ibeacons/{id}/position – Cập nhật vị trí đặt beacon trên bản đồ (cho Ân, W5)
 *  PUT    /api/ibeacons/{id}/battery  – Cập nhật mức pin (W6)
 *  POST   /api/ibeacons/battery-check – Chạy 1 lượt quét pin ngay (W6, demo)
 *  DELETE /api/ibeacons/{id}       – Xóa iBeacon
 */
@Tag(name = "4. iBeacon", description = "CRUD beacon, registry cho Mobile, gán hiện vật (Khoa)")
@RestController
@RequestMapping("/api/ibeacons")
@RequiredArgsConstructor
public class IBeaconController {

    private final IBeaconService beaconService;
    private final BatteryAlertService batteryAlertService;

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

    @Operation(summary = "Cập nhật vị trí đặt beacon trên bản đồ (Sprint 5 - cho Ân)",
            description = "Body: { \"posX\": 12.5, \"posY\": 30.0 } - đơn vị quy ước của bản đồ "
                    + "(x_coord/y_coord trong bảng ibeacons), KHÔNG phải kinh/vĩ độ. "
                    + "Chỉ ADMIN (Gateway tự kiểm theo luật write)")
    @PutMapping("/{id}/position")
    public ResponseEntity<ApiResponse<IBeaconResponse>> updatePosition(
            @PathVariable Long id,
            @Valid @RequestBody IBeaconPositionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật vị trí thành công",
                beaconService.updatePosition(id, request.getPosX(), request.getPosY())));
    }

    @Operation(summary = "W6: Cập nhật mức pin beacon",
            description = "Body: { \"batteryLevel\": 15 } (0–100). Gói tin iBeacon thường không chứa mức pin "
                    + "nên Admin cập nhật/giả lập bằng API này; pin hồi lại >= 20% sẽ xóa cờ cảnh báo. "
                    + "Chỉ ADMIN (Gateway tự kiểm theo luật write)")
    @PutMapping("/{id}/battery")
    public ResponseEntity<ApiResponse<IBeaconResponse>> updateBattery(
            @PathVariable Long id,
            @Valid @RequestBody IBeaconBatteryRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật pin thành công",
                beaconService.updateBattery(id, request.getBatteryLevel())));
    }

    @Operation(summary = "W6: Chạy 1 lượt quét pin ngay (demo thay vì chờ 15 phút)",
            description = "Quét beacon ACTIVE pin < 20% -> phát sự kiện beacon.battery.low; "
                    + "trả về số beacon vừa cảnh báo. Chỉ ADMIN (Gateway tự kiểm theo luật write)")
    @PostMapping("/battery-check")
    public ResponseEntity<ApiResponse<Integer>> runBatteryCheck() {
        int alerted = batteryAlertService.checkAndAlert();
        return ResponseEntity.ok(ApiResponse.ok(
                alerted > 0 ? "Đã cảnh báo " + alerted + " beacon pin thấp" : "Không có beacon nào pin thấp",
                alerted));
    }
}
