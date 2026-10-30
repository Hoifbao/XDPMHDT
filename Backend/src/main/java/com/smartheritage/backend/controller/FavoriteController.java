package com.smartheritage.backend.controller;

import com.smartheritage.backend.dto.ApiResponse;
import com.smartheritage.backend.dto.FavoriteResponse;
import com.smartheritage.backend.entity.User;
import com.smartheritage.backend.security.CurrentUserProvider;
import com.smartheritage.backend.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me/favorites")
@RequiredArgsConstructor
public class FavoriteController {
    private final FavoriteService favoriteService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<FavoriteResponse>>> list(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        User user = currentUserProvider.requireCurrentUser();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100));
        return ResponseEntity.ok(ApiResponse.ok("Danh sách hiện vật yêu thích", favoriteService.list(user.getId(), pageable)));
    }

    /** POST is the team contract; PUT is accepted as an idempotent compatibility alias. */
    @RequestMapping(value = "/{artifactId}", method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<ApiResponse<Void>> add(
        @PathVariable Long artifactId) {
        User user = currentUserProvider.requireCurrentUser();
        favoriteService.add(user, artifactId);
        return ResponseEntity.ok(ApiResponse.ok("Đã lưu hiện vật vào yêu thích", null));
    }

    @DeleteMapping("/{artifactId}")
    public ResponseEntity<ApiResponse<Void>> remove(
        @PathVariable Long artifactId) {
        User user = currentUserProvider.requireCurrentUser();
        favoriteService.remove(user.getId(), artifactId);
        return ResponseEntity.ok(ApiResponse.ok("Đã bỏ yêu thích", null));
    }

    @GetMapping("/{artifactId}/exists")
    public ResponseEntity<ApiResponse<Boolean>> exists(
        @PathVariable Long artifactId) {
        User user = currentUserProvider.requireCurrentUser();
        return ResponseEntity.ok(ApiResponse.ok("Trạng thái yêu thích", favoriteService.exists(user.getId(), artifactId)));
    }
}
