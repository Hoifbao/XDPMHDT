package com.smartheritage.backend.security;

import com.smartheritage.backend.entity.User;
import com.smartheritage.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Resolves the caller to the existing users table.
 * A dev-only X-User-Id fallback exists solely because the repository's current
 * login flow does not issue JWT/set an authenticated principal yet. It is OFF by default.
 */
@Component
@RequiredArgsConstructor
public class CurrentUserProvider {
    private final UserRepository userRepository;

    @Value("${app.personalization.allow-dev-user-header:false}")
    private boolean allowDevUserHeader;

    public User requireCurrentUser(Long devUserId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            Object principal = auth.getPrincipal();
            if (principal instanceof User user) {
                return userRepository.findById(user.getId()).orElseThrow(() -> unauthorized());
            }
            String username = auth.getName();
            if (username != null && !username.isBlank()) {
                return userRepository.findByUsername(username).orElseThrow(() -> unauthorized());
            }
        }

        if (allowDevUserHeader && devUserId != null) {
            return userRepository.findById(devUserId).orElseThrow(() -> unauthorized());
        }
        throw unauthorized();
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED,
            "Cần đăng nhập. X-User-Id chỉ được phép trong local demo khi bật app.personalization.allow-dev-user-header=true.");
    }
}
