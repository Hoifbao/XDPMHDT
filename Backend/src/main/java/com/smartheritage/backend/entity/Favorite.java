package com.smartheritage.backend.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity Favorite ánh xạ tới bảng likes.
 * Mỗi người dùng chỉ có thể yêu thích một hiện vật một lần,
 * được xác định bằng khóa chính ghép FavoriteId.
 */
@Entity
@Table(name = "likes")
@Getter
@Setter
@NoArgsConstructor
public class Favorite {

    @EmbeddedId
    private FavoriteId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_likes_user"))
    private User user;

    @MapsId("artifactId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "artifact_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_likes_artifact"))
    private Artifact artifact;

    @Column(name = "saved_at", nullable = false)
    private LocalDateTime savedAt;

    /**
     * Constructor đầy đủ và Builder tương thích với các trường của entity.
     */
    @Builder
    public Favorite(
            FavoriteId id,
            User user,
            Artifact artifact,
            LocalDateTime savedAt) {
        this.id = id;
        this.user = user;
        this.artifact = artifact;
        this.savedAt = savedAt != null
                ? savedAt
                : LocalDateTime.now();
    }

    /**
     * Bảo đảm savedAt được thiết lập trước khi entity được lưu.
     */
    @PrePersist
    void beforeInsert() {
        if (savedAt == null) {
            savedAt = LocalDateTime.now();
        }
    }
}