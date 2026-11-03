package com.smartheritage.content.controller;

import com.smartheritage.common.api.ApiResponse;
import com.smartheritage.content.service.MediaStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import java.io.IOException;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaUploadController {
    private final MediaStorageService storage;
    public record Uploaded(String fileUrl, String mediaType, long size) {}

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Uploaded> upload(@RequestParam("file") MultipartFile file) throws IOException {
        var stored = storage.store(file);
        String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/media/files/").path(stored.name()).toUriString();
        return ApiResponse.ok(new Uploaded(url, stored.mediaType(), stored.size()));
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
