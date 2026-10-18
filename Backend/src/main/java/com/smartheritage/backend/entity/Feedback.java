package com.smartheritage.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Entity Feedback, ánh xạ tới bảng Feedbacks (vật lý: feedbacks).
 * Hiện thực quan hệ Reviews (Users - Artifacts) của ERD tuần 1,
 * gồm các thuộc tính Rating, Comment, Status, Created_At.
 *
 * Dùng @Getter/@Setter thay cho @Data để equals/hashCode/toString không
 * kéo theo việc nạp lazy quan hệ user.
 */
@Entity
@Table(name = "Feedbacks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Feedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Chưa có Entity Artifact trong Backend (thuộc phần của thành viên khác),
    // nên tạm ánh xạ cột khóa ngoại dạng Long. Khóa ngoại thật nằm ở init_db.sql.
    // Khi Artifact.java có sẵn, đổi thành @ManyToOne tới Artifact.
    @Column(name = "artifact_id", nullable = false)
    private Long artifactId;

    @Column(nullable = false)
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}