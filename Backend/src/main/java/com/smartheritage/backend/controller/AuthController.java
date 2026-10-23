package com.smartheritage.backend.controller;

import com.smartheritage.backend.entity.User;
import com.smartheritage.backend.repository.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * API xác thực cơ bản (Sprint 2 - Bảo):
 *  - POST /api/auth/register : đăng ký tài khoản du khách
 *  - POST /api/auth/login    : đăng nhập bằng username + mật khẩu
 *  - GET  /api/auth/users/{id}: lấy thông tin hồ sơ (Profile)
 * JWT và phân quyền RBAC nâng cao làm ở Sprint 3.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final int MIN_PASSWORD_LENGTH = 6;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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
                .role("VISITOR")
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
                    body.put("message", "Đăng nhập thành công!");
                    return ResponseEntity.ok(body);
                })
                .orElse(ResponseEntity.status(401).body(Map.of("message", "Sai tài khoản hoặc mật khẩu!")));
    }

    /** Hồ sơ người dùng cho màn hình Profile (Web & Mobile). */
    @GetMapping("/users/{id}")
    public ResponseEntity<?> getProfile(@PathVariable Long id) {
        return userRepository.findById(id)
                .<ResponseEntity<?>>map(user -> ResponseEntity.ok(toProfile(user)))
                .orElse(ResponseEntity.status(404).body(Map.of("message", "Không tìm thấy người dùng!")));
    }

    // ---------- Helpers ----------

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
    public static class LoginRequest {
        private String username;
        private String password;
    }
}
