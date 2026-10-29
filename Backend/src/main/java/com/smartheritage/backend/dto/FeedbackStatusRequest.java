package com.smartheritage.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/** Payload khi ADMIN duyệt hoặc từ chối một đánh giá. */
@Data
public class FeedbackStatusRequest {

    @Schema(description = "Trạng thái mới: APPROVED hoặc REJECTED", example = "APPROVED")
    private String status;
}
