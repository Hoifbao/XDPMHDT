package com.smartheritage.analytics.controller;

import com.smartheritage.analytics.dto.StatisticsResponse;
import com.smartheritage.analytics.service.StatisticsService;
import com.smartheritage.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDate;

/**
 * REST Controller – Thống kê Dashboard (Tuấn)
 *
 *  GET /api/statistics/overview?top=5&siteId=&from=&to=
 *      Tổng lượt xem, số khách, top hiện vật, phân bố ngôn ngữ, điểm đánh giá TB,
 *      thời gian dừng TB, lượt theo ngày, phản hồi gần đây.
 *
 * Chỉ ADMIN: Gateway kiểm tra quyền trước khi chuyển request tới đây.
 */
@Tag(name = "7. Thống kê", description = "Số liệu Dashboard, chỉ ADMIN (Tuấn)")
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @Operation(summary = "Tổng quan: lượt xem, số khách, top hiện vật, ngôn ngữ, điểm đánh giá, thời gian dừng TB",
            description = "Đọc analytics_db (NiFi đồng bộ mỗi 1 phút). Bỏ trống siteId/from/to thì không lọc.")
    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<StatisticsResponse>> getOverview(
            @Parameter(description = "Số hiện vật trong top, 1 - 50") @RequestParam(defaultValue = "5") int top,
            @Parameter(description = "Chỉ tính hiện vật của di tích này") @RequestParam(required = false) Long siteId,
            @Parameter(description = "Từ ngày (yyyy-MM-dd), tính cả ngày này")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Đến ngày (yyyy-MM-dd), tính cả ngày này")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.ok("Thành công", statisticsService.getOverview(top, siteId, from, to)));
    }

    // GlobalExceptionHandler dùng chung không bắt lỗi sai kiểu tham số (vd from=abc) nên sẽ thành 500
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest().body(ApiResponse.error("Tham số " + e.getName() + " không đúng định dạng"));
    }
}
