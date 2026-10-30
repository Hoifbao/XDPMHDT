package com.smartheritage.content.controller;

import com.smartheritage.common.api.ApiResponse;
import com.smartheritage.content.dto.MediaResourceRequest;
import com.smartheritage.content.dto.MediaResourceResponse;
import com.smartheritage.content.service.MediaResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Ảnh/audio/video của hiện vật (Gateway chuyển /api/media/** và /api/artifacts/** về đây). */
@Tag(name = "Media", description = "Ảnh, audio, video của hiện vật")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MediaResourceController {

    private final MediaResourceService mediaResourceService;

    @Operation(summary = "Media của hiện vật")
    @GetMapping("/artifacts/{artifactId}/media")
    public ResponseEntity<ApiResponse<List<MediaResourceResponse>>> getMedia(@PathVariable Long artifactId) {
        return ResponseEntity.ok(ApiResponse.ok(mediaResourceService.getByArtifact(artifactId)));
    }

    @Operation(summary = "Thêm media", description = "Truyền URL dạng text (chưa upload file)")
    @PostMapping("/artifacts/{artifactId}/media")
    public ResponseEntity<ApiResponse<MediaResourceResponse>> addMedia(
            @PathVariable Long artifactId, @RequestBody MediaResourceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Thêm tài nguyên thành công", mediaResourceService.create(artifactId, request)));
    }

    @Operation(summary = "Xóa media")
    @DeleteMapping("/media/{mediaId}")
    public ResponseEntity<ApiResponse<Void>> deleteMedia(@PathVariable Long mediaId) {
        mediaResourceService.delete(mediaId);
        return ResponseEntity.ok(ApiResponse.ok("Xoá tài nguyên thành công", null));
    }
}
