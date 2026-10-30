package com.smartheritage.backend.controller;

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

@RestController
@RequestMapping("/api/me/visits")
@RequiredArgsConstructor
public class VisitHistoryController {
    private final VisitHistoryService visitHistoryService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<VisitHistoryResponse>>> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        User user = currentUserProvider.requireCurrentUser();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100));
        return ResponseEntity.ok(ApiResponse.ok("Lịch sử tham quan", visitHistoryService.list(user.getId(), pageable)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Boolean>> record(
        @Valid @RequestBody RecordVisitRequest request) {
        User user = currentUserProvider.requireCurrentUser();
        boolean created = visitHistoryService.record(user, request);
        return ResponseEntity.ok(ApiResponse.ok(created ? "Đã ghi nhận lượt xem" : "Sự kiện đã được đồng bộ trước đó", created));
    }

    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<SyncVisitsResponse>> sync(
        @Valid @RequestBody SyncVisitsRequest request) {
        User user = currentUserProvider.requireCurrentUser();
        return ResponseEntity.ok(ApiResponse.ok("Đồng bộ lịch sử hoàn tất",
            visitHistoryService.sync(user, request.getEvents())));
    }
}
