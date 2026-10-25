package com.smartheritage.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartheritage.backend.dto.ApiResponse;
import com.smartheritage.backend.dto.ArtifactResponse;
import com.smartheritage.backend.service.ArtifactService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Demo công nghệ Redis (Phú – Chapter 3).
 * Redis chạy trong Docker: Infra/docker-compose.yml (service redis-cache, cổng 6379).
 *
 *  POST /api/demo/redis?key=..&value=..&ttlSeconds=60 – Lưu một chuỗi vào Redis
 *  GET  /api/demo/redis/{key}                          – Đọc chuỗi và TTL còn lại
 *  GET  /api/demo/redis/artifacts/{id}                 – Cache-aside: đọc hiện vật từ Redis,
 *                                                        chưa có thì đọc MySQL rồi lưu cache 60 giây
 */
@RestController
@RequestMapping("/api/demo/redis")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RedisDemoController {

    private static final Duration ARTIFACT_TTL = Duration.ofSeconds(60);

    private final StringRedisTemplate redis;
    private final ArtifactService artifactService;
    private final ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> put(
            @RequestParam String key,
            @RequestParam String value,
            @RequestParam(defaultValue = "60") long ttlSeconds) {
        try {
            redis.opsForValue().set(key, value, Duration.ofSeconds(ttlSeconds));
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("key", key);
            result.put("value", value);
            result.put("ttlSeconds", ttlSeconds);
            return ResponseEntity.ok(ApiResponse.ok("Đã lưu vào Redis", result));
        } catch (DataAccessException e) {
            return redisDown();
        }
    }

    @GetMapping("/{key}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> get(@PathVariable String key) {
        try {
            String value = redis.opsForValue().get(key);
            if (value == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Key không tồn tại hoặc đã hết hạn: " + key));
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("key", key);
            result.put("value", value);
            result.put("ttlSecondsRemaining", redis.getExpire(key));
            return ResponseEntity.ok(ApiResponse.ok("Đọc từ Redis", result));
        } catch (DataAccessException e) {
            return redisDown();
        }
    }

    @GetMapping("/artifacts/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getArtifactCached(@PathVariable Long id) {
        String cacheKey = "artifact:" + id;
        long start = System.nanoTime();
        try {
            String cached = redis.opsForValue().get(cacheKey);
            String source;
            JsonNode data;
            if (cached != null) {
                source = "REDIS";
                data = objectMapper.readTree(cached);
            } else {
                source = "MYSQL";
                ArtifactResponse artifact = artifactService.getArtifactById(id);
                String json = objectMapper.writeValueAsString(artifact);
                redis.opsForValue().set(cacheKey, json, ARTIFACT_TTL);
                data = objectMapper.readTree(json);
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("source", source);
            result.put("elapsedMs", (System.nanoTime() - start) / 1_000_000.0);
            result.put("artifact", data);
            return ResponseEntity.ok(ApiResponse.ok("Đọc hiện vật (" + source + ")", result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        } catch (DataAccessException e) {
            return redisDown();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi xử lý JSON: " + e.getMessage()));
        }
    }

    private <T> ResponseEntity<ApiResponse<T>> redisDown() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error("Không kết nối được Redis. Chạy: docker compose -f Infra/docker-compose.yml up -d"));
    }
}
