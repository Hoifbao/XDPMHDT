package com.smartheritage.backend.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import com.smartheritage.backend.entity.User;
import com.smartheritage.backend.repository.UserRepository;
import com.smartheritage.backend.security.AuthPrincipal;
import com.smartheritage.backend.security.JwtService;
import com.smartheritage.backend.security.Roles;
import com.smartheritage.backend.service.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * API xác thực (Bảo):
 *  - POST /api/auth/register  : đăng ký tài khoản du khách (role VISITOR)
 *  - POST /api/auth/login     : đăng nhập, trả về hồ sơ + accessToken + refreshToken
 *  - POST /api/auth/refresh   : đổi refreshToken lấy cặp token mới (token cũ bị thu hồi)
 *  - POST /api/auth/logout    : thu hồi refreshToken
 *  - GET  /api/auth/me        : hồ sơ của người đang đăng nhập (cần Bearer token)
 *  - GET  /api/auth/users/{id}: hồ sơ theo id (chỉ của chính mình, ADMIN xem được tất cả)
 */
@Tag(name = "1. Xác thực", description = "Đăng ký, đăng nhập, refresh/logout JWT, hồ sơ (Bảo)")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final int MIN_PASSWORD_LENGTH = 6;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        String username = trim(req.getUsername());
        String email = trim(req.getEmail());
        String fullName = trim(req.getFullName());
        String password = req.getPassword();

        // Kiểm tra dữ liệu đầu vào
        if (username.isEmpty() || email.isEmpty() || password == null || password.isEmpty()) {
            return badRequest("Vui lòng nhập đầy đủ tên đăng nhập, email và mật khẩu!");
        }
        if (username.length() > 50) {
            return badRequest("Tên đăng nhập tối đa 50 ký tự!");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return badRequest("Email không hợp lệ!");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            return badRequest("Mật khẩu phải có ít nhất " + MIN_PASSWORD_LENGTH + " ký tự!");
        }
        if (userRepository.existsByUsername(username)) {
            return badRequest("Username đã tồn tại!");
        }
        if (userRepository.existsByEmail(email)) {
            return badRequest("Email đã được sử dụng!");
        }

        User newUser = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .fullName(fullName.isEmpty() ? null : fullName)
                .preferredLanguage("vi")
                .role(Roles.VISITOR)
                .build();

        userRepository.save(newUser);
        return ResponseEntity.ok(Map.of("message", "Đăng ký thành công!", "userId", newUser.getId()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        String username = trim(req.getUsername());
        if (username.isEmpty() || req.getPassword() == null || req.getPassword().isEmpty()) {
            return badRequest("Vui lòng nhập tên đăng nhập và mật khẩu!");
        }

        return userRepository.findByUsername(username)
                .filter(user -> passwordEncoder.matches(req.getPassword(), user.getPasswordHash()))
                .<ResponseEntity<?>>map(user -> {
                    Map<String, Object> body = toProfile(user);
                    body.putAll(issueTokens(user));
                    body.put("message", "Đăng nhập thành công!");
                    return ResponseEntity.ok(body);
                })
                .orElse(ResponseEntity.status(401).body(Map.of("message", "Sai tài khoản hoặc mật khẩu!")));
    }

    @Operation(summary = "Đổi refreshToken lấy accessToken + refreshToken mới (refreshToken cũ bị vô hiệu)")
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshRequest req) {
        return refreshTokenService.consume(req.getRefreshToken())
                .<ResponseEntity<?>>map(user -> {
                    Map<String, Object> body = toProfile(user);
                    body.putAll(issueTokens(user));
                    body.put("message", "Làm mới token thành công!");
                    return ResponseEntity.ok(body);
                })
                .orElse(ResponseEntity.status(401).body(Map.of(
                        "message", "Refresh token không hợp lệ hoặc đã hết hạn, vui lòng đăng nhập lại!")));
    }

    @Operation(summary = "Đăng xuất: thu hồi refreshToken. Access token tự hết hạn sau tối đa 15 phút")
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody(required = false) RefreshRequest req) {
        refreshTokenService.revoke(req != null ? req.getRefreshToken() : null);
        return ResponseEntity.ok(Map.of("message", "Đăng xuất thành công!"));
    }

    @Operation(summary = "Hồ sơ của người đang đăng nhập", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal AuthPrincipal me) {
        return getProfile(me.userId(), me);
    }

    /** Hồ sơ người dùng cho màn hình Profile (Web & Mobile). */
    @Operation(summary = "Hồ sơ theo id (của chính mình, hoặc ADMIN xem bất kỳ)",
            security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/users/{id}")
    public ResponseEntity<?> getProfile(@PathVariable Long id, @AuthenticationPrincipal AuthPrincipal me) {
        if (me == null || (!me.isAdmin() && !me.userId().equals(id))) {
            return ResponseEntity.status(403).body(Map.of("message", "Bạn chỉ được xem hồ sơ của chính mình!"));
        }
        return userRepository.findById(id)
                .<ResponseEntity<?>>map(user -> ResponseEntity.ok(toProfile(user)))
                .orElse(ResponseEntity.status(404).body(Map.of("message", "Không tìm thấy người dùng!")));
    }

    // ---------- Helpers ----------

    private Map<String, Object> issueTokens(User user) {
        Map<String, Object> tokens = new LinkedHashMap<>();
        tokens.put("tokenType", "Bearer");
        tokens.put("accessToken", jwtService.generateAccessToken(user));
        tokens.put("expiresIn", jwtService.getAccessTtlSeconds());
        tokens.put("refreshToken", refreshTokenService.issue(user));
        tokens.put("refreshExpiresIn", refreshTokenService.getRefreshTtlSeconds());
        return tokens;
    }

    private static Map<String, Object> toProfile(User user) {
        // Không bao giờ trả passwordHash ra ngoài
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("userId", user.getId());
        body.put("username", user.getUsername());
        body.put("email", user.getEmail());
        body.put("fullName", user.getFullName() != null ? user.getFullName() : "");
        body.put("role", user.getRole());
        body.put("preferredLanguage", user.getPreferredLanguage());
        body.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : "");
        return body;
    }

    private static ResponseEntity<?> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    @Data
    public static class RegisterRequest {
        private String username;
        private String email;
        private String password;
        private String fullName;
    }

    @Data
    public static class RefreshRequest {
        private String refreshToken;
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }
}
