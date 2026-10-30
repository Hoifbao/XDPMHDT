package com.smartheritage.backend.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class VisitHistoryResponse {

    private Long id;
    private Long artifactId;
    private String artifactName;
    private String thumbnailUrl;
    private String siteName;
    private LocalDateTime detectedAt;
    private Integer durationSeconds;
    private String source;
    private String clientEventId;

    /**
     * Constructor chính, hỗ trợ Lombok Builder.
     * Thứ tự tương thích với record của nhánh feature.
     */
    @Builder
    public VisitHistoryResponse(
            Long id,
            Long artifactId,
            String artifactName,
            String siteName,
            String thumbnailUrl,
            LocalDateTime detectedAt,
            Integer durationSeconds,
            String source,
            String clientEventId) {
        this.id = id;
        this.artifactId = artifactId;
        this.artifactName = artifactName;
        this.siteName = siteName;
        this.thumbnailUrl = thumbnailUrl;
        this.detectedAt = detectedAt;
        this.durationSeconds = durationSeconds;
        this.source = source;
        this.clientEventId = clientEventId;
    }

    /**
     * Constructor 7 tham số tương thích với class cũ ở main.
     */
    public VisitHistoryResponse(
            Long id,
            Long artifactId,
            String artifactName,
            String thumbnailUrl,
            String siteName,
            LocalDateTime detectedAt,
            Integer durationSeconds) {
        this(
                id,
                artifactId,
                artifactName,
                siteName,
                thumbnailUrl,
                detectedAt,
                durationSeconds,
                null,
                null);
    }

    // Tương thích với accessor của record cũ.

    public Long id() {
        return id;
    }

    public Long artifactId() {
        return artifactId;
    }

    public String artifactName() {
        return artifactName;
    }

    public String siteName() {
        return siteName;
    }

    public String thumbnailUrl() {
        return thumbnailUrl;
    }

    public LocalDateTime detectedAt() {
        return detectedAt;
    }

    public Integer durationSeconds() {
        return durationSeconds;
    }

    public String source() {
        return source;
    }

    public String clientEventId() {
        return clientEventId;
    }
}