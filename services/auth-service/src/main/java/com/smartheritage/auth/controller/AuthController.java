package com.smartheritage.auth.controller;

import com.smartheritage.auth.entity.User;
import com.smartheritage.auth.repository.UserRepository;
import com.smartheritage.auth.service.RefreshTokenService;
import com.smartheritage.common.security.AuthPrincipal;
import com.smartheritage.common.security.JwtService;
import com.smartheritage.common.security.Roles;
import com.smartheritage.common.web.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * API xác thực (Bảo) - tách từ Backend sang auth-service ở Sprint 4.
 * Đường dẫn và JSON trả về GIỮ NGUYÊN như tuần 3, nên Web Admin (auth.js) và Mobile (AuthService)
 * không phải sửa gì:
 *  - POST /api/auth/register  : đăng ký tài khoản du khách (role VISITOR)
 *  - POST /api/auth/login     : đăng nhập, trả về hồ sơ + accessToken + refreshToken
 *  - POST /api/auth/refresh   : đổi refreshToken lấy cặp token mới (token cũ bị thu hồi)
 *  - POST /api/auth/logout    : thu hồi refreshToken
 *  - GET  /api/auth/me        : hồ sơ của người đang đăng nhập
 *  - GET  /api/auth/users/{id}: hồ sơ theo id (chỉ của chính mình, ADMIN xem được tất cả)
 * Gateway kiểm tra JWT; /me và /users/{id} đọc người dùng từ header X-User-* (CurrentUser).
 * Swagger (Bảo - Sprint 5): ví dụ request/response và mã lỗi theo DoD (Docs/testing/swagger-review-w4.md).
 */
