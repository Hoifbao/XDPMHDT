package com.smartheritage.backend.controller;

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
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StatisticsController {

    private final StatisticsService statisticsService;

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
