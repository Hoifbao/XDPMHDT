package com.smartheritage.backend.security;

/**
 * Người dùng đang gọi API, lấy từ access token.
 * Dùng trong Controller: {@code @AuthenticationPrincipal AuthPrincipal me}
 * hoặc {@link SecurityUtils#currentUser()}.
 */
public record AuthPrincipal(Long userId, String username, String role) {

    public boolean isAdmin() {
        return Roles.ADMIN.equals(role);
    }
}
