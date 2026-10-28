package com.smartheritage.common.security;

import java.security.Principal;

/** Người dùng đang gọi API: lấy từ access token (Gateway) hoặc từ header X-User-* (các service). */
public record AuthPrincipal(Long userId, String username, String role) implements Principal {

    @Override
    public String getName() {
        return username;
    }

    public boolean isAdmin() {
        return Roles.ADMIN.equals(role);
    }
}
