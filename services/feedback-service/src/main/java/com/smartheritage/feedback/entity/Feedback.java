package com.smartheritage.feedback.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity Feedback, ánh xạ tới feedback_db.feedbacks (Infra/db/06_feedback.sql).
 *
 * Sau khi tách service: không còn @ManyToOne User / Artifact (2 bảng đó nằm ở auth_db, content_db).
 * Người gửi và hiện vật chỉ lưu bằng id; tên người gửi được chụp lại vào reviewerName lúc gửi.
 * Mỗi người chỉ có 1 đánh giá cho 1 hiện vật (gửi lại thì ghi đè, xem FeedbackService.submitFeedback).
 */
@Entity
@Table(name = "feedbacks",
        uniqueConstraints = @UniqueConstraint(name = "uq_feedbacks_user_artifact", columnNames = {"user_id", "artifact_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Feedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** id trong auth_db.users, lấy từ header X-User-Id (Gateway gắn từ token) */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** id trong content_db.artifacts */
    @Column(name = "artifact_id", nullable = false)
    private Long artifactId;

    /** Tên hiển thị của người gửi tại thời điểm gửi (fullName); null thì API trả "Khách" */
    @Column(name = "reviewer_name", length = 100)
    private String reviewerName;

    @Column(nullable = false)
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    /** PENDING | APPROVED | REJECTED */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Lần sửa cuối (du khách sửa đánh giá hoặc ADMIN đổi trạng thái). NiFi dùng cột này để bắt thay đổi.
     * Gán ở đây (cùng đồng hồ với createdAt), không để MySQL tự gán vì container MySQL chạy giờ UTC.
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
