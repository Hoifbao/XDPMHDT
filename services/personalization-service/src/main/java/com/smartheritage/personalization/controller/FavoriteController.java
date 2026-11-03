package com.smartheritage.personalization.controller;

import com.smartheritage.common.api.ApiResponse;
import com.smartheritage.common.web.CurrentUser;
import com.smartheritage.personalization.dto.FavoriteResponse;
import com.smartheritage.personalization.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Personalization - Favorites", description = "Yêu thích của người đang đăng nhập")
@RestController
@RequestMapping("/api/me/favorites")
@RequiredArgsConstructor
public class FavoriteController {
    private final FavoriteService favoriteService;

    @Operation(summary = "Danh sách yêu thích của tôi")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<FavoriteResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = CurrentUser.requireUserId();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100));
        return ResponseEntity.ok(ApiResponse.ok("Danh sách hiện vật yêu thích", favoriteService.list(userId, pageable)));
    }

    @Operation(summary = "Thêm yêu thích (idempotent)")
    @RequestMapping(value = "/{artifactId}", method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<ApiResponse<Void>> add(@PathVariable Long artifactId) {
        favoriteService.add(CurrentUser.requireUserId(), artifactId);
        return ResponseEntity.ok(ApiResponse.ok("Đã lưu hiện vật vào yêu thích", null));
    }

    @Operation(summary = "Bỏ yêu thích")
    @DeleteMapping("/{artifactId}")
    public ResponseEntity<ApiResponse<Void>> remove(@PathVariable Long artifactId) {
        favoriteService.remove(CurrentUser.requireUserId(), artifactId);
        return ResponseEntity.ok(ApiResponse.ok("Đã bỏ yêu thích", null));
    }

    @Operation(summary = "Kiểm tra hiện vật đã được yêu thích chưa")
    @GetMapping("/{artifactId}/exists")
    public ResponseEntity<ApiResponse<Boolean>> exists(@PathVariable Long artifactId) {
        return ResponseEntity.ok(ApiResponse.ok("Trạng thái yêu thích",
                favoriteService.exists(CurrentUser.requireUserId(), artifactId)));
    }
}
