package com.smartheritage.backend.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Tiện ích cho các Service/Controller khác (Phú, Tường...) lấy người dùng đang đăng nhập:
 * <pre>
 *   Long userId = SecurityUtils.currentUserId().orElseThrow();
 * </pre>
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<AuthPrincipal> currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }

    public static Optional<Long> currentUserId() {
        return currentUser().map(AuthPrincipal::userId);
    }
}
