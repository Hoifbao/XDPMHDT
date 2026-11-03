package com.smartheritage.content.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

/**
 * Payload khi tạo mới hoặc cập nhật hiện vật.
 */
@Data
@Schema(description = "Payload thêm/sửa hiện vật (sửa: chỉ gửi trường cần đổi)")
public class ArtifactRequest {

    /** ID của địa điểm di tích */
    @Schema(description = "ID di tích (bắt buộc khi thêm), được kiểm tra qua site-service", example = "1")
    private Long siteId;
    @Schema(description = "Tên cũ của siteId, giữ cho client cũ; gửi cả 2 thì phải bằng nhau", example = "1")
    private Long locationId;

    @Schema(description = "Tên hiện vật, bắt buộc, tối đa 200 ký tự", example = "Khuê Văn Các")
    private String name;

    @Schema(description = "Mã hiện vật, không trùng", example = "KVC-01")
    private String artifactCode;

    @Schema(description = "Mô tả ngắn", example = "Gác vuông ở trung tâm Văn Miếu, biểu tượng của Hà Nội.")
    private String description;

    @Schema(description = "Ảnh đại diện, URL http(s)", example = "https://example.com/media/kvc-thumb.jpg")
    private String thumbnailUrl;

    @Schema(description = "Niên đại", example = "Năm 1805 (triều Nguyễn)")
    private String period;

    @Schema(description = "Chất liệu", example = "Gỗ, gạch")
    private String material;

    @Schema(description = "Kích thước", example = "Cao 9 m")
    private String dimensions;

    @Schema(description = "[Cũ] Beacon UUID gắn trực tiếp, không trùng; luồng chuẩn quản lý ở beacon-service", example = "FDA50693-A4E2-4FB1-AFCF-C6EB07647825")
    private String beaconUuid;

    /** ACTIVE | INACTIVE */
    @Schema(description = "ACTIVE hoặc INACTIVE", example = "ACTIVE")
    private String status;

    /** Nội dung đa ngôn ngữ (tuỳ chọn, tạo kèm hiện vật) */
    @Schema(description = "Bản dịch kèm theo; khi thêm bắt buộc có bản vi. Gửi khi sửa thì thay cả danh sách")
    private List<ArtifactContentRequest> contents;
    @Schema(description = "Media kèm theo (mediaType, fileUrl, caption). Gửi khi sửa thì thay cả danh sách")
    private List<com.smartheritage.content.entity.MediaResource> media;
}
