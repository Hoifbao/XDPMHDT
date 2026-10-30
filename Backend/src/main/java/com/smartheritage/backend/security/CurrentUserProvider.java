package com.smartheritage.backend.security;

import com.smartheritage.backend.entity.User;
import com.smartheritage.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Lấy người dùng đang gọi API từ access token (JWT) để đọc/ghi dữ liệu cá nhân.
 * Không nhận header X-User-Id từ client: từ W4 Gateway xóa mọi header X-User-* do client gửi.
 * W5: personalization-service đọc userId bằng CurrentUser (services/common).
 */
@Component
@RequiredArgsConstructor
public class CurrentUserProvider {
    private final UserRepository userRepository;

    public User requireCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw unauthorized();
        }
        if (auth.getPrincipal() instanceof AuthPrincipal principal) {
            return userRepository.findById(principal.userId()).orElseThrow(this::unauthorized);
        }
        String username = auth.getName();
        if (username != null && !username.isBlank()) {
            return userRepository.findByUsername(username).orElseThrow(this::unauthorized);
        }
        throw unauthorized();
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cần đăng nhập để dùng chức năng cá nhân hóa.");
    }
}
