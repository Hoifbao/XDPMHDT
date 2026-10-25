package com.smartheritage.backend.controller;

import com.smartheritage.backend.dto.ApiResponse;
import com.smartheritage.backend.dto.MediaResourceRequest;
import com.smartheritage.backend.dto.MediaResourceResponse;
import com.smartheritage.backend.service.MediaResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller – Tài nguyên đa phương tiện của hiện vật.
 *
 *  GET    /api/artifacts/{artifactId}/media   – Danh sách tài nguyên của hiện vật
 *  POST   /api/artifacts/{artifactId}/media   – Thêm tài nguyên (URL dạng text)
 *  DELETE /api/media/{mediaId}                – Xoá một tài nguyên
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MediaResourceController {

    private final MediaResourceService mediaResourceService;

    @GetMapping("/artifacts/{artifactId}/media")
    public ResponseEntity<ApiResponse<List<MediaResourceResponse>>> getMedia(
            @PathVariable Long artifactId) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Thành công",
                    mediaResourceService.getByArtifact(artifactId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/artifacts/{artifactId}/media")
    public ResponseEntity<ApiResponse<MediaResourceResponse>> addMedia(
            @PathVariable Long artifactId,
            @RequestBody MediaResourceRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(
                    "Thêm tài nguyên thành công", mediaResourceService.create(artifactId, request)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/media/{mediaId}")
    public ResponseEntity<ApiResponse<Void>> deleteMedia(@PathVariable Long mediaId) {
        try {
            mediaResourceService.delete(mediaId);
            return ResponseEntity.ok(ApiResponse.ok("Xoá tài nguyên thành công", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }
}
