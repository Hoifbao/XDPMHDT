package com.smartheritage.content.controller;

import com.smartheritage.common.api.ApiResponse;
import com.smartheritage.content.entity.Language;
import com.smartheritage.content.service.LanguageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LanguageController {
    private final LanguageService service;
    public record Request(String code, String name, Boolean active) {}

    @GetMapping("/languages")
    public ApiResponse<List<Language>> active() { return ApiResponse.ok(service.list(false)); }

    @GetMapping("/admin/languages")
    public ApiResponse<List<Language>> all() { return ApiResponse.ok(service.list(true)); }

    @PostMapping("/admin/languages")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Language> create(@RequestBody Request body) {
        if (body.code() == null) throw new IllegalArgumentException("code là bắt buộc");
        return ApiResponse.ok(service.save(body.code(), body.name(), body.active(), true));
    }

    @PutMapping("/admin/languages/{code}")
    public ApiResponse<Language> update(@PathVariable String code, @RequestBody Request body) {
        return ApiResponse.ok(service.save(code, body.name(), body.active(), false));
    }
}
