package com.smartheritage.beacon.controller;

import com.smartheritage.beacon.dto.DeviceRegisterRequest;
import com.smartheritage.beacon.dto.DeviceResponse;
import com.smartheritage.beacon.service.DeviceService;
import com.smartheritage.common.api.ApiResponse;
import com.smartheritage.common.web.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller – thiết bị + thông báo của beacon-service (Khoa, Sprint 5).
 *
 * Base URL: /api/me/devices (qua Gateway http://localhost:8080; route beacon cho
 * /api/me/devices/** do GatewayRoutesConfig định tuyến về beacon-service).
 * Gateway kiểm tra JWT: tất cả các đường dẫn này chỉ dành cho người đã đăng nhập;
 * service lấy userId từ header X-User-* (CurrentUser, KHÔNG tự kiểm tra JWT).
 *
 * Endpoints:
 *  POST   /api/me/devices          – Đăng ký FCM token (gửi luôn thông báo chào mừng nếu có cấu hình;
 *                                     ADMIN tự được đăng ký topic "admins" - W6)
 *  GET    /api/me/devices          – Danh sách thiết bị của tôi
 *  POST   /api/me/devices/test     – Gửi thử 1 thông báo tới các thiết bị của tôi (demo FCM)
 *  DELETE /api/me/devices          – Xoá toàn bộ token của tôi (dùng khi đăng xuất)
 */
@Tag(name = "5. Thiết bị & Thông báo", description = "Đăng ký FCM token theo user, gửi thử FCM (Khoa, Sprint 5)")
@RestController
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @Operation(summary = "Đăng ký thiết bị nhận thông báo",
            description = "Body: { \"token\": \"<fcm-token-500-ky-tu>\", \"platform\": \"android\" }. "
                    + "Upsert theo token; nếu đã cấu hình service account thì gửi luôn thông báo chào mừng. "
                    + "ADMIN sẽ được server tự đăng ký token vào topic \"admins\" nhận cảnh báo pin beacon (W6)")
    @PostMapping("/api/me/devices")
    public ResponseEntity<ApiResponse<Map<String, Object>>> registerDevice(
            @Valid @RequestBody DeviceRegisterRequest request) {
        Long userId = CurrentUser.requireUserId();
        DeviceService.DeviceRegistrationResult result = deviceService.register(userId, request);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("device", result.device());
        data.put("isNew", result.isNew());
        data.put("welcomeNotificationSent", result.welcomeSent());
        data.put("adminTopicSubscribed", result.adminTopicSubscribed());
        return ResponseEntity.status(201)
                .body(ApiResponse.ok("Đăng ký nhận thông báo thành công", data));
    }

    @Operation(summary = "Danh sách thiết bị của tôi (token rút gọn)")
    @GetMapping("/api/me/devices")
    public ResponseEntity<ApiResponse<List<DeviceResponse>>> getMyDevices() {
        Long userId = CurrentUser.requireUserId();
        return ResponseEntity.ok(ApiResponse.ok("Thành công", deviceService.getMyDevices(userId)));
    }

    @Operation(summary = "Gửi thử 1 thông báo tới các thiết bị của tôi",
            description = "Cần đã cấu hình FIREBASE_SERVICE_ACCOUNT; dùng để demo tiêu chí "
                    + "\"Gửi FCM thành công lên 1 điện thoại đã đăng ký token\"")
    @PostMapping("/api/me/devices/test")
    public ResponseEntity<ApiResponse<Integer>> sendTestNotification() {
        Long userId = CurrentUser.requireUserId();
        int sent = deviceService.sendTest(userId,
                "Smart Heritage",
                "Đây là thông báo thử từ beacon-service. Khi đến gần hiện vật nào cũng có nhắc như thế này.");
        return ResponseEntity.ok(ApiResponse.ok("Đã gửi thông báo thử tới " + sent + " thiết bị", sent));
    }

    @Operation(summary = "Xoá toàn bộ thiết bị của tôi (đăng xuất)")
    @DeleteMapping("/api/me/devices")
    public ResponseEntity<ApiResponse<Void>> deleteMyDevices() {
        Long userId = CurrentUser.requireUserId();
        deviceService.deleteMyDevices(userId);
        return ResponseEntity.ok(ApiResponse.ok("Đã xoá thông tin thiết bị", null));
    }
}
