package com.smartheritage.beacon.service;

import com.smartheritage.beacon.dto.DeviceRegisterRequest;
import com.smartheritage.beacon.dto.DeviceResponse;
import com.smartheritage.beacon.entity.Device;
import com.smartheritage.beacon.repository.DeviceRepository;
import com.smartheritage.common.web.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test logic DeviceService của beacon-service (Khoa - Sprint 5): đăng ký FCM token,
 * upsert theo token (đổi chủ), gửi thử thông báo, xoá khi đăng xuất.
 * Repository + FcmService đều mock - không cần Firebase thật khi chạy test.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeviceServiceTest {

    private static final Long USER_ID = 7L;

    @Mock DeviceRepository deviceRepository;
    @Mock FcmService fcmService;

    private DeviceService service;

    @BeforeEach
    void setUp() {
        service = new DeviceService(deviceRepository, fcmService);
    }

    private Device device(Long id, Long userId, String token, String platform) {
        return Device.builder()
                .id(id).userId(userId).fcmToken(token).platform(platform)
                .build();
    }

    private DeviceRegisterRequest request(String token, String platform) {
        DeviceRegisterRequest request = new DeviceRegisterRequest();
        request.setToken(token);
        request.setPlatform(platform);
        return request;
    }

    // ─────────────── register ───────────────

    @Test
    void registerNewTokenSavesAndSendsWelcome() {
        when(deviceRepository.findByFcmToken(anyString())).thenReturn(Optional.empty());
        when(deviceRepository.save(any())).thenAnswer(inv -> {
            Device d = inv.getArgument(0);
            d.setId(5L);
            return d;
        });
        when(fcmService.sendToToken(anyString(), anyString(), anyString())).thenReturn(true);

        DeviceService.DeviceRegistrationResult result =
                service.register(USER_ID, request("token-fcm-may-thu-1", "Android"));

        assertThat(result.isNew()).isTrue();
        assertThat(result.welcomeSent()).isTrue();
        assertThat(result.device().getUserId()).isEqualTo(USER_ID);
        verify(deviceRepository).save(any(Device.class));
        verify(fcmService).sendToToken(anyString(), anyString(), anyString());
    }

    @Test
    void reRegisterSameTokenMovesOwnershipWithoutDuplicate() {
        Device existing = device(5L, 3L, "token-gan-nhau", "android");
        when(deviceRepository.findByFcmToken("token-gan-nhau")).thenReturn(Optional.of(existing));
        when(deviceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DeviceService.DeviceRegistrationResult result =
                service.register(USER_ID, request("token-gan-nhau", "android"));

        // Cùng 1 dòng, chỉ đổi chủ sang user mới - KHÔNG tạo thêm dòng mới
        assertThat(result.isNew()).isFalse();
        assertThat(result.device().getId()).isEqualTo(5L);
        assertThat(result.device().getUserId()).isEqualTo(USER_ID);
        verify(deviceRepository).save(existing);
    }

    @Test
    void registerRejectsUnknownPlatform() {
        assertThatThrownBy(() -> service.register(USER_ID, request("token...", "windows")))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("android, ios, web");
        verify(deviceRepository, never()).save(any());
    }

    @Test
    void registerWithoutPlatformDefaultsUnknown() {
        when(deviceRepository.findByFcmToken(anyString())).thenReturn(Optional.empty());
        when(deviceRepository.save(any())).thenAnswer(inv -> {
            Device d = inv.getArgument(0);
            d.setId(6L);
            return d;
        });
        when(fcmService.sendToToken(anyString(), anyString(), anyString())).thenReturn(false);

        DeviceService.DeviceRegistrationResult result =
                service.register(USER_ID, request("token-co-truoc", null));

        assertThat(result.device().getPlatform()).isEqualTo("unknown");
        // Chưa cấu hình service account -> welcomeSent = false nhưng đăng ký vẫn thành công
        assertThat(result.welcomeSent()).isFalse();
    }

    // ─────────────── list / send / delete ───────────────

    @Test
    void listMyDevicesMasksTokenPreview() {
        Device d1 = device(5L, USER_ID, "abcdefgh1234567890", "android");
        when(deviceRepository.findByUserIdOrderByUpdatedAtDesc(USER_ID)).thenReturn(List.of(d1));

        List<DeviceResponse> items = service.getMyDevices(USER_ID);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).getTokenPreview()).startsWith("abcdefgh").endsWith("…");
        assertThat(items.get(0).getTokenPreview()).doesNotContain("7890");
    }

    @Test
    void sendTestSendsToEveryMyDevice() {
        when(deviceRepository.findByUserIdOrderByUpdatedAtDesc(USER_ID)).thenReturn(List.of(
                device(5L, USER_ID, "token-1", "android"),
                device(6L, USER_ID, "token-2", "web")));
        when(fcmService.sendToToken(anyString(), anyString(), anyString())).thenReturn(true);

        int sent = service.sendTest(USER_ID, "Tiêu đề", "Nội dung");

        assertThat(sent).isEqualTo(2);
        verify(fcmService).sendToToken("token-1", "Tiêu đề", "Nội dung");
        verify(fcmService).sendToToken("token-2", "Tiêu đề", "Nội dung");
    }

    @Test
    void sendTestWithoutDevicesThrowsBadRequest() {
        when(deviceRepository.findByUserIdOrderByUpdatedAtDesc(USER_ID)).thenReturn(List.of());

        assertThatThrownBy(() -> service.sendTest(USER_ID, "Tiêu đề", "Nội dung"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("chưa đăng ký thiết bị");
    }

    @Test
    void deleteMyDevicesRemovesTokens() {
        service.deleteMyDevices(USER_ID);
        verify(deviceRepository).deleteByUserId(USER_ID);
    }
}
