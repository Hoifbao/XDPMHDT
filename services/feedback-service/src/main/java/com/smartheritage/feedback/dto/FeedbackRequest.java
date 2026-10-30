package com.smartheritage.feedback.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload khi du khách gửi đánh giá (số sao và bình luận) cho một hiện vật.
 * Người gửi KHÔNG nằm trong body: lấy từ header X-User-Id do Gateway gắn sau khi kiểm tra token.
 * Kiểm tra dữ liệu (rating nguyên 1-5, độ dài bình luận) làm trong FeedbackService.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Nội dung đánh giá", example = "{\"rating\": 5, \"comment\": \"Hiện vật rất đẹp và nhiều thông tin.\"}")
public class FeedbackRequest {

    /**
     * Dùng Double để phát hiện giá trị thập phân (vd 4.5) thay vì bị cắt ngầm thành số nguyên.
     */
    @Schema(description = "Số sao, chỉ nhận số nguyên từ 1 đến 5", example = "5")
    private Double rating;

    @Schema(description = "Bình luận (tuỳ chọn, tối đa 500 ký tự). Rỗng được lưu là null.",
            example = "Hiện vật rất đẹp và nhiều thông tin.")
    private String comment;
}
