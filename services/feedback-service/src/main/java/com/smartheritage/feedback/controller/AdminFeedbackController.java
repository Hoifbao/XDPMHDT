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
import java.util.Map;

/**
 * ADMIN kiểm duyệt đánh giá (US15).
 *
 *  GET       /api/admin/feedbacks?status=&artifactId= – Danh sách đánh giá, lọc theo trạng thái và/hoặc hiện vật
 *            (bỏ trống = tất cả)
 *  PUT/PATCH /api/admin/feedbacks/{id}/status – Duyệt (APPROVED) hoặc từ chối (REJECTED).
 *            PUT theo task tuần 4; giữ PATCH để tương thích với Backend cũ (tuần 3).
 *  GET       /api/admin/feedbacks/pending-count – Số đánh giá chờ duyệt cho menu Web Admin (tuần 6)
 *
 * Quyền chỉ ADMIN do Gateway kiểm tra (/api/admin/** → ADMIN).
 * Tuần 5: Gateway phải chuyển /api/admin/feedbacks/** sang feedback-service.
 */
@RestController
@RequestMapping("/api/admin/feedbacks")
@RequiredArgsConstructor
public class AdminFeedbackController {

    private final FeedbackService feedbackService;

    @Operation(summary = "ADMIN xem danh sách đánh giá (mới nhất trước), lọc theo trạng thái và hiện vật",
            description = "Trả mọi trạng thái nếu bỏ trống status. Lọc theo hiện vật không gọi content-service; "
                    + "Web Admin lấy tên hiện vật từ GET /api/artifacts.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "status không phải PENDING/APPROVED/REJECTED hoặc artifactId không hợp lệ")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<FeedbackResponse>>> list(
            @Parameter(description = "PENDING | APPROVED | REJECTED; bỏ trống = tất cả", example = "PENDING")
            @RequestParam(required = false) String status,
            @Parameter(description = "ID hiện vật; bỏ trống = mọi hiện vật", example = "1")
            @RequestParam(required = false) Long artifactId) {
        return ResponseEntity.ok(ApiResponse.ok("Thành công", feedbackService.getFeedbacksForAdmin(status, artifactId)));
    }

    @Operation(summary = "Số đánh giá đang chờ duyệt (hiện trên menu Web Admin)",
            description = "Ví dụ data: {\"pending\": 2}")
    @GetMapping("/pending-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> pendingCount() {
        return ResponseEntity.ok(ApiResponse.ok("Thành công", Map.of("pending", feedbackService.countPending())));
    }

    @Operation(summary = "ADMIN duyệt hoặc từ chối một đánh giá (APPROVED / REJECTED)",
            description = "REJECTED = Ẩn: đánh giá không còn hiện trên app và không tính vào điểm trung bình. "
                    + "Không đặt lại PENDING. Body ví dụ: {\"status\":\"APPROVED\"}")
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
            @Parameter(description = "ID đánh giá", example = "3") @PathVariable Long id,
            @RequestBody FeedbackStatusRequest request) {
        FeedbackResponse response = feedbackService.updateStatus(id, request == null ? null : request.getStatus());
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật trạng thái đánh giá thành công", response));
    }
}
