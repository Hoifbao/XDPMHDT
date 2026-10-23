package com.smartheritage.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Thiết bị iBeacon (bảng ibeacons trong Infra/init_db.sql).
 *
 * Quan hệ theo ERD:
 *  - Located_At : Heritage_Sites (0,n) - iBeacons (1,1)  => site_id NOT NULL
 *  - Assigned_To: Artifacts      (0,n) - iBeacons (0,1)  => artifact_id NULL (beacon chưa gán hiện vật)
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

    /** Di tích nơi đặt beacon (bắt buộc) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_ibeacons_site"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private HeritageSite site;

    /** Hiện vật được gán (có thể chưa gán) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artifact_id",
            foreignKey = @ForeignKey(name = "fk_ibeacons_artifact"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Artifact artifact;

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
