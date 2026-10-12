package com.smartheritage.backend.controller;

import com.smartheritage.backend.entity.User;
import com.smartheritage.backend.repository.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username đã tồn tại!"));
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email đã được sử dụng!"));
        }

        User newUser = User.builder()
                .username(req.getUsername())
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .fullName(req.getFullName())
                .preferredLanguage("vi")
                .role("VISITOR")
                .build();

        userRepository.save(newUser);
        return ResponseEntity.ok(Map.of("message", "Đăng ký thành công!", "userId", newUser.getId()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        return userRepository.findByUsername(req.getUsername())
                .filter(user -> passwordEncoder.matches(req.getPassword(), user.getPasswordHash()))
                .map(user -> ResponseEntity.ok(Map.of(
                        "message", "Đăng nhập thành công!",
                        "userId", user.getId(),
                        "username", user.getUsername(),
                        "fullName", user.getFullName() != null ? user.getFullName() : "",
                        "role", user.getRole()
                )))
                .orElse(ResponseEntity.status(401).body(Map.of("message", "Sai tài khoản hoặc mật khẩu!")));
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
