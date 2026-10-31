package com.smartheritage.site.controller;

import com.smartheritage.common.api.ApiResponse;
import com.smartheritage.site.dto.HeritageSiteRequest;
import com.smartheritage.site.dto.HeritageSiteResponse;
import com.smartheritage.site.dto.NearbyHeritageSiteResponse;
import com.smartheritage.site.service.HeritageSiteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Di tích (Heritage Sites). Base URL: /api/heritage-sites
 * Không @CrossOrigin, không kiểm tra quyền: Gateway lo CORS và luật GET công khai,
 * thêm/sửa/xóa chỉ ADMIN. Lỗi: ApiException -> GlobalExceptionHandler trả {success:false, message}.
 */
@Tag(name = "2. Di tích", description = "CRUD di tích, tìm di tích gần (Ân)")
@RestController
@RequestMapping("/api/heritage-sites")
@RequiredArgsConstructor
public class HeritageSiteController {

    private final HeritageSiteService service;

    @Operation(summary = "Danh sách di tích")
    @GetMapping
    public ResponseEntity<ApiResponse<List<HeritageSiteResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(service.getAll()));
    }

    /**
     * GET /api/heritage-sites/nearby?lat=&lng=&radius=
     * Tìm các di tích có tọa độ trong bán kính (km), gần nhất xếp trước.
     *
     * @param lat    vĩ độ WGS-84 (-90 đến 90), bắt buộc
     * @param lng    kinh độ WGS-84 (-180 đến 180), bắt buộc
     * @param radius bán kính tìm kiếm km, tuỳ chọn (mặc định 5, tối đa 50)
     */
    @Operation(summary = "Di tích gần vị trí (Haversine)",
            description = "Sắp gần → xa; radius km, mặc định 5, tối đa 50; tọa độ sai trả 400")
    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<List<NearbyHeritageSiteResponse>>> getNearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "5") double radius) {
        return ResponseEntity.ok(ApiResponse.ok(service.getNearby(lat, lng, radius)));
    }

    @Operation(summary = "Chi tiết di tích", description = "Không tồn tại trả 404 (content/beacon-service dùng để kiểm tra siteId)")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<HeritageSiteResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(service.getById(id)));
    }

    @Operation(summary = "Thêm di tích")
    @PostMapping
    public ResponseEntity<ApiResponse<HeritageSiteResponse>> create(@RequestBody HeritageSiteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo di tích thành công", service.create(request)));
    }

    @Operation(summary = "Sửa di tích",
            description = "Cập nhật một phần: bỏ trống trường nào thì giữ nguyên; clearCoordinates = true để xóa tọa độ")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<HeritageSiteResponse>> update(@PathVariable Long id,
                                                                   @RequestBody HeritageSiteRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật di tích thành công", service.update(id, request)));
    }

    @Operation(summary = "Xóa di tích")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Xóa di tích thành công", null));
    }
}
