package com.smartheritage.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Dữ liệu trả về đầy đủ của một hiện vật.
 */
@Data
@Builder
@Schema(description = "Chi tiết hiện vật")
public class ArtifactResponse {

    @Schema(example = "1")
    private Long id;
    @Schema(example = "1")
    private Long siteId;
    @Schema(description = "Bằng siteId (giữ cho client cũ)", example = "1")
    private Long locationId;
    @Schema(description = "Tên di tích", example = "Văn Miếu - Quốc Tử Giám")
    private String locationName;
    @Schema(example = "Khuê Văn Các")
    private String name;
    @Schema(example = "KVC-01")
    private String artifactCode;
    @Schema(example = "Gác vuông ở trung tâm Văn Miếu, biểu tượng của Hà Nội.")
    private String description;
    @Schema(example = "https://example.com/media/kvc-thumb.jpg")
    private String thumbnailUrl;
    @Schema(example = "Năm 1805 (triều Nguyễn)")
    private String period;
    @Schema(example = "Gỗ, gạch")
    private String material;
    private String dimensions;
    private String beaconUuid;
    @Schema(example = "ACTIVE")
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** Nội dung đa ngôn ngữ */
    @Schema(description = "Tất cả bản dịch")
    private List<ArtifactContentResponse> contents;
    @Schema(description = "Ảnh/audio/video")
    private List<com.smartheritage.content.entity.MediaResource> media;
    @Schema(description = "Ngôn ngữ client yêu cầu (?lang)", example = "en")
    private String requestedLanguage;
    @Schema(description = "Ngôn ngữ thực tế của content", example = "en")
    private String languageCode;
    @Schema(description = "true = chưa có bản dịch yêu cầu, đang trả bản tiếng Việt", example = "false")
    private boolean fallback;
    @Schema(description = "Nội dung theo ngôn ngữ đã chọn")
    private ArtifactContentResponse content;
}
