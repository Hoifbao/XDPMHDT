package com.smartheritage.beacon.dto;

import com.smartheritage.beacon.entity.Device;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Response thiết bị đăng ký nhận thông báo - Khoa, Sprint 5.
 * Token trả về rút gọn (8 ký tự đầu) để Web/Mobile xem danh sách nhưng không lộ token.
 */
@Data
@Builder
public class DeviceResponse {

    private Long id;

    private Long userId;

    private String tokenPreview;

    private String platform;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public static DeviceResponse from(Device device) {
        String token = device.getFcmToken();
        return DeviceResponse.builder()
                .id(device.getId())
                .userId(device.getUserId())
                .tokenPreview(token == null ? null
                        : token.substring(0, Math.min(8, token.length())) + "…")
                .platform(device.getPlatform())
                .createdAt(device.getCreatedAt())
                .updatedAt(device.getUpdatedAt())
                .build();
    }
}
