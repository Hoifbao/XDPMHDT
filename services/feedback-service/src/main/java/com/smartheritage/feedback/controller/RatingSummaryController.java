package com.smartheritage.feedback.controller;

import com.smartheritage.common.api.ApiResponse;
import com.smartheritage.feedback.dto.RatingSummaryResponse;
import com.smartheritage.feedback.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /api/artifacts/{artifactId}/rating-summary – điểm trung bình của hiện vật (công khai).
 * Tuần 5: Gateway phải thêm đường dẫn này vào route feedback-service (đặt trước /api/artifacts/**).
 */
@RestController
@RequiredArgsConstructor
public class RatingSummaryController {

    private final FeedbackService feedbackService;

    @Operation(summary = "Điểm trung bình và số lượt theo từng mức sao của hiện vật",
            description = "Chỉ tính đánh giá đã duyệt (APPROVED). Chưa có đánh giá: averageRating = null, totalRatings = 0.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "ID hiện vật không hợp lệ")
    })
    @GetMapping("/api/artifacts/{artifactId}/rating-summary")
    public ResponseEntity<ApiResponse<RatingSummaryResponse>> getRatingSummary(@PathVariable Long artifactId) {
        return ResponseEntity.ok(ApiResponse.ok("Thành công", feedbackService.getRatingSummary(artifactId)));
    }
}
