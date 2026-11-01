package com.smartheritage.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Payload khi thêm tài nguyên đa phương tiện cho hiện vật.
 * Tuần 2: chỉ truyền URL dạng text (chưa upload file thật).
 */
@Data
@Schema(description = "Payload thêm media (truyền URL, chưa upload file)")
public class MediaResourceRequest {

    /** IMAGE | AUDIO | VIDEO */
    @Schema(description = "IMAGE, AUDIO hoặc VIDEO", example = "IMAGE")
    private String mediaType;

    @Schema(description = "URL file, bắt buộc", example = "https://example.com/media/kvc-1.jpg")
    private String fileUrl;

    @Schema(description = "Chú thích (tuỳ chọn)", example = "Toàn cảnh Khuê Văn Các")
    private String caption;
}
