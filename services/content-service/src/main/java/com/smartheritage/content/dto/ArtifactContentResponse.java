package com.smartheritage.content.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Dữ liệu trả về cho một bản nội dung ngôn ngữ.
 */
@Data
@Builder
public class ArtifactContentResponse {

    private Long id;
    private Long artifactId;
    private String languageCode;
    private String title;
    private String description;
    private String audioUrl;
    private String videoUrl;
    private String imageUrls;
    private String transcript;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
