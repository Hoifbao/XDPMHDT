package com.smartheritage.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * Dữ liệu tóm tắt hiện vật – dùng cho danh sách (không kèm contents).
 */
@Data
@Builder
@Schema(description = "Hiện vật dạng tóm tắt (danh sách)")
public class ArtifactSummaryResponse {

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
    @Schema(example = "https://example.com/media/kvc-thumb.jpg")
    private String thumbnailUrl;
    @Schema(example = "Năm 1805 (triều Nguyễn)")
    private String period;
    @Schema(example = "Gỗ, gạch")
    private String material;
    private String beaconUuid;
    @Schema(example = "ACTIVE")
    private String status;
}
