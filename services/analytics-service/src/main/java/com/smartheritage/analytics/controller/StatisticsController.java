package com.smartheritage.analytics.controller;

import com.smartheritage.analytics.dto.RealtimeResponse;
import com.smartheritage.analytics.dto.StatisticsResponse;
import com.smartheritage.analytics.service.ReportExportService;
import com.smartheritage.analytics.service.StatisticsService;
import com.smartheritage.common.api.ApiResponse;
import com.smartheritage.common.web.ApiException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
 *  GET /api/statistics/realtime?siteId=   (Tuần 6)
 *      Số liệu hôm nay từ sự kiện RabbitMQ, cập nhật ngay không chờ NiFi.
 *
 *  GET /api/statistics/export?format=xlsx|pdf&siteId=&from=&to=   (Tuần 6)
 *      Tải file báo cáo, cùng số liệu với /overview theo bộ lọc.
 *
 * Chỉ ADMIN: Gateway kiểm tra quyền trước khi chuyển request tới đây.
 */
@Tag(name = "7. Thống kê", description = "Số liệu Dashboard, chỉ ADMIN (Tuấn)")
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final StatisticsService statisticsService;
    private final ReportExportService reportExportService;

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

    @Operation(summary = "Số liệu hôm nay (thời gian thực): lượt xem, đánh giá mới, điểm TB, top hiện vật",
            description = "Đọc rt_daily_counters do sự kiện visit.recorded, feedback.created cập nhật ngay. "
                    + "Chỉ tính hôm nay, không cộng với số liệu /overview (NiFi).")
    @GetMapping("/realtime")
    public ResponseEntity<ApiResponse<RealtimeResponse>> getRealtime(
            @Parameter(description = "Chỉ tính hiện vật của di tích này") @RequestParam(required = false) Long siteId) {
        return ResponseEntity.ok(ApiResponse.ok("Thành công", statisticsService.getRealtime(siteId)));
    }

    @Operation(summary = "Xuất báo cáo thống kê ra Excel (xlsx) hoặc PDF",
            description = "Cùng số liệu với /overview theo bộ lọc (nguồn NiFi). Excel có 4 sheet: Tổng quan, "
                    + "Top hiện vật, Ngôn ngữ, Đánh giá. PDF nhúng font tiếng Việt. Chỉ ADMIN (VISITOR bị Gateway trả 403).")
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @Parameter(description = "xlsx hoặc pdf") @RequestParam String format,
            @Parameter(description = "Chỉ tính hiện vật của di tích này") @RequestParam(required = false) Long siteId,
            @Parameter(description = "Từ ngày (yyyy-MM-dd), tính cả ngày này")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Đến ngày (yyyy-MM-dd), tính cả ngày này")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        byte[] file;
        MediaType type;
        switch (format) {
            case "xlsx" -> {
                file = reportExportService.exportExcel(siteId, from, to);
                type = XLSX;
            }
            case "pdf" -> {
                file = reportExportService.exportPdf(siteId, from, to);
                type = MediaType.APPLICATION_PDF;
            }
            default -> throw ApiException.badRequest("format phải là xlsx hoặc pdf");
        }
        String fileName = "bao-cao-thong-ke-" + LocalDate.now() + "." + format;
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileName).build().toString())
                .body(file);
    }

    // GlobalExceptionHandler dùng chung không bắt lỗi sai kiểu tham số (vd from=abc) nên sẽ thành 500
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest().body(ApiResponse.error("Tham số " + e.getName() + " không đúng định dạng"));
    }
}
