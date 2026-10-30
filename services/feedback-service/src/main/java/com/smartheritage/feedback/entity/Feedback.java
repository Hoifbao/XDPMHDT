package com.smartheritage.feedback.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity Feedback, ánh xạ tới feedback_db.feedbacks (Infra/db/06_feedback.sql).
 *
 * Sau khi tách service: không còn @ManyToOne User / Artifact (2 bảng đó nằm ở auth_db, content_db).
 * Người gửi và hiện vật chỉ lưu bằng id; tên người gửi được chụp lại vào reviewerName lúc gửi.
 */
@Entity
@Table(name = "feedbacks")
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
}
