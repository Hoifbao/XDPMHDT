package com.smartheritage.personalization.controller;

import com.smartheritage.common.api.ApiResponse;
import com.smartheritage.common.web.CurrentUser;
import com.smartheritage.personalization.dto.RecordVisitRequest;
import com.smartheritage.personalization.dto.SyncVisitsRequest;
import com.smartheritage.personalization.dto.SyncVisitsResponse;
import com.smartheritage.personalization.dto.VisitHistoryResponse;
import com.smartheritage.personalization.service.VisitHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Personalization - Visit history", description = "Lịch sử tham quan và đồng bộ offline")
@RestController
@RequestMapping("/api/me/visits")
@RequiredArgsConstructor
public class VisitHistoryController {
    private final VisitHistoryService visitHistoryService;

    @Operation(summary = "Lịch sử tham quan của tôi")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<VisitHistoryResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = CurrentUser.requireUserId();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100));
        return ResponseEntity.ok(ApiResponse.ok("Lịch sử tham quan", visitHistoryService.list(userId, pageable)));
    }

    @Operation(summary = "Ghi một lượt xem; trùng clientEventId thì data=false")
    @PostMapping
    public ResponseEntity<ApiResponse<Boolean>> record(@Valid @RequestBody RecordVisitRequest request) {
        boolean created = visitHistoryService.record(CurrentUser.requireUserId(), request);
        return ResponseEntity.ok(ApiResponse.ok(
                created ? "Đã ghi nhận lượt xem" : "Sự kiện đã được đồng bộ trước đó", created));
    }

    @Operation(summary = "Đồng bộ tối đa 100 lượt xem offline")
    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<SyncVisitsResponse>> sync(@Valid @RequestBody SyncVisitsRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Đồng bộ lịch sử hoàn tất",
                visitHistoryService.sync(CurrentUser.requireUserId(), request.getEvents())));
    }
}
