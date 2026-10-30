package com.smartheritage.feedback.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Điểm trung bình của một hiện vật, chỉ tính các đánh giá ĐÃ DUYỆT (APPROVED),
 * giống danh sách đánh giá du khách nhìn thấy.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RatingSummaryResponse {

    @Schema(example = "1")
    private Long artifactId;

    @Schema(description = "Điểm trung bình, làm tròn 1 chữ số thập phân; null nếu chưa có đánh giá", example = "4.5")
    private Double averageRating;

    @Schema(description = "Tổng số đánh giá đã duyệt", example = "2")
    private long totalRatings;

    @Schema(description = "Số lượt theo từng mức sao (luôn đủ 5 khóa từ 5 xuống 1)",
            example = "{\"5\": 1, \"4\": 1, \"3\": 0, \"2\": 0, \"1\": 0}")
    private Map<Integer, Long> distribution;
}
