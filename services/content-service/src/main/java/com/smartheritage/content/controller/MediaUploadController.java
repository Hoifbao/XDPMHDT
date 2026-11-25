package com.smartheritage.content.controller;

import com.smartheritage.common.api.ApiResponse;
import com.smartheritage.content.service.MediaStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.io.IOException;

@RestController
@RequestMapping("/api/media")
public class MediaUploadController {
    private final MediaStorageService storage;
    // Bao W8 (loi 13): request di qua Gateway co Host = content-service:8083 -> link tra ve la dia chi Docker noi bo.
    // Co PUBLIC_BASE_URL (vd http://localhost:8080) thi tao link tu bien nay; de trong thi giu cach cu (doc X-Forwarded-*).
    private final String publicBaseUrl;

    public MediaUploadController(MediaStorageService storage,
                                 @Value("${app.media.public-base-url:}") String publicBaseUrl) {
        this.storage = storage;
        this.publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl.trim().replaceAll("/+$", "");
    }
    public record Uploaded(String fileUrl, String mediaType, long size) {}

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Uploaded> upload(@RequestParam("file") MultipartFile file) throws IOException {
        var stored = storage.store(file);
        return ApiResponse.ok(new Uploaded(fileUrl(stored.name()), stored.mediaType(), stored.size()));
    }

    public String fileUrl(String name) {
        if (!publicBaseUrl.isEmpty()) return publicBaseUrl + "/api/media/files/" + name;
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/media/files/").path(name).toUriString();
    }

    @GetMapping("/files/{name}")
    public ResponseEntity<Resource> file(@PathVariable String name) throws IOException {
        Resource resource = storage.load(name);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(storage.contentType(name)))
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(30)))
                .body(resource);
    }
}
