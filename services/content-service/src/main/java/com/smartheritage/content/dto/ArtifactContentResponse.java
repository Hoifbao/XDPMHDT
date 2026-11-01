package com.smartheritage.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Dữ liệu trả về cho một bản nội dung ngôn ngữ.
 */
@Data
@Builder
@Schema(description = "Một bản nội dung ngôn ngữ")
public class ArtifactContentResponse {

    @Schema(example = "2")
    private Long id;
    @Schema(example = "1")
    private Long artifactId;
    @Schema(example = "en")
    private String languageCode;
    @Schema(example = "Khue Van Cac")
    private String title;
    @Schema(example = "Khue Van Cac, the Constellation of Literature Pavilion, is a symbol of the Temple of Literature.")
    private String description;
    @Schema(example = "https://example.com/audio/kvc-en.mp3")
    private String audioUrl;
    private String videoUrl;
    private String imageUrls;
    private String transcript;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
