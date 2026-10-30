package com.smartheritage.feedback.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Payload khi ADMIN duyệt hoặc từ chối một đánh giá. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackStatusRequest {

    @Schema(description = "Trạng thái mới: APPROVED hoặc REJECTED", example = "APPROVED")
    private String status;
}
