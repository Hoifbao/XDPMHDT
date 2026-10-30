package com.smartheritage.content.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Nội dung theo ngôn ngữ của hiện vật (bảng content_db.artifact_contents), mỗi ngôn ngữ 1 bản. */
@Entity
@Table(
        name = "artifact_contents",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_artifact_language",
                columnNames = {"artifact_id", "language_code"}
        )
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArtifactContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "artifact_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Artifact artifact;

    /** vi | en */
    @Column(name = "language_code", nullable = false, length = 10)
    private String languageCode;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "audio_url", length = 1000)
    private String audioUrl;

    @Column(name = "video_url", length = 1000)
    private String videoUrl;

    /** JSON array string: ["url1","url2"] */
    @Column(name = "image_urls", columnDefinition = "TEXT")
    private String imageUrls;

    @Column(name = "transcript", columnDefinition = "TEXT")
    private String transcript;

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
