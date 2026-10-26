
package com.smartheritage.backend.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO dùng chung cho chức năng Favorites.
 * Giữ tương thích với constructor và accessor của record cũ.
 */
@Data
@NoArgsConstructor
public class FavoriteResponse {

    private Long artifactId;
    private String artifactName;
    private String thumbnailUrl;
    private Long siteId;
    private String siteName;
    private LocalDateTime savedAt;

    /**
     * Constructor chính, hỗ trợ Lombok Builder.
     */
    @Builder
    public FavoriteResponse(
            Long artifactId,
            String artifactName,
            String thumbnailUrl,
            Long siteId,
            String siteName,
            LocalDateTime savedAt) {
        this.artifactId = artifactId;
        this.artifactName = artifactName;
        this.thumbnailUrl = thumbnailUrl;
        this.siteId = siteId;
        this.siteName = siteName;
        this.savedAt = savedAt;
    }

    /**
     * Constructor tương thích với record 5 trường trước đây.
     */
    public FavoriteResponse(
            Long artifactId,
            String artifactName,
            String siteName,
            String thumbnailUrl,
            LocalDateTime savedAt) {
        this(
                artifactId,
                artifactName,
                thumbnailUrl,
                null,
                siteName,
                savedAt);
    }

    // Tương thích với cách gọi accessor của record cũ.

    public Long artifactId() {
        return artifactId;
    }

    public String artifactName() {
        return artifactName;
    }

    public String thumbnailUrl() {
        return thumbnailUrl;
    }

    public Long siteId() {
        return siteId;
    }

    public String siteName() {
        return siteName;
    }

    public LocalDateTime savedAt() {
        return savedAt;
    }
}
