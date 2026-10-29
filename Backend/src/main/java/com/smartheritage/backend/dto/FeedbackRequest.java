package com.smartheritage.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Payload khi du khách gửi đánh giá (số sao và bình luận) cho một hiện vật.
 * Người gửi KHÔNG nằm trong body: Backend lấy từ access token đã xác thực.
 * Việc kiểm tra dữ liệu (rating nguyên từ 1 đến 5, độ dài bình luận...)
 * được thực hiện thủ công trong FeedbackService.
 */
@Data
public class FeedbackRequest {

    /**
     * Số sao, chỉ chấp nhận giá trị nguyên từ 1 đến 5.
     * Dùng Double để phát hiện giá trị thập phân (ví dụ 4.5) thay vì bị cắt ngầm
     * thành số nguyên; Service kiểm tra rồi mới chuyển sang Integer để lưu.
     */
    @Schema(description = "Số sao, chỉ nhận số nguyên từ 1 đến 5", example = "5")
    private Double rating;

    /** Bình luận (tuỳ chọn, tối đa 500 ký tự sau khi trim). Rỗng được lưu là null. */
    @Schema(description = "Bình luận (tuỳ chọn, tối đa 500 ký tự)", example = "Hiện vật rất đẹp và nhiều thông tin.")
    private String comment;
}