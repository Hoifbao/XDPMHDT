package com.smartheritage.beacon.service;

import com.smartheritage.beacon.dto.DeviceRegisterRequest;
import com.smartheritage.beacon.dto.DeviceResponse;
import com.smartheritage.beacon.entity.Device;
import com.smartheritage.beacon.repository.DeviceRepository;
import com.smartheritage.common.web.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

/**
 * Đăng ký/quản lý thiết bị nhận thông báo - Khoa, Sprint 5.
 *
 * userId lấy từ header X-User-Id mà Gateway gắn sau khi kiểm JWT (CurrentUser).
 * Upsert theo fcm_token: token cũ đổi chủ, không tạo dòng trùng.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DeviceService {

    private static final Set<String> VALID_PLATFORMS = Set.of("android", "ios", "web", "unknown");
    private static final String WELCOME_TITLE = "Smart Heritage";
    private static final String WELCOME_BODY =
            "Đăng ký nhận thông báo thành công! Bạn sẽ được nhắc khi đến gần hiện vật.";

    private final DeviceRepository deviceRepository;
    private final FcmService fcmService;

    @Transactional(readOnly = true)
    public List<DeviceResponse> getMyDevices(Long userId) {
        return deviceRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(DeviceResponse::from)
                .toList();
    }

    /**
     * Đăng ký FCM token của máy cho user đăng nhập.
     * @return hàng thời gian gửi FCM chào mừng đã phát đi (demo "Gửi được FCM từ API")
     * hay chưa - tuỳ cấu hình service account.
     */
    public DeviceRegistrationResult register(Long userId, DeviceRegisterRequest request) {
        String token = request.getToken().trim();
        String platform = normalizePlatform(request.getPlatform());

        Device device = deviceRepository.findByFcmToken(token).orElseGet(Device::new);
        boolean isNew = device.getId() == null;
        device.setUserId(userId);
        device.setFcmToken(token);
        device.setPlatform(platform);
        device = deviceRepository.save(device);

        boolean welcomeSent = fcmService.sendToToken(token, WELCOME_TITLE, WELCOME_BODY);
        if (isNew) {
            log.info("[DEVICE] User {} đăng ký thiết bị mới (platform={}, token {}…)", userId, platform,
                    token.substring(0, Math.min(8, token.length())));
        }
        return new DeviceRegistrationResult(DeviceResponse.from(device), isNew, welcomeSent);
    }

    /** Gửi thử 1 thông báo tới tất cả thiết bị của user (demo "Gửi được FCM từ API"). */
    public int sendTest(Long userId, String title, String body) {
        List<Device> devices = deviceRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        if (devices.isEmpty()) {
            throw ApiException.badRequest("Bạn chưa đăng ký thiết bị nhận thông báo nào");
        }
        int sent = 0;
        for (Device device : devices) {
            if (fcmService.sendToToken(device.getFcmToken(), title, body)) {
                sent++;
            }
        }
        return sent;
    }

    /** Xoá toàn bộ thiết bị của user (qua nút đăng xuất, logout trên Mobile). */
    public void deleteMyDevices(Long userId) {
        deviceRepository.deleteByUserId(userId);
    }

    private String normalizePlatform(String platform) {
        if (!StringUtils.hasText(platform) || "unknown".equalsIgnoreCase(platform)) {
            return "unknown";
        }
        String value = platform.trim().toLowerCase();
        if (!VALID_PLATFORMS.contains(value)) {
            throw ApiException.badRequest("platform chỉ nhận: android, ios, web");
        }
        return value;
    }

    /**
     * Kết quả đăng ký: thiết bị (token rút gọn) + trạng thái gửi FCM chào mừng.
     * welcomeSent là kết quả gọi FCM: theo cấu hình FIREBASE_SERVICE_ACCOUNT (chưa cấu hình -> false).
     */
    public record DeviceRegistrationResult(DeviceResponse device, boolean isNew, boolean welcomeSent) {
    }
}
