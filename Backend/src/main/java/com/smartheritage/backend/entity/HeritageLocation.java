package com.smartheritage.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "HeritageLocations")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HeritageLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 500)
    private String address;

    /** Vĩ độ (latitude) */
    @Column(precision = 10)
    private Double latitude;

    /** Kinh độ (longitude) */
    @Column(precision = 10)
    private Double longitude;

    /** URL ảnh đại diện của di tích */
    @Column(name = "cover_image_url", length = 1000)
    private String coverImageUrl;

    /** Thành phố / tỉnh */
    @Column(length = 100)
    private String city;

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

    @OneToMany(mappedBy = "location", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Artifact> artifacts = new ArrayList<>();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
