package com.smartheritage.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Dùng khi tạo hoặc cập nhật một bản nội dung ngôn ngữ cho hiện vật.
 */
@Data
@Schema(description = "Nội dung một ngôn ngữ của hiện vật (ghi đè theo languageCode)")
public class ArtifactContentRequest {

    /** Mã ngôn ngữ: vi, en, fr, zh, ja, ko */
    @Schema(description = "Mã ngôn ngữ: vi hoặc en", example = "en")
    private String languageCode;

    @Schema(description = "Tiêu đề, bắt buộc, tối đa 200 ký tự", example = "Khue Van Cac")
    private String title;

    @Schema(description = "Mô tả, bắt buộc", example = "Khue Van Cac, the Constellation of Literature Pavilion, is a symbol of the Temple of Literature.")
    private String description;

    @Schema(description = "Audio thuyết minh, URL http(s)", example = "https://example.com/audio/kvc-en.mp3")
    private String audioUrl;

    @Schema(description = "Video, URL http(s)", example = "https://example.com/media/kvc.mp4")
    private String videoUrl;

    /** JSON array string của ảnh: ["url1","url2"] */
    @Schema(description = "Chuỗi JSON mảng URL ảnh", example = "[\"https://example.com/media/kvc-1.jpg\"]")
    private String imageUrls;

    @Schema(description = "Lời thuyết minh dạng chữ", example = "Khue Van Cac was built in 1805...")
    private String transcript;
}
