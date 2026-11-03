package com.smartheritage.personalization.controller;

import com.smartheritage.common.api.ApiResponse;
import com.smartheritage.common.web.ApiException;
import com.smartheritage.personalization.service.RedisDemoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Tag(name = "Personalization - Redis demo")
@RestController
@RequestMapping("/api/cache/demo")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.redis-demo.enabled", havingValue = "true")
public class RedisCacheDemoController {
    private final RedisDemoService redisDemoService;

    @Value("${app.redis-demo.default-ttl-seconds:600}")
    private long defaultTtlSeconds;

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> write(
            @RequestParam String key,
            @RequestParam String value,
            @RequestParam(required = false) Long ttlSeconds) {
        if (value == null || value.isBlank() || value.length() > 2000) {
            throw ApiException.badRequest("value phải có 1–2000 ký tự.");
        }
        long ttl = ttlSeconds == null ? defaultTtlSeconds : ttlSeconds;
        if (ttl < 1 || ttl > 86400) throw ApiException.badRequest("TTL phải từ 1 đến 86400 giây.");
        try {
            redisDemoService.write(key, value, Duration.ofSeconds(ttl));
        } catch (IllegalArgumentException ex) {
            throw ApiException.badRequest(ex.getMessage());
        } catch (DataAccessException ex) {
            throw ApiException.serviceUnavailable("Không kết nối được Redis.");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("key", key);
        body.put("value", value);
        body.put("ttlSeconds", ttl);
        body.put("stored", true);
        return ResponseEntity.ok(ApiResponse.ok("Đã ghi dữ liệu vào Redis", body));
    }

    @GetMapping("/{key}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> read(@PathVariable String key) {
        try {
            var cached = redisDemoService.read(key);
            if (cached.isEmpty()) throw ApiException.notFound("Không tìm thấy key trong Redis hoặc TTL đã hết.");
            return ResponseEntity.ok(ApiResponse.ok("Đọc dữ liệu từ Redis",
                    Map.<String, Object>of("key", key, "value", cached.get(), "hit", true)));
        } catch (IllegalArgumentException ex) {
            throw ApiException.badRequest(ex.getMessage());
        } catch (DataAccessException ex) {
            throw ApiException.serviceUnavailable("Không kết nối được Redis.");
        }
    }

    @DeleteMapping("/{key}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> delete(@PathVariable String key) {
        try {
            boolean deleted = redisDemoService.delete(key);
            return ResponseEntity.ok(ApiResponse.ok("Đã xử lý xóa key Redis",
                    Map.of("key", key, "deleted", deleted)));
        } catch (IllegalArgumentException ex) {
            throw ApiException.badRequest(ex.getMessage());
        } catch (DataAccessException ex) {
            throw ApiException.serviceUnavailable("Không kết nối được Redis.");
        }
    }
}
