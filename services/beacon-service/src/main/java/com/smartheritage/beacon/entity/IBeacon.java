package com.smartheritage.beacon.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Thiết bị iBeacon (bảng ibeacons - Infra/db/04_beacon.sql, Tường).
 *
 * Sau khi tách service KHÔNG còn quan hệ sang bảng của service khác (tọa độ bảng gốc
 * nằm ở site_db.heritage_sites và content_db.artifacts):
 *  - site_id    BIGINT NOT NULL  (di tích nơi đặt beacon; kiểm tra qua site-service)
 *  - artifact_id BIGINT NULL     (hiện vật đang gán, null = chưa gán; kiểm tra qua content-service)
 *
 * Bộ (uuid, major, minor) là định danh duy nhất của 1 beacon ngoài thực tế:
 * cùng UUID cho cả khu di tích, Major = khu vực, Minor = từng thiết bị.
 */
@Entity
// Viết thường "ibeacons": Spring Boot đổi "iBeacons" thành "i_beacons" (bảng khác)
@Table(name = "ibeacons",
        uniqueConstraints = @UniqueConstraint(name = "uq_ibeacons_identity",
                columnNames = {"uuid", "major", "minor"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IBeacon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Di tích nơi đặt beacon (bắt buộc) - tên di tích lấy qua site-service */
    @Column(name = "site_id", nullable = false)
    private Long siteId;

    /** Hiện vật được gán (có thể chưa gán) - tên hiện vật lấy qua content-service */
    @Column(name = "artifact_id")
    private Long artifactId;

    @Column(nullable = false, length = 36)
    private String uuid;

    @Column(nullable = false)
    private Integer major;

    @Column(nullable = false)
    private Integer minor;

    /** Toạ độ đặt beacon trên sơ đồ di tích (đơn vị do bản đồ quy ước, tuần 5 làm cùng Ân) */
    @Column(name = "x_coord")
    private Double posX;

    @Column(name = "y_coord")
    private Double posY;

    /** Phần trăm pin 0–100, null = chưa có số liệu */
    @Column(name = "battery_level")
    private Integer batteryLevel;

    /** Lần gần nhất đã gửi cảnh báo pin thấp (W6). null = chưa cảnh báo.
     *  Đặt khi gửi beacon.battery.low; xóa khi pin >= 20 (sạc/thay pin) để cho phép cảnh báo lần sau. */
    @Column(name = "battery_alerted_at")
    private LocalDateTime batteryAlertedAt;

    /** ACTIVE | INACTIVE */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

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
