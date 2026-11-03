package com.smartheritage.personalization.controller;

import com.smartheritage.common.api.ApiResponse;
import com.smartheritage.common.web.ApiException;
import com.smartheritage.common.web.CurrentUser;
import com.smartheritage.personalization.dto.VisitHistoryResponse;
import com.smartheritage.personalization.service.VisitHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Tag(name = "Personalization - Admin visits", description = "Nguồn lượt xem cho analytics/NiFi")
@RestController
@RequestMapping("/api/admin/visits")
@RequiredArgsConstructor
public class AdminVisitHistoryController {
    private final VisitHistoryService visitHistoryService;

    @Operation(summary = "Danh sách lượt xem; có thể lọc ngày và di tích")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<VisitHistoryResponse>>> list(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) Long siteId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        if (!CurrentUser.isAdmin()) {
            throw ApiException.forbidden("Chỉ ADMIN được truy cập dữ liệu lượt xem tổng hợp.");
        }
        if (from != null && to != null && to.isBefore(from)) {
            throw ApiException.badRequest("Tham số to phải bằng hoặc sau from.");
        }
        if (siteId != null && siteId <= 0) {
            throw ApiException.badRequest("siteId phải lớn hơn 0.");
        }
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100));
        return ResponseEntity.ok(ApiResponse.ok("Nguồn lượt xem cho analytics-service",
                visitHistoryService.listForAdmin(
                        from == null ? null : from.atStartOfDay(),
                        to == null ? null : to.plusDays(1).atStartOfDay(),
                        siteId,
                        pageable)));
    }
}
