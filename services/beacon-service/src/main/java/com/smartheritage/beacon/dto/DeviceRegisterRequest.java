package com.smartheritage.beacon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request đăng ký thiết bị nhận thông báo - Khoa, Sprint 5.
 * Mobile gọi POST /api/me/devices sau khi lấy được FCM token từ Firebase SDK.
 */
@Data
public class DeviceRegisterRequest {

    /** FCM token từ Firebase SDK trên máy (bắt buộc) */
    @NotBlank(message = "Thiếu FCM token của thiết bị")
    @Size(max = 512, message = "FCM token tối đa 512 ký tự")
    private String token;

    /** android | ios | web | unknown (mặc định unknown) */
    @Size(max = 20, message = "platform tối đa 20 ký tự")
    private String platform;
}
