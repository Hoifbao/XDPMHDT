package com.smartheritage.feedback.controller;

import com.smartheritage.common.api.ApiResponse;
import com.smartheritage.feedback.dto.FeedbackResponse;
import com.smartheritage.feedback.dto.FeedbackStatusRequest;
import com.smartheritage.feedback.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ADMIN kiểm duyệt đánh giá (US15).
 *
 *  GET       /api/admin/feedbacks?status=  – Danh sách đánh giá, lọc theo trạng thái (bỏ trống = tất cả)
 *  PUT/PATCH /api/admin/feedbacks/{id}/status – Duyệt (APPROVED) hoặc từ chối (REJECTED).
 *            PUT theo task tuần 4; giữ PATCH để tương thích với Backend cũ (tuần 3).
 *
 * Quyền chỉ ADMIN do Gateway kiểm tra (/api/admin/** → ADMIN).
 * Tuần 5: Gateway phải chuyển /api/admin/feedbacks/** sang feedback-service.
 */
@RestController
@RequestMapping("/api/admin/feedbacks")
@RequiredArgsConstructor
public class AdminFeedbackController {

    private final FeedbackService feedbackService;

    @Operation(summary = "ADMIN xem danh sách đánh giá (mới nhất trước), lọc theo trạng thái")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "status không phải PENDING/APPROVED/REJECTED")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<FeedbackResponse>>> list(
            @Parameter(description = "PENDING | APPROVED | REJECTED; bỏ trống = tất cả", example = "PENDING")
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.ok("Thành công", feedbackService.getFeedbacksForAdmin(status)));
    }

    @Operation(summary = "ADMIN duyệt hoặc từ chối một đánh giá (APPROVED / REJECTED)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Cập nhật trạng thái thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "status thiếu hoặc không phải APPROVED/REJECTED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Không tìm thấy đánh giá")
    })
    @RequestMapping(value = "/{id}/status", method = {RequestMethod.PUT, RequestMethod.PATCH})
    public ResponseEntity<ApiResponse<FeedbackResponse>> updateStatus(
            @PathVariable Long id,
            @RequestBody FeedbackStatusRequest request) {
        FeedbackResponse response = feedbackService.updateStatus(id, request == null ? null : request.getStatus());
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật trạng thái đánh giá thành công", response));
    }
}
