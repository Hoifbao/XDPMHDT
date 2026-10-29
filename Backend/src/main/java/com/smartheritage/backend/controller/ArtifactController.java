package com.smartheritage.backend.controller;

import com.smartheritage.backend.dto.*;
import com.smartheritage.backend.service.ArtifactService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * REST Controller – Artifact CRUD
 *
 * Base URL: /api/artifacts
 *
 * Endpoints:
 *  GET    /api/artifacts                          – Danh sách tất cả hiện vật
 *  GET    /api/artifacts?locationId=1             – Lọc theo địa điểm
 *  GET    /api/artifacts?search=trống đồng        – Tìm theo tên
 *  GET    /api/artifacts/{id}                     – Chi tiết hiện vật
 *  GET    /api/artifacts/beacon/{uuid}            – Lấy theo Beacon UUID (mobile BLE)
 *  POST   /api/artifacts                          – Tạo hiện vật mới
 *  PUT    /api/artifacts/{id}                     – Cập nhật hiện vật
 *  DELETE /api/artifacts/{id}                     – Xóa hiện vật
 *
 *  GET    /api/artifacts/{id}/contents            – Danh sách nội dung đa ngôn ngữ
 *  GET    /api/artifacts/{id}/contents/{lang}     – Nội dung theo ngôn ngữ
 *  POST   /api/artifacts/{id}/contents            – Thêm / cập nhật nội dung ngôn ngữ
 *  DELETE /api/artifacts/contents/{contentId}     – Xóa một bản nội dung ngôn ngữ
 */
@RestController
@RequestMapping("/api/artifacts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ArtifactController {

    private final ArtifactService artifactService;

    // ══════════════════════════════════════════
    //  ARTIFACT CRUD
    // ══════════════════════════════════════════

    /**
     * GET /api/artifacts
     * Query params (tuỳ chọn):
     *   ?locationId=1   → lọc theo địa điểm
     *   ?search=keyword → tìm theo tên
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ArtifactSummaryResponse>>> getArtifacts(
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) Long siteId,
            @RequestParam(required = false) String search) {

        List<ArtifactSummaryResponse> result;

        if (siteId != null) locationId = siteId;
        if (locationId != null) {
            result = artifactService.getArtifactsByLocation(locationId);
        } else if (search != null && !search.isBlank()) {
            result = artifactService.searchArtifacts(search);
        } else {
            result = artifactService.getAllArtifacts();
        }

        return ResponseEntity.ok(ApiResponse.ok("Thành công", result));
    }

    /**
     * GET /api/artifacts/{id}
     * Trả về chi tiết đầy đủ kèm contents đa ngôn ngữ.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ArtifactResponse>> getArtifactById(@PathVariable Long id, @RequestParam(defaultValue = "vi") String lang) {
        try {
            ArtifactResponse response = artifactService.localize(artifactService.getArtifactById(id), lang);
            return ResponseEntity.ok(ApiResponse.ok("Thành công", response));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * GET /api/artifacts/beacon/{uuid}
     * Mobile app gọi khi phát hiện iBeacon BLE.
     */
    @GetMapping("/beacon/{uuid}")
    public ResponseEntity<ApiResponse<ArtifactResponse>> getByBeaconUuid(
            @PathVariable String uuid, @RequestParam(defaultValue = "vi") String lang) {
        try {
            ArtifactResponse response = artifactService.localize(artifactService.getArtifactByBeaconUuid(uuid), lang);
            return ResponseEntity.ok(ApiResponse.ok("Thành công", response));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * POST /api/artifacts
     * Body: ArtifactRequest (có thể kèm contents[])
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ArtifactResponse>> createArtifact(
            @RequestBody ArtifactRequest request) {
        try {
            ArtifactResponse response = artifactService.createArtifact(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok("Tạo hiện vật thành công", response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * PUT /api/artifacts/{id}
     * Body: ArtifactRequest (chỉ gửi field cần thay đổi)
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ArtifactResponse>> updateArtifact(
            @PathVariable Long id,
            @RequestBody ArtifactRequest request) {
        try {
            ArtifactResponse response = artifactService.updateArtifact(id, request);
            return ResponseEntity.ok(ApiResponse.ok("Cập nhật hiện vật thành công", response));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * DELETE /api/artifacts/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteArtifact(@PathVariable Long id) {
        try {
            artifactService.deleteArtifact(id);
            return ResponseEntity.ok(ApiResponse.ok("Xóa hiện vật thành công", null));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    // ══════════════════════════════════════════
    //  ARTIFACT CONTENT (đa ngôn ngữ)
    // ══════════════════════════════════════════

    /**
     * GET /api/artifacts/{id}/contents
     * Lấy danh sách tất cả bản nội dung ngôn ngữ của hiện vật.
     */
    @GetMapping("/{id}/contents")
    public ResponseEntity<ApiResponse<List<ArtifactContentResponse>>> getContents(
            @PathVariable Long id) {
        try {
            List<ArtifactContentResponse> contents = artifactService.getContents(id);
            return ResponseEntity.ok(ApiResponse.ok("Thành công", contents));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * GET /api/artifacts/{id}/contents/{lang}
     * Lấy nội dung theo ngôn ngữ, fallback về "vi" nếu không có.
     * Ví dụ: /api/artifacts/1/contents/en
     */
    @GetMapping("/{id}/contents/{lang}")
    public ResponseEntity<ApiResponse<ArtifactContentResponse>> getContentByLanguage(
            @PathVariable Long id,
            @PathVariable String lang) {
        try {
            ArtifactContentResponse content = artifactService.getContentByLanguage(id, lang);
            return ResponseEntity.ok(ApiResponse.ok("Thành công", content));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * POST /api/artifacts/{id}/contents
     * Upsert nội dung ngôn ngữ (tạo mới hoặc cập nhật theo languageCode).
     */
    @PostMapping("/{id}/contents")
    public ResponseEntity<ApiResponse<ArtifactContentResponse>> upsertContent(
            @PathVariable Long id,
            @RequestBody ArtifactContentRequest request) {
        try {
            ArtifactContentResponse response = artifactService.upsertContent(id, request);
            return ResponseEntity.ok(ApiResponse.ok("Lưu nội dung ngôn ngữ thành công", response));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * DELETE /api/artifacts/contents/{contentId}
     * Xóa một bản nội dung ngôn ngữ theo ID của ArtifactContent.
     */
    @DeleteMapping("/contents/{contentId}")
    public ResponseEntity<ApiResponse<Void>> deleteContent(@PathVariable Long contentId) {
        try {
            artifactService.deleteContent(contentId);
            return ResponseEntity.ok(ApiResponse.ok("Xóa nội dung ngôn ngữ thành công", null));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}
