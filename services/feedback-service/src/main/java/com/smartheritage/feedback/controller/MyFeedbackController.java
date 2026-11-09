package com.smartheritage.feedback.controller;

import com.smartheritage.common.api.ApiResponse;
import com.smartheritage.common.web.CurrentUser;
import com.smartheritage.feedback.dto.FeedbackResponse;
import com.smartheritage.feedback.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * GET /api/feedbacks/me – "Đánh giá của tôi" trên Mobile (tuần 6).
 *
 * Gateway coi GET /api/** là công khai nhưng vẫn gắn X-User-Id khi có token hợp lệ,
 * nên thiếu đăng nhập sẽ bị CurrentUser.requireUserId() trả 401 tại đây.
 */
@RestController
@RequestMapping("/api/feedbacks/me")
@RequiredArgsConstructor
public class MyFeedbackController {

    private final FeedbackService feedbackService;

    @Operation(summary = "Du khách xem các đánh giá của chính mình, kèm trạng thái",
            description = "Trả mọi trạng thái (PENDING, APPROVED, REJECTED), mới cập nhật lên trước. "
                    + "Người dùng lấy từ header X-User-Id do Gateway gắn.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Thành công (danh sách có thể rỗng)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "Chưa đăng nhập")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<FeedbackResponse>>> getMine() {
        Long userId = CurrentUser.requireUserId();
        return ResponseEntity.ok(ApiResponse.ok("Thành công", feedbackService.getMyFeedbacks(userId)));
    }
}
