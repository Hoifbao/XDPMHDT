package com.smartheritage.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Thiết bị iBeacon, gán cho một hiện vật */
@Entity
@Table(name = "iBeacons",
        uniqueConstraints = @UniqueConstraint(name = "uk_ibeacon_uuid_major_minor",
                columnNames = {"uuid", "major", "minor"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IBeacon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 36)
    private String uuid;

    @Column(nullable = false)
    private Integer major;

    @Column(nullable = false)
    private Integer minor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artifact_id", foreignKey = @ForeignKey(name = "fk_ibeacon_artifact"))
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Artifact artifact;

    /** ACTIVE | INACTIVE */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
