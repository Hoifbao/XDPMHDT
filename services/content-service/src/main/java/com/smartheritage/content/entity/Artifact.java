package com.smartheritage.content.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Hiện vật (bảng content_db.artifacts - script Infra/db/03_content.sql).
 * Sau khi tách: không còn @ManyToOne HeritageSite, chỉ giữ cột site_id; tên di tích hỏi site-service.
 */
@Entity
@Table(name = "artifacts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Artifact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** id trong site_db.heritage_sites (không khóa ngoại) */
    @Column(name = "site_id", nullable = false)
    private Long siteId;

    @Column(nullable = false, length = 200)
    private String name;

    /** Mã định danh vật lý trên hiện vật (tuỳ chọn) */
    @Column(name = "artifact_code", length = 50, unique = true)
    private String artifactCode;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    /** Niên đại */
    @Column(name = "period", length = 100)
    private String period;

    /** Chất liệu */
    @Column(length = 200)
    private String material;

    /** Kích thước */
    @Column(length = 200)
    private String dimensions;

    /** Tạm thời; liên kết chính thức ở beacon_db.ibeacons */
    @Column(name = "beacon_uuid", length = 100, unique = true)
    private String beaconUuid;

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

    @OneToMany(mappedBy = "artifact", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ArtifactContent> contents = new ArrayList<>();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
