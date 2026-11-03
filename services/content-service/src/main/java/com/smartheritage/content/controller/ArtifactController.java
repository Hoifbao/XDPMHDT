package com.smartheritage.content.controller;

import com.smartheritage.common.api.ApiResponse;
import com.smartheritage.content.dto.*;
import com.smartheritage.content.service.ArtifactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    // ---------- Ví dụ JSON cho Swagger (Bảo - Sprint 5, DoD: ví dụ response + mã lỗi) ----------
    static final String JSON = "application/json";
    static final String EX_400 = "{\"success\": false, \"message\": \"Bắt buộc có bản tiếng Việt\", \"data\": null}";
    static final String EX_400_MEDIA = "{\"success\": false, \"message\": \"mediaType phải là IMAGE, AUDIO hoặc VIDEO\", \"data\": null}";
    static final String EX_404 = "{\"success\": false, \"message\": \"Không tìm thấy hiện vật với ID: 99\", \"data\": null}";
    static final String EX_503 = "{\"success\": false, \"message\": \"Không kết nối được dịch vụ di tích (site-service), vui lòng thử lại sau!\", \"data\": null}";
    static final String EX_DETAIL = "{\"success\": true, \"message\": \"Thành công\", \"data\": {\"id\": 1, \"siteId\": 1, "
            + "\"locationId\": 1, \"locationName\": \"Văn Miếu - Quốc Tử Giám\", \"name\": \"Khuê Văn Các\", "
            + "\"artifactCode\": \"KVC-01\", \"period\": \"Năm 1805 (triều Nguyễn)\", \"material\": \"Gỗ, gạch\", "
            + "\"status\": \"ACTIVE\", \"requestedLanguage\": \"en\", \"languageCode\": \"en\", \"fallback\": false, "
            + "\"content\": {\"id\": 2, \"artifactId\": 1, \"languageCode\": \"en\", \"title\": \"Khue Van Cac\", "
            + "\"description\": \"Khue Van Cac, the Constellation of Literature Pavilion, is a symbol of the Temple of Literature.\", "
            + "\"audioUrl\": \"https://example.com/audio/kvc-en.mp3\"}}}";

    private final ArtifactService artifactService;

    @Operation(summary = "Danh sách hiện vật", description = "Lọc theo siteId (hoặc locationId) hoặc tìm theo tên (search)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Danh sách (có thể rỗng)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Tham số sai kiểu (vd siteId=abc)",
                    content = @Content(mediaType = JSON, examples = @ExampleObject(value = EX_400)))
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<ArtifactSummaryResponse>>> getArtifacts(
            @Parameter(description = "Tên cũ của siteId (giữ cho client cũ)", example = "1") @RequestParam(required = false) Long locationId,
            @Parameter(description = "ID di tích", example = "1") @RequestParam(required = false) Long siteId,
            @Parameter(description = "Từ khóa tìm theo tên", example = "Khuê") @RequestParam(required = false) String search) {
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
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Danh sách theo đúng thứ tự ids"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Thiếu ids hoặc quá 100 id",
                    content = @Content(mediaType = JSON, examples = @ExampleObject(value = EX_400)))
    })
    @GetMapping("/batch")
    public ResponseEntity<ApiResponse<List<ArtifactSummaryResponse>>> getBatch(
            @Parameter(description = "Danh sách id, cách nhau bởi dấu phẩy (tối đa 100)", example = "3,1,2") @RequestParam List<Long> ids) {
        return ResponseEntity.ok(ApiResponse.ok(artifactService.getBatch(ids)));
    }

    @Operation(summary = "Chi tiết hiện vật", description = "?lang=vi|en; thiếu bản dịch thì trả bản tiếng Việt, fallback = true")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Chi tiết hiện vật kèm nội dung theo ngôn ngữ",
                    content = @Content(mediaType = JSON, examples = @ExampleObject(value = EX_DETAIL))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "lang không phải vi/en",
                    content = @Content(mediaType = JSON, examples = @ExampleObject(value = EX_400))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Không tìm thấy hiện vật",
                    content = @Content(mediaType = JSON, examples = @ExampleObject(value = EX_404)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ArtifactResponse>> getArtifactById(
            @Parameter(description = "ID hiện vật", example = "1") @PathVariable Long id,
            @Parameter(description = "Ngôn ngữ: vi hoặc en", example = "en") @RequestParam(defaultValue = "vi") String lang) {
        return ResponseEntity.ok(ApiResponse.ok(artifactService.localize(artifactService.getArtifactById(id), lang)));
    }

    @Operation(summary = "[Cũ] Tìm hiện vật theo beacon UUID",
            description = "Luồng quét beacon chuẩn: /api/ibeacons/registry rồi /api/artifacts/{id}")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Chi tiết hiện vật"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "lang không phải vi/en",
                    content = @Content(mediaType = JSON, examples = @ExampleObject(value = EX_400))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Không có hiện vật gắn beacon UUID này",
                    content = @Content(mediaType = JSON, examples = @ExampleObject(value = EX_404)))
    })
    @GetMapping("/beacon/{uuid}")
    public ResponseEntity<ApiResponse<ArtifactResponse>> getByBeaconUuid(
            @Parameter(description = "Beacon UUID", example = "FDA50693-A4E2-4FB1-AFCF-C6EB07647825") @PathVariable String uuid,
            @Parameter(description = "Ngôn ngữ: vi hoặc en", example = "vi") @RequestParam(defaultValue = "vi") String lang) {
        return ResponseEntity.ok(ApiResponse.ok(
                artifactService.localize(artifactService.getArtifactByBeaconUuid(uuid), lang)));
    }

    @Operation(summary = "Thêm hiện vật", description = "Bắt buộc có bản tiếng Việt; siteId được kiểm tra qua site-service")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201",
                    description = "Tạo thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Dữ liệu không hợp lệ: thiếu name/siteId/bản tiếng Việt, siteId không tồn tại, URL sai, trùng mã",
                    content = @Content(mediaType = JSON, examples = @ExampleObject(value = EX_400))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "Chưa đăng nhập (Gateway chặn)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "Không phải ADMIN (Gateway chặn)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "Trùng mã hiện vật, beacon UUID hoặc ngôn ngữ (ràng buộc DB)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503",
                    description = "site-service không phản hồi khi kiểm tra siteId",
                    content = @Content(mediaType = JSON, examples = @ExampleObject(value = EX_503)))
    })
    @PostMapping
    public ResponseEntity<ApiResponse<ArtifactResponse>> createArtifact(@RequestBody ArtifactRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo hiện vật thành công", artifactService.createArtifact(request)));
    }

    @Operation(summary = "Sửa hiện vật", description = "Chỉ gửi trường cần đổi; gửi contents/media thì thay cả danh sách")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Cập nhật thành công"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Dữ liệu không hợp lệ",
                    content = @Content(mediaType = JSON, examples = @ExampleObject(value = EX_400))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "Chưa đăng nhập (Gateway chặn)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "Không phải ADMIN (Gateway chặn)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Không tìm thấy hiện vật",
                    content = @Content(mediaType = JSON, examples = @ExampleObject(value = EX_404))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "Trùng mã hiện vật, beacon UUID hoặc ngôn ngữ (ràng buộc DB)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503",
                    description = "site-service không phản hồi khi kiểm tra siteId",
                    content = @Content(mediaType = JSON, examples = @ExampleObject(value = EX_503)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ArtifactResponse>> updateArtifact(
            @Parameter(description = "ID hiện vật", example = "1") @PathVariable Long id, @RequestBody ArtifactRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật hiện vật thành công", artifactService.updateArtifact(id, request)));
    }

    @Operation(summary = "Xóa hiện vật", description = "Xóa kèm bản dịch và media")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Đã xóa"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "Chưa đăng nhập (Gateway chặn)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "Không phải ADMIN (Gateway chặn)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Không tìm thấy hiện vật",
                    content = @Content(mediaType = JSON, examples = @ExampleObject(value = EX_404)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteArtifact(@Parameter(description = "ID hiện vật", example = "1") @PathVariable Long id) {
        artifactService.deleteArtifact(id);
        return ResponseEntity.ok(ApiResponse.ok("Xóa hiện vật thành công", null));
    }

    @Operation(summary = "Các bản dịch của hiện vật")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Các bản dịch"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Không tìm thấy hiện vật",
                    content = @Content(mediaType = JSON, examples = @ExampleObject(value = EX_404)))
    })
    @GetMapping("/{id}/contents")
    public ResponseEntity<ApiResponse<List<ArtifactContentResponse>>> getContents(@Parameter(description = "ID hiện vật", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(artifactService.getContents(id)));
    }

    @Operation(summary = "Bản dịch theo ngôn ngữ", description = "Không có thì trả bản tiếng Việt")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Bản dịch"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "lang không phải vi/en",
                    content = @Content(mediaType = JSON, examples = @ExampleObject(value = EX_400))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Không có hiện vật hoặc chưa có bản dịch",
                    content = @Content(mediaType = JSON, examples = @ExampleObject(value = EX_404)))
    })
    @GetMapping("/{id}/contents/{lang}")
    public ResponseEntity<ApiResponse<ArtifactContentResponse>> getContentByLanguage(
            @Parameter(description = "ID hiện vật", example = "1") @PathVariable Long id,
            @Parameter(description = "Ngôn ngữ: vi hoặc en", example = "en") @PathVariable String lang) {
        return ResponseEntity.ok(ApiResponse.ok(artifactService.getContentByLanguage(id, lang)));
    }

    @Operation(summary = "Thêm/sửa bản dịch", description = "Ghi đè theo languageCode")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Đã lưu bản dịch"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Thiếu languageCode/title/description hoặc URL sai",
                    content = @Content(mediaType = JSON, examples = @ExampleObject(value = EX_400))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "Chưa đăng nhập (Gateway chặn)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "Không phải ADMIN (Gateway chặn)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Không tìm thấy hiện vật",
                    content = @Content(mediaType = JSON, examples = @ExampleObject(value = EX_404))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "Trùng mã hiện vật, beacon UUID hoặc ngôn ngữ (ràng buộc DB)")
    })
    @PostMapping("/{id}/contents")
    public ResponseEntity<ApiResponse<ArtifactContentResponse>> upsertContent(
            @Parameter(description = "ID hiện vật", example = "1") @PathVariable Long id, @RequestBody ArtifactContentRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Lưu nội dung ngôn ngữ thành công", artifactService.upsertContent(id, request)));
    }

    @Operation(summary = "Xóa bản dịch", description = "Không xóa được bản tiếng Việt")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Đã xóa bản dịch"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Không được xóa bản tiếng Việt",
                    content = @Content(mediaType = JSON, examples = @ExampleObject(value = EX_400))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "Chưa đăng nhập (Gateway chặn)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "Không phải ADMIN (Gateway chặn)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Không tìm thấy bản dịch",
                    content = @Content(mediaType = JSON, examples = @ExampleObject(value = EX_404)))
    })
    @DeleteMapping("/contents/{contentId}")
    public ResponseEntity<ApiResponse<Void>> deleteContent(
            @Parameter(description = "ID bản dịch", example = "5") @PathVariable Long contentId) {
        artifactService.deleteContent(contentId);
        return ResponseEntity.ok(ApiResponse.ok("Xóa nội dung ngôn ngữ thành công", null));
    }
}
