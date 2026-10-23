package com.smartheritage.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Artifacts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Artifact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Thuộc khu di tích nào (bảng heritage_sites - entity HeritageSite của Ân) */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private HeritageSite site;

    @Column(nullable = false, length = 200)
    private String name;

    /** Mã định danh vật lý trên hiện vật (tuỳ chọn) */
    @Column(name = "artifact_code", length = 50, unique = true)
    private String artifactCode;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** URL ảnh thumbnail chính */
    @Column(name = "thumbnail_url", length = 1000)
    private String thumbnailUrl;

    /** Năm / thế kỷ niên đại */
    @Column(name = "period", length = 100)
    private String period;

    /** Chất liệu / vật liệu */
    @Column(length = 200)
    private String material;

    /** Kích thước */
    @Column(length = 200)
    private String dimensions;

    /** UUID của iBeacon gắn với hiện vật này */
    @Column(name = "beacon_uuid", length = 100)
    private String beaconUuid;

    /** Trạng thái: ACTIVE | INACTIVE */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    /** Danh sách nội dung đa phương tiện theo ngôn ngữ */
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
