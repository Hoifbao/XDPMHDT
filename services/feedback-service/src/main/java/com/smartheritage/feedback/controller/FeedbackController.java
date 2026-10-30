package com.smartheritage.feedback.controller;

import com.smartheritage.common.api.ApiResponse;
import com.smartheritage.common.web.CurrentUser;
import com.smartheritage.feedback.dto.FeedbackRequest;
import com.smartheritage.feedback.dto.FeedbackResponse;
import com.smartheritage.feedback.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Đánh giá (Feedback & Rating) của du khách cho hiện vật (US08).
 *
 *  POST /api/artifacts/{artifactId}/feedbacks – Gửi đánh giá, trạng thái ban đầu PENDING (VISITOR hoặc ADMIN)
 *  GET  /api/artifacts/{artifactId}/feedbacks – Các đánh giá ĐÃ DUYỆT của hiện vật (công khai)
 *
 * Quyền do Gateway kiểm tra; người gửi đọc từ header X-User-Id (CurrentUser), không nhận từ body.
 */
@RestController
@RequestMapping("/api/artifacts/{artifactId}/feedbacks")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @Operation(summary = "Du khách gửi đánh giá (số sao + bình luận) cho hiện vật",
            description = "Người gửi lấy từ header X-User-Id do Gateway gắn. Đánh giá mới ở trạng thái PENDING, "
                    + "chỉ hiện công khai sau khi ADMIN duyệt.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201",
                    description = "Gửi đánh giá thành công, trạng thái PENDING"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Dữ liệu không hợp lệ (rating, comment) hoặc hiện vật không hoạt động"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "Không có header X-User-Id (chưa đăng nhập)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Không tìm thấy hiện vật"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503",
                    description = "content-service không phản hồi")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<FeedbackResponse>> createFeedback(
            @PathVariable Long artifactId,
            @RequestBody FeedbackRequest request) {
        Long userId = CurrentUser.requireUserId();
        FeedbackResponse response = feedbackService.createFeedback(artifactId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Gửi đánh giá thành công, đánh giá đang chờ duyệt", response));
    }

    @Operation(summary = "Danh sách đánh giá đã duyệt của hiện vật (mới nhất trước)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Thành công (danh sách có thể rỗng)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Không tìm thấy hiện vật hoặc hiện vật không hoạt động"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503",
                    description = "content-service không phản hồi")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<FeedbackResponse>>> getApprovedFeedbacks(@PathVariable Long artifactId) {
        return ResponseEntity.ok(ApiResponse.ok("Thành công", feedbackService.getApprovedFeedbacks(artifactId)));
    }
}
