package com.smartheritage.backend.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import com.smartheritage.backend.dto.ApiResponse;
import com.smartheritage.backend.dto.VisitHistoryResponse;
import com.smartheritage.backend.entity.User;
import com.smartheritage.backend.security.CurrentUserProvider;
import com.smartheritage.backend.service.VisitHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

/** Read-only feed used by analytics-service; no aggregation is performed here. */
@Tag(name = "5. Cá nhân hóa - Admin", description = "Nguồn lượt xem cho analytics-service, chỉ ADMIN (Phú)")
@RestController
@RequestMapping("/api/admin/visits")
@RequiredArgsConstructor
public class AdminVisitHistoryController {
    private final VisitHistoryService visitHistoryService;
    private final CurrentUserProvider currentUserProvider;

    @Operation(summary = "ADMIN: danh sách lượt xem theo from/to/siteId cho analytics-service")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<VisitHistoryResponse>>> list(
        @RequestParam(required = false) LocalDate from,
        @RequestParam(required = false) LocalDate to,
        @RequestParam(required = false) Long siteId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size) {
        User user = currentUserProvider.requireCurrentUser();
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Chỉ ADMIN được truy cập dữ liệu lượt xem tổng hợp.");
        }
        if (from != null && to != null && to.isBefore(from)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tham số to phải bằng hoặc sau from.");
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
