package com.smartheritage.backend.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import com.smartheritage.backend.dto.ApiResponse;
import com.smartheritage.backend.dto.RecordVisitRequest;
import com.smartheritage.backend.dto.SyncVisitsRequest;
import com.smartheritage.backend.dto.SyncVisitsResponse;
import com.smartheritage.backend.dto.VisitHistoryResponse;
import com.smartheritage.backend.entity.User;
import com.smartheritage.backend.security.CurrentUserProvider;
import com.smartheritage.backend.service.VisitHistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "5. Cá nhân hóa - Lịch sử", description = "Lịch sử tham quan, đồng bộ offline theo clientEventId (Phú)")
@RestController
@RequestMapping("/api/me/visits")
@RequiredArgsConstructor
public class VisitHistoryController {
    private final VisitHistoryService visitHistoryService;
    private final CurrentUserProvider currentUserProvider;

    @Operation(summary = "Lịch sử tham quan của tôi (phân trang, mới nhất trước)")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<VisitHistoryResponse>>> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        User user = currentUserProvider.requireCurrentUser();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100));
        return ResponseEntity.ok(ApiResponse.ok("Lịch sử tham quan", visitHistoryService.list(user.getId(), pageable)));
    }

    @Operation(summary = "Ghi 1 lượt xem; trùng clientEventId thì không ghi (data = false)")
    @PostMapping
    public ResponseEntity<ApiResponse<Boolean>> record(
        @Valid @RequestBody RecordVisitRequest request) {
        User user = currentUserProvider.requireCurrentUser();
        boolean created = visitHistoryService.record(user, request);
        return ResponseEntity.ok(ApiResponse.ok(created ? "Đã ghi nhận lượt xem" : "Sự kiện đã được đồng bộ trước đó", created));
    }

    @Operation(summary = "Đồng bộ tối đa 100 lượt xem offline; trả received/created/duplicates")
    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<SyncVisitsResponse>> sync(
        @Valid @RequestBody SyncVisitsRequest request) {
        User user = currentUserProvider.requireCurrentUser();
        return ResponseEntity.ok(ApiResponse.ok("Đồng bộ lịch sử hoàn tất",
            visitHistoryService.sync(user, request.getEvents())));
    }
}
