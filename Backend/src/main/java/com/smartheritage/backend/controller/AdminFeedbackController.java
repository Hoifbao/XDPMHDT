package com.smartheritage.backend.controller;

import com.smartheritage.backend.dto.ApiResponse;
import com.smartheritage.backend.dto.FeedbackResponse;
import com.smartheritage.backend.dto.FeedbackStatusRequest;
import com.smartheritage.backend.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

/**
 * REST Controller – ADMIN kiểm duyệt đánh giá (US15)
 *
 *  PATCH /api/admin/feedbacks/{id}/status – Duyệt (APPROVED) hoặc từ chối (REJECTED) một đánh giá
 *
 * Quyền: chỉ ADMIN (SecurityConfig: /api/admin/** → hasRole(ADMIN)). Không dùng @CrossOrigin
 * vì CORS đã cấu hình chung ở SecurityConfig / Gateway.
 * Quy ước lỗi: IllegalArgumentException → 400, NoSuchElementException → 404.
 */
@RestController
@RequestMapping("/api/admin/feedbacks")
@RequiredArgsConstructor
public class AdminFeedbackController {

    private final FeedbackService feedbackService;

    @Operation(summary = "ADMIN duyệt hoặc từ chối một đánh giá (APPROVED / REJECTED)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Cập nhật trạng thái thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "status thiếu hoặc không phải APPROVED/REJECTED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "Chưa đăng nhập hoặc token hết hạn"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "Không phải ADMIN"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Không tìm thấy đánh giá")
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<FeedbackResponse>> updateStatus(
            @PathVariable Long id,
            @RequestBody FeedbackStatusRequest request) {
        try {
            FeedbackResponse response = feedbackService.updateStatus(id, request.getStatus());
            return ResponseEntity.ok(ApiResponse.ok("Cập nhật trạng thái đánh giá thành công", response));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}