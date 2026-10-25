package com.smartheritage.backend.controller;

import com.smartheritage.backend.dto.ApiResponse;
import com.smartheritage.backend.dto.FavoriteResponse;
import com.smartheritage.backend.dto.VisitHistoryResponse;
import com.smartheritage.backend.dto.VisitRequest;
import com.smartheritage.backend.service.PersonalizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller – Cá nhân hoá (Phú)
 *
 *  GET    /api/users/{userId}/favorites                     – Danh sách yêu thích
 *  PUT    /api/users/{userId}/favorites/{artifactId}        – Thêm yêu thích (gọi lại không trùng)
 *  DELETE /api/users/{userId}/favorites/{artifactId}        – Bỏ yêu thích
 *  GET    /api/users/{userId}/favorites/{artifactId}/exists – Đã thích chưa (true/false)
 *  GET    /api/users/{userId}/visits                        – Lịch sử tham quan
 *  POST   /api/users/{userId}/visits                        – Ghi một lượt tham quan
 */
@RestController
@RequestMapping("/api/users/{userId}")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PersonalizationController {

    private final PersonalizationService personalizationService;

    @GetMapping("/favorites")
    public ResponseEntity<ApiResponse<List<FavoriteResponse>>> getFavorites(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Thành công", personalizationService.getFavorites(userId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/favorites/{artifactId}")
    public ResponseEntity<ApiResponse<FavoriteResponse>> addFavorite(
            @PathVariable Long userId, @PathVariable Long artifactId) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Đã thêm vào yêu thích",
                    personalizationService.addFavorite(userId, artifactId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/favorites/{artifactId}")
    public ResponseEntity<ApiResponse<Void>> removeFavorite(
            @PathVariable Long userId, @PathVariable Long artifactId) {
        personalizationService.removeFavorite(userId, artifactId);
        return ResponseEntity.ok(ApiResponse.ok("Đã bỏ yêu thích", null));
    }

    @GetMapping("/favorites/{artifactId}/exists")
    public ResponseEntity<ApiResponse<Boolean>> isFavorite(
            @PathVariable Long userId, @PathVariable Long artifactId) {
        return ResponseEntity.ok(ApiResponse.ok("Thành công",
                personalizationService.isFavorite(userId, artifactId)));
    }

    @GetMapping("/visits")
    public ResponseEntity<ApiResponse<List<VisitHistoryResponse>>> getVisits(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Thành công", personalizationService.getVisits(userId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/visits")
    public ResponseEntity<ApiResponse<VisitHistoryResponse>> recordVisit(
            @PathVariable Long userId, @RequestBody VisitRequest request) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Đã ghi lượt tham quan",
                    personalizationService.recordVisit(userId, request)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
