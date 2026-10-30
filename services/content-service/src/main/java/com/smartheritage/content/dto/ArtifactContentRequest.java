package com.smartheritage.content.dto;

import lombok.Data;

/**
 * Dùng khi tạo hoặc cập nhật một bản nội dung ngôn ngữ cho hiện vật.
 */
@Data
public class ArtifactContentRequest {

    /** Mã ngôn ngữ: vi, en, fr, zh, ja, ko */
    private String languageCode;

    private String title;

    private String description;

    private String audioUrl;

    private String videoUrl;

    /** JSON array string của ảnh: ["url1","url2"] */
    private String imageUrls;

    private String transcript;
}
