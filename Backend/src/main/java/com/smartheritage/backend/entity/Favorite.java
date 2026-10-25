package com.smartheritage.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Hiện vật yêu thích của du khách (Phú).
 * Ánh xạ bảng likes trong Infra/init_db.sql – quan hệ Likes (Users N-N Artifacts) của ERD.
 * Một user chỉ thích một hiện vật một lần nhờ khoá chính ghép (user_id, artifact_id).
 */
@Entity
@Table(name = "likes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Favorite {

    @EmbeddedId
    private FavoriteId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_likes_user"))
    private User user;

    @MapsId("artifactId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "artifact_id", foreignKey = @ForeignKey(name = "fk_likes_artifact"))
    private Artifact artifact;

    @Column(name = "saved_at", nullable = false)
    @Builder.Default
    private LocalDateTime savedAt = LocalDateTime.now();
}
