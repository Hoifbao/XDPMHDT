package com.smartheritage.beacon.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Thiết bị đăng ký nhận thông báo (bảng devices - Infra/db/04_beacon.sql, Khoa - Sprint 5).
 *
 * Lưu FCM token theo user (auth_db.users - không FK, kiểm tra qua header X-User-Id của Gateway):
 *  - 1 user có thể đăng ký nhiều máy (1 token = 1 máy).
 *  - Token đăng ký lại bởi user khác được "chuyển nhượng" (upsert theo fcm_token) -
 *    đúng thiết thực: token chỉ có hiệu lực ở app đang dùng.
 */
@Entity
@Table(name = "devices",
        uniqueConstraints = @UniqueConstraint(name = "uq_devices_token", columnNames = "fcm_token"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Người dùng sở hữu thiết bị (bắt buộc) - id trong auth_db.users */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** FCM token của app trên máy này (duy nhất trên toàn hệ thống) */
    @Column(name = "fcm_token", nullable = false, length = 512)
    private String fcmToken;

    /** android | ios | web | unknown */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String platform = "unknown";

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
