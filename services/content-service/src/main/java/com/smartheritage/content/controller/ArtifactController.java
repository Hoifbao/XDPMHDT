package com.smartheritage.content.controller;

import com.smartheritage.common.api.ApiResponse;
import com.smartheritage.content.dto.*;
import com.smartheritage.content.service.ArtifactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Hiện vật và nội dung đa ngôn ngữ. Không @CrossOrigin, không kiểm tra quyền:
 * Gateway lo CORS và luật GET công khai, thêm/sửa/xóa chỉ ADMIN.
 * Lỗi: ApiException/IllegalArgumentException -> GlobalExceptionHandler trả {success:false, message}.
 */
@Tag(name = "Artifacts", description = "Hiện vật, nội dung đa ngôn ngữ ?lang=vi|en")
@RestController
@RequestMapping("/api/artifacts")
@RequiredArgsConstructor
public class ArtifactController {

    private final ArtifactService artifactService;

    @Operation(summary = "Danh sách hiện vật", description = "Lọc theo siteId (hoặc locationId) hoặc tìm theo tên (search)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ArtifactSummaryResponse>>> getArtifacts(
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) Long siteId,
            @RequestParam(required = false) String search) {
        Long site = siteId != null ? siteId : locationId;
        List<ArtifactSummaryResponse> result;
        if (site != null) {
            result = artifactService.getArtifactsBySite(site);
        } else if (search != null && !search.isBlank()) {
            result = artifactService.searchArtifacts(search);
        } else {
            result = artifactService.getAllArtifacts();
        }
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @Operation(summary = "Lấy nhiều hiện vật theo id",
            description = "Vd ?ids=3,1,2. Trả đúng thứ tự yêu cầu, bỏ qua id không tồn tại, tối đa 100 id")
    @GetMapping("/batch")
    public ResponseEntity<ApiResponse<List<ArtifactSummaryResponse>>> getBatch(@RequestParam List<Long> ids) {
        return ResponseEntity.ok(ApiResponse.ok(artifactService.getBatch(ids)));
    }

    @Operation(summary = "Chi tiết hiện vật", description = "?lang=vi|en; thiếu bản dịch thì trả bản tiếng Việt, fallback = true")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ArtifactResponse>> getArtifactById(
            @PathVariable Long id, @RequestParam(defaultValue = "vi") String lang) {
        return ResponseEntity.ok(ApiResponse.ok(artifactService.localize(artifactService.getArtifactById(id), lang)));
    }

    @Operation(summary = "[Cũ] Tìm hiện vật theo beacon UUID",
            description = "Luồng quét beacon chuẩn: /api/ibeacons/registry rồi /api/artifacts/{id}")
    @GetMapping("/beacon/{uuid}")
    public ResponseEntity<ApiResponse<ArtifactResponse>> getByBeaconUuid(
            @PathVariable String uuid, @RequestParam(defaultValue = "vi") String lang) {
        return ResponseEntity.ok(ApiResponse.ok(
                artifactService.localize(artifactService.getArtifactByBeaconUuid(uuid), lang)));
    }

    @Operation(summary = "Thêm hiện vật", description = "Bắt buộc có bản tiếng Việt; siteId được kiểm tra qua site-service")
    @PostMapping
    public ResponseEntity<ApiResponse<ArtifactResponse>> createArtifact(@RequestBody ArtifactRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo hiện vật thành công", artifactService.createArtifact(request)));
    }

    @Operation(summary = "Sửa hiện vật", description = "Chỉ gửi trường cần đổi; gửi contents/media thì thay cả danh sách")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ArtifactResponse>> updateArtifact(
            @PathVariable Long id, @RequestBody ArtifactRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật hiện vật thành công", artifactService.updateArtifact(id, request)));
    }

    @Operation(summary = "Xóa hiện vật", description = "Xóa kèm bản dịch và media")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteArtifact(@PathVariable Long id) {
        artifactService.deleteArtifact(id);
        return ResponseEntity.ok(ApiResponse.ok("Xóa hiện vật thành công", null));
    }

    @Operation(summary = "Các bản dịch của hiện vật")
    @GetMapping("/{id}/contents")
    public ResponseEntity<ApiResponse<List<ArtifactContentResponse>>> getContents(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(artifactService.getContents(id)));
    }

    @Operation(summary = "Bản dịch theo ngôn ngữ", description = "Không có thì trả bản tiếng Việt")
    @GetMapping("/{id}/contents/{lang}")
    public ResponseEntity<ApiResponse<ArtifactContentResponse>> getContentByLanguage(
            @PathVariable Long id, @PathVariable String lang) {
        return ResponseEntity.ok(ApiResponse.ok(artifactService.getContentByLanguage(id, lang)));
    }

    @Operation(summary = "Thêm/sửa bản dịch", description = "Ghi đè theo languageCode")
    @PostMapping("/{id}/contents")
    public ResponseEntity<ApiResponse<ArtifactContentResponse>> upsertContent(
            @PathVariable Long id, @RequestBody ArtifactContentRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Lưu nội dung ngôn ngữ thành công", artifactService.upsertContent(id, request)));
    }

    @Operation(summary = "Xóa bản dịch", description = "Không xóa được bản tiếng Việt")
    @DeleteMapping("/contents/{contentId}")
    public ResponseEntity<ApiResponse<Void>> deleteContent(@PathVariable Long contentId) {
        artifactService.deleteContent(contentId);
        return ResponseEntity.ok(ApiResponse.ok("Xóa nội dung ngôn ngữ thành công", null));
    }
}
