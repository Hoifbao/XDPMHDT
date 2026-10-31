package com.smartheritage.feedback.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Dữ liệu trả về của một đánh giá (giữ nguyên các trường như Backend cũ để Web/Mobile không phải sửa).
 * Không trả userId, email hay username của người đánh giá; chỉ trả tên hiển thị (reviewerName).
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FeedbackResponse {

    @Schema(example = "7")
    private Long id;

    @Schema(description = "ID hiện vật được đánh giá", example = "1")
    private Long artifactId;

    @Schema(description = "Số sao, từ 1 đến 5", example = "5")
    private Integer rating;

    @Schema(description = "Bình luận, có thể null", example = "Hiện vật rất đẹp và nhiều thông tin.")
    private String comment;

    @Schema(description = "Tên hiển thị của người đánh giá; không có thì \"Khách\"", example = "Nguyễn Văn An")
    private String reviewerName;

    @Schema(description = "PENDING | APPROVED | REJECTED", example = "PENDING")
    private String status;

    @Schema(example = "2026-10-30T09:30:00")
    private LocalDateTime createdAt;

    @Schema(description = "Lần sửa cuối (du khách sửa đánh giá hoặc ADMIN đổi trạng thái)", example = "2026-10-31T08:15:00")
    private LocalDateTime updatedAt;
}
