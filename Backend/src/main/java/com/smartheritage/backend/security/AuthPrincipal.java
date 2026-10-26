package com.smartheritage.backend.security;

import java.security.Principal;

/**
 * Người dùng đang gọi API, lấy từ access token.
 * Dùng trong Controller: {@code @AuthenticationPrincipal AuthPrincipal me}
 * hoặc {@link SecurityUtils#currentUser()}.
 * Cài đặt Principal để authentication.getName() trả về username
 * (code dùng auth.getName(), vd CurrentUserProvider của Phú, vẫn chạy đúng).
 */
public record AuthPrincipal(Long userId, String username, String role) implements Principal {

    @Override
    public String getName() {
        return username;
    }

    public boolean isAdmin() {
        return Roles.ADMIN.equals(role);
    }
}
