package com.smartheritage.backend.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import com.smartheritage.backend.dto.ApiResponse;
import com.smartheritage.backend.dto.FeedbackRequest;
import com.smartheritage.backend.dto.FeedbackResponse;
import com.smartheritage.backend.security.SecurityUtils;
import com.smartheritage.backend.service.FeedbackService;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * REST Controller – Đánh giá (Feedback & Rating) của du khách cho hiện vật
 *
 * Base URL: /api/artifacts/{artifactId}/feedbacks
 *
 * Endpoints:
 *  POST /api/artifacts/{artifactId}/feedbacks – Gửi đánh giá (số sao + bình luận), trạng thái ban đầu PENDING.
 *                                               Cần đăng nhập; người gửi lấy từ token, không nhận từ body.
 *  GET  /api/artifacts/{artifactId}/feedbacks – Xem các đánh giá ĐÃ ĐƯỢC DUYỆT của hiện vật
 *
 * Quy ước lỗi: IllegalArgumentException → 400, NoSuchElementException → 404, chưa đăng nhập → 401.
 */
@Tag(name = "6. Đánh giá", description = "Gửi đánh giá, xem đánh giá đã duyệt theo hiện vật (Tường)")
@RestController
@RequestMapping("/api/artifacts/{artifactId}/feedbacks")
@RequiredArgsConstructor
public class FeedbackController {

    private static final String NOT_LOGGED_IN = "Bạn chưa đăng nhập hoặc phiên đăng nhập đã hết hạn!";

    private final FeedbackService feedbackService;

    /**
     * POST /api/artifacts/{artifactId}/feedbacks
     * Body: FeedbackRequest (rating, comment). Người gửi lấy từ access token.
     */
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201",
                    description = "Gửi đánh giá thành công, trạng thái PENDING"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Dữ liệu không hợp lệ (rating, comment) hoặc hiện vật không hoạt động"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "Chưa đăng nhập hoặc token hết hạn"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Không tìm thấy hiện vật hoặc người dùng")
    })
    @Operation(summary = "Gửi đánh giá (1–5 sao + bình luận), userId lấy từ token; chờ duyệt")
    @PostMapping
    public ResponseEntity<ApiResponse<FeedbackResponse>> createFeedback(
            @PathVariable Long artifactId,
            @RequestBody FeedbackRequest request) {
        // Danh tính chỉ lấy từ token đã xác thực (SecurityConfig đã yêu cầu đăng nhập; kiểm tra lại để an toàn)
        Long userId = SecurityUtils.currentUserId().orElse(null);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(NOT_LOGGED_IN));
        }
        try {
            FeedbackResponse response = feedbackService.createFeedback(artifactId, userId, request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok("Gửi đánh giá thành công, đánh giá đang chờ duyệt", response));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * GET /api/artifacts/{artifactId}/feedbacks
     * Chỉ trả các đánh giá đã được duyệt (APPROVED), mới nhất trước.
     */
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Thành công (danh sách có thể rỗng)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Không tìm thấy hiện vật hoặc hiện vật không hoạt động")
    })
    @Operation(summary = "Đánh giá đã duyệt (APPROVED) của hiện vật")
    @GetMapping
    public ResponseEntity<ApiResponse<List<FeedbackResponse>>> getApprovedFeedbacks(
            @PathVariable Long artifactId) {
        try {
            List<FeedbackResponse> feedbacks = feedbackService.getApprovedFeedbacks(artifactId);
            return ResponseEntity.ok(ApiResponse.ok("Thành công", feedbacks));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}