package com.smartheritage.content.controller;

import com.smartheritage.common.api.ApiResponse;
import com.smartheritage.content.dto.MediaResourceRequest;
import com.smartheritage.content.dto.MediaResourceResponse;
import com.smartheritage.content.service.MediaResourceService;
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

/** Ảnh/audio/video của hiện vật (Gateway chuyển /api/media/** và /api/artifacts/** về đây). */
@Tag(name = "Media", description = "Ảnh, audio, video của hiện vật")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MediaResourceController {

    private final MediaResourceService mediaResourceService;

    @Operation(summary = "Media của hiện vật")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Danh sách media"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Không tìm thấy hiện vật",
                    content = @Content(mediaType = ArtifactController.JSON, examples = @ExampleObject(value = ArtifactController.EX_404)))
    })
    @GetMapping("/artifacts/{artifactId}/media")
    public ResponseEntity<ApiResponse<List<MediaResourceResponse>>> getMedia(@Parameter(description = "ID hiện vật", example = "1") @PathVariable Long artifactId) {
        return ResponseEntity.ok(ApiResponse.ok(mediaResourceService.getByArtifact(artifactId)));
    }

    @Operation(summary = "Thêm media", description = "Truyền URL dạng text (chưa upload file)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201",
                    description = "Đã thêm media"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "mediaType không phải IMAGE/AUDIO/VIDEO hoặc thiếu fileUrl",
                    content = @Content(mediaType = ArtifactController.JSON, examples = @ExampleObject(value = ArtifactController.EX_400_MEDIA))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "Chưa đăng nhập (Gateway chặn)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "Không phải ADMIN (Gateway chặn)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Không tìm thấy hiện vật",
                    content = @Content(mediaType = ArtifactController.JSON, examples = @ExampleObject(value = ArtifactController.EX_404)))
    })
    @PostMapping("/artifacts/{artifactId}/media")
    public ResponseEntity<ApiResponse<MediaResourceResponse>> addMedia(
            @Parameter(description = "ID hiện vật", example = "1") @PathVariable Long artifactId, @RequestBody MediaResourceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Thêm tài nguyên thành công", mediaResourceService.create(artifactId, request)));
    }

    @Operation(summary = "Xóa media")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Đã xóa media"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "Chưa đăng nhập (Gateway chặn)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "Không phải ADMIN (Gateway chặn)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "Không tìm thấy media",
                    content = @Content(mediaType = ArtifactController.JSON, examples = @ExampleObject(value = ArtifactController.EX_404)))
    })
    @DeleteMapping("/media/{mediaId}")
    public ResponseEntity<ApiResponse<Void>> deleteMedia(
            @Parameter(description = "ID media", example = "10") @PathVariable Long mediaId) {
        mediaResourceService.delete(mediaId);
        return ResponseEntity.ok(ApiResponse.ok("Xoá tài nguyên thành công", null));
    }
}
