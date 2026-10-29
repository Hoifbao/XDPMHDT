package com.smartheritage.template.controller;

import com.smartheritage.common.api.ApiResponse;
import com.smartheritage.template.dto.SampleItemRequest;
import com.smartheritage.template.entity.SampleItem;
import com.smartheritage.template.service.SampleItemService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller mẫu. Không cần @CrossOrigin (Gateway lo CORS) và không cần kiểm tra quyền
 * (Gateway đã áp dụng luật: GET công khai, thêm/sửa/xóa chỉ ADMIN).
 */
@RestController
@RequestMapping("/api/samples")
@RequiredArgsConstructor
public class SampleItemController {

    private final SampleItemService service;

    @Operation(summary = "Danh sách mục mẫu")
    @GetMapping
    public ResponseEntity<ApiResponse<List<SampleItem>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(service.findAll()));
    }

    @Operation(summary = "Chi tiết 1 mục; không có -> 404")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SampleItem>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.findById(id)));
    }

    @Operation(summary = "Thêm mục (qua Gateway, ADMIN)")
    @PostMapping
    public ResponseEntity<ApiResponse<SampleItem>> create(@Valid @RequestBody SampleItemRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Thêm thành công!", service.create(req)));
    }

    @Operation(summary = "Xóa mục (qua Gateway, ADMIN)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Xóa thành công!", null));
    }
}
