package com.smartheritage.backend.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import com.smartheritage.backend.dto.ApiResponse;
import com.smartheritage.backend.dto.StatisticsResponse;
import com.smartheritage.backend.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller – Thống kê Dashboard (Tuấn)
 *
 *  GET /api/statistics/overview?top=5 – Tổng lượt xem, số khách, top hiện vật,
 *                                       phân bố ngôn ngữ, điểm đánh giá TB,
 *                                       thời gian dừng TB, lượt theo ngày, phản hồi gần đây
 *
 * Khi JWT của Bảo được merge, API này nên chỉ cho ADMIN gọi.
 */
@Tag(name = "7. Thống kê", description = "Số liệu Dashboard, chỉ ADMIN (Tuấn)")
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @Operation(summary = "Tổng quan: lượt xem, số khách, top hiện vật, ngôn ngữ, điểm đánh giá, thời gian dừng TB")
    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<StatisticsResponse>> getOverview(
            @RequestParam(defaultValue = "5") int top) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Thành công", statisticsService.getOverview(top)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
