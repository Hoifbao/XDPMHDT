package com.smartheritage.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Một media của hiện vật")
public class MediaResourceResponse {

    @Schema(example = "1")
    private Long id;
    @Schema(example = "1")
    private Long artifactId;
    @Schema(description = "IMAGE, AUDIO hoặc VIDEO", example = "IMAGE")
    private String mediaType;
    @Schema(example = "https://example.com/media/kvc-1.jpg")
    private String fileUrl;
    @Schema(example = "Toàn cảnh Khuê Văn Các")
    private String caption;
}
