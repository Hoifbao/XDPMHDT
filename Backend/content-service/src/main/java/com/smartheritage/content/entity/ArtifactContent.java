package com.smartheritage.content.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Lưu trữ nội dung đa phương tiện theo ngôn ngữ cho từng hiện vật.
 * Mỗi bản ghi tương ứng với 1 ngôn ngữ (vi, en, fr, ja, ...).
 */
@Entity
@Table(
    name = "ArtifactContents",
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

    /** Hiện vật chứa nội dung này */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "artifact_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Artifact artifact;

    /** Mã ngôn ngữ: vi, en, fr, zh, ja, ko, ... */
    @Column(name = "language_code", nullable = false, length = 10)
    private String languageCode;

    /** Tên hiện vật trong ngôn ngữ này */
    @Column(name = "title", length = 200)
    private String title;

    /** Bài mô tả / giới thiệu lịch sử */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** URL file audio thuyết minh */
    @Column(name = "audio_url", length = 1000)
    private String audioUrl;

    /** URL video giới thiệu */
    @Column(name = "video_url", length = 1000)
    private String videoUrl;

    /** Danh sách URL ảnh, lưu dạng JSON string "["url1","url2"]" */
    @Column(name = "image_urls", columnDefinition = "TEXT")
    private String imageUrls;

    /** Phụ đề / transcript cho audio (hỗ trợ người khiếm thính) */
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
