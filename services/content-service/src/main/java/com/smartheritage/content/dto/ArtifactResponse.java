package com.smartheritage.content.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Dữ liệu trả về đầy đủ của một hiện vật.
 */
@Data
@Builder
public class ArtifactResponse {

    private Long id;
    private Long siteId;
    private Long locationId;
    private String locationName;
    private String name;
    private String artifactCode;
    private String description;
    private String thumbnailUrl;
    private String period;
    private String material;
    private String dimensions;
    private String beaconUuid;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Nội dung đa ngôn ngữ */
    private List<ArtifactContentResponse> contents;
    private List<com.smartheritage.content.entity.MediaResource> media;
    private String requestedLanguage;
    private String languageCode;
    private boolean fallback;
    private ArtifactContentResponse content;
}