@Tag(name = "Auth", description = "Đăng ký, đăng nhập JWT (access + refresh token), đăng xuất, hồ sơ người dùng")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final String NOT_LOGGED_IN = "Bạn chưa đăng nhập hoặc phiên đăng nhập đã hết hạn!";

    // ---------- Ví dụ JSON cho Swagger (chỉ để hiển thị tài liệu) ----------
    private static final String JSON = "application/json";
    private static final String EX_PROFILE = "{\"userId\": 2, \"username\": \"visitor01\", "
            + "\"email\": \"visitor01@gmail.com\", \"fullName\": \"Nguyễn Văn A\", \"role\": \"VISITOR\", "
            + "\"preferredLanguage\": \"vi\", \"createdAt\": \"2026-10-20T09:15:00\"}";
    private static final String EX_TOKENS = "{\"userId\": 2, \"username\": \"visitor01\", "
            + "\"email\": \"visitor01@gmail.com\", \"fullName\": \"Nguyễn Văn A\", \"role\": \"VISITOR\", "
            + "\"preferredLanguage\": \"vi\", \"createdAt\": \"2026-10-20T09:15:00\", "
            + "\"tokenType\": \"Bearer\", \"accessToken\": \"eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIyIn0.abc\", "
            + "\"expiresIn\": 900, \"refreshToken\": \"q8Zr3vX1kP0dN4sT7wYbE2mHcJ6uLfA9gRiOeQzW5nU\", "
            + "\"refreshExpiresIn\": 604800, \"message\": \"Đăng nhập thành công!\"}";
    private static final String EX_401 = "{\"message\": \"" + NOT_LOGGED_IN + "\"}";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Operation(summary = "Đăng ký tài khoản du khách (VISITOR)",
            description = "Công khai. Mật khẩu tối thiểu 6 ký tự, username tối đa 50 ký tự, email đúng định dạng. "
                    + "Tài khoản mới luôn có role VISITOR và ngôn ngữ mặc định vi.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đăng ký thành công",
                    content = @Content(mediaType = JSON, examples = @ExampleObject(
                            value = "{\"message\": \"Đăng ký thành công!\", \"userId\": 2}"))),
            @ApiResponse(responseCode = "400",
                    description = "Thiếu trường bắt buộc, email sai định dạng, mật khẩu < 6 ký tự, username/email đã tồn tại",
                    content = @Content(mediaType = JSON, examples = @ExampleObject(
                            value = "{\"message\": \"Username đã tồn tại!\"}")))
    })
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

    @Operation(summary = "Đăng nhập, trả về hồ sơ + accessToken + refreshToken",
            description = "Công khai. accessToken sống 15 phút (expiresIn, giây), gửi kèm header "
                    + "Authorization: Bearer <accessToken>. refreshToken sống 7 ngày, dùng cho /refresh. "
                    + "Tài khoản mẫu: admin / Test@1234.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đăng nhập thành công",
                    content = @Content(mediaType = JSON, examples = @ExampleObject(value = EX_TOKENS))),
            @ApiResponse(responseCode = "400", description = "Thiếu tên đăng nhập hoặc mật khẩu",
                    content = @Content(mediaType = JSON, examples = @ExampleObject(
                            value = "{\"message\": \"Vui lòng nhập tên đăng nhập và mật khẩu!\"}"))),
            @ApiResponse(responseCode = "401", description = "Sai tài khoản hoặc mật khẩu",
                    content = @Content(mediaType = JSON, examples = @ExampleObject(
                            value = "{\"message\": \"Sai tài khoản hoặc mật khẩu!\"}")))
    })
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

    @Operation(summary = "Đổi refreshToken lấy accessToken + refreshToken mới (refreshToken cũ bị vô hiệu)",
            description = "Công khai (không cần accessToken). Mỗi refreshToken chỉ dùng được 1 lần: "
                    + "gọi lại bằng token cũ sẽ nhận 401.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cấp cặp token mới (cùng cấu trúc với /login)",
                    content = @Content(mediaType = JSON, examples = @ExampleObject(value = EX_TOKENS))),
            @ApiResponse(responseCode = "401", description = "refreshToken không hợp lệ, đã dùng, đã thu hồi hoặc hết hạn",
                    content = @Content(mediaType = JSON, examples = @ExampleObject(
                            value = "{\"message\": \"Refresh token không hợp lệ hoặc đã hết hạn, vui lòng đăng nhập lại!\"}")))
    })
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

    @Operation(summary = "Đăng xuất: thu hồi refreshToken. Access token tự hết hạn sau tối đa 15 phút",
            description = "Luôn trả 200, kể cả khi body rỗng hoặc refreshToken không tồn tại (không lộ token nào hợp lệ).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đã thu hồi refreshToken (nếu có)",
                    content = @Content(mediaType = JSON, examples = @ExampleObject(
                            value = "{\"message\": \"Đăng xuất thành công!\"}")))
    })
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody(required = false) RefreshRequest req) {
        refreshTokenService.revoke(req != null ? req.getRefreshToken() : null);
        return ResponseEntity.ok(Map.of("message", "Đăng xuất thành công!"));
    }

    @Operation(summary = "Hồ sơ của người đang đăng nhập (gọi qua Gateway kèm Bearer token)",
            description = "Gọi qua Gateway http://localhost:8080 với header Authorization: Bearer <accessToken>. "
                    + "Gọi thẳng cổng 8081 không có header X-User-Id nên trả 401.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Hồ sơ người dùng (không có passwordHash)",
                    content = @Content(mediaType = JSON, examples = @ExampleObject(value = EX_PROFILE))),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập, token sai hoặc hết hạn",
                    content = @Content(mediaType = JSON, examples = @ExampleObject(value = EX_401))),
            @ApiResponse(responseCode = "404", description = "Tài khoản đã bị xóa",
                    content = @Content(mediaType = JSON, examples = @ExampleObject(
                            value = "{\"message\": \"Không tìm thấy người dùng!\"}")))
    })
    @GetMapping("/me")
    public ResponseEntity<?> me() {
        Optional<AuthPrincipal> me = CurrentUser.get();
        if (me.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("message", NOT_LOGGED_IN));
        }
        return profileOf(me.get().userId(), me.get());
    }

    /** Hồ sơ người dùng cho màn hình Profile (Web & Mobile). */
    @Operation(summary = "Hồ sơ theo id (của chính mình, hoặc ADMIN xem bất kỳ)",
            description = "Gọi qua Gateway kèm Bearer token. VISITOR chỉ xem được id của chính mình.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Hồ sơ người dùng",
                    content = @Content(mediaType = JSON, examples = @ExampleObject(value = EX_PROFILE))),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập, token sai hoặc hết hạn",
                    content = @Content(mediaType = JSON, examples = @ExampleObject(value = EX_401))),
            @ApiResponse(responseCode = "403", description = "VISITOR xem hồ sơ của người khác",
                    content = @Content(mediaType = JSON, examples = @ExampleObject(
                            value = "{\"message\": \"Bạn chỉ được xem hồ sơ của chính mình!\"}"))),
            @ApiResponse(responseCode = "404", description = "Không có người dùng với id này",
                    content = @Content(mediaType = JSON, examples = @ExampleObject(
                            value = "{\"message\": \"Không tìm thấy người dùng!\"}")))
    })
    @GetMapping("/users/{id}")
    public ResponseEntity<?> getProfile(@Parameter(description = "ID người dùng", example = "2") @PathVariable Long id) {
        Optional<AuthPrincipal> me = CurrentUser.get();
        if (me.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("message", NOT_LOGGED_IN));
        }
        return profileOf(id, me.get());
    }

    // ---------- Helpers ----------

    private ResponseEntity<?> profileOf(Long id, AuthPrincipal me) {
        if (!me.isAdmin() && !me.userId().equals(id)) {
            return ResponseEntity.status(403).body(Map.of("message", "Bạn chỉ được xem hồ sơ của chính mình!"));
        }
        return userRepository.findById(id)
                .<ResponseEntity<?>>map(user -> ResponseEntity.ok(toProfile(user)))
                .orElse(ResponseEntity.status(404).body(Map.of("message", "Không tìm thấy người dùng!")));
    }

    private Map<String, Object> issueTokens(User user) {
        Map<String, Object> tokens = new LinkedHashMap<>();
        tokens.put("tokenType", "Bearer");
        tokens.put("accessToken", jwtService.generateAccessToken(user.getId(), user.getUsername(), user.getRole()));
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
    @Schema(description = "Thông tin đăng ký tài khoản du khách")
    public static class RegisterRequest {
        @Schema(description = "Tên đăng nhập, tối đa 50 ký tự, không trùng", example = "visitor01",
                requiredMode = Schema.RequiredMode.REQUIRED)
        private String username;
        @Schema(description = "Email hợp lệ, không trùng", example = "visitor01@gmail.com",
                requiredMode = Schema.RequiredMode.REQUIRED)
        private String email;
        @Schema(description = "Mật khẩu, tối thiểu 6 ký tự", example = "Test@1234",
                requiredMode = Schema.RequiredMode.REQUIRED)
        private String password;
        @Schema(description = "Họ tên hiển thị (tuỳ chọn)", example = "Nguyễn Văn A")
        private String fullName;
    }

    @Data
    @Schema(description = "refreshToken nhận được từ /login hoặc /refresh")
    public static class RefreshRequest {
        @Schema(description = "refreshToken (dùng 1 lần)", example = "q8Zr3vX1kP0dN4sT7wYbE2mHcJ6uLfA9gRiOeQzW5nU",
                requiredMode = Schema.RequiredMode.REQUIRED)
        private String refreshToken;
    }

    @Data
    @Schema(description = "Thông tin đăng nhập")
    public static class LoginRequest {
        @Schema(description = "Tên đăng nhập", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
        private String username;
        @Schema(description = "Mật khẩu", example = "Test@1234", requiredMode = Schema.RequiredMode.REQUIRED)
        private String password;
    }
}
