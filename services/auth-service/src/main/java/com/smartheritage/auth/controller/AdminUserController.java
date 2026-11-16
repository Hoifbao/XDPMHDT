package com.smartheritage.auth.controller;

import com.smartheritage.auth.entity.User;
import com.smartheritage.auth.repository.UserRepository;
import com.smartheritage.auth.service.RefreshTokenService;
import com.smartheritage.common.security.AuthPrincipal;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Quản lý người dùng cho Web Admin (Bảo - Sprint 7).
 * Yêu cầu đề bài: module 6 "managing users", Task Package 1 "user management".
 *  - GET   /api/admin/users              : danh sách (phân trang, tìm theo username/email/họ tên, lọc role)
 *  - PATCH /api/admin/users/{id}/role    : đổi vai trò VISITOR <-> ADMIN
 *  - PATCH /api/admin/users/{id}/status  : khoá / mở khoá tài khoản
 * Gateway chuyển /api/admin/users/** sang auth-service và chỉ cho ADMIN (AccessRules luật /api/admin/**).
 * Controller vẫn tự kiểm header X-User-* để gọi thẳng cổng 8081 không vượt quyền được.
 * Không cho ADMIN tự khoá hoặc tự hạ quyền chính mình (tránh hệ thống không còn ai quản trị).
 * Khoá tài khoản: thu hồi mọi refresh token; access token cũ tự hết hạn sau tối đa 15 phút.
 */
@Tag(name = "Admin - Users", description = "Quản lý người dùng (chỉ ADMIN): danh sách, đổi vai trò, khoá/mở khoá")
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> VALID_ROLES = Set.of(Roles.VISITOR, Roles.ADMIN);
    private static final String JSON = "application/json";
    private static final String EX_PAGE = "{\"content\": [{\"userId\": 2, \"username\": \"visitor_an\", "
            + "\"email\": \"an.nguyen@example.com\", \"fullName\": \"Nguyễn Văn An\", \"role\": \"VISITOR\", "
            + "\"preferredLanguage\": \"vi\", \"locked\": false, \"createdAt\": \"2026-10-01T08:05\"}], "
            + "\"page\": 0, \"size\": 20, \"totalElements\": 1, \"totalPages\": 1}";
    private static final String EX_USER = "{\"message\": \"Đã khoá tài khoản visitor_an!\", \"user\": {\"userId\": 2, "
            + "\"username\": \"visitor_an\", \"role\": \"VISITOR\", \"locked\": true}}";

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    @Operation(summary = "Danh sách người dùng (chỉ ADMIN)",
            description = "Sắp xếp theo id tăng dần. q tìm trong username, email, họ tên (không phân biệt hoa thường). "
                    + "role = VISITOR hoặc ADMIN để lọc. size tối đa " + MAX_PAGE_SIZE + ".")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Danh sách 1 trang",
                    content = @Content(mediaType = JSON, examples = @ExampleObject(value = EX_PAGE))),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập"),
            @ApiResponse(responseCode = "403", description = "Không phải ADMIN")
    })
    @GetMapping
    public ResponseEntity<?> list(
            @Parameter(description = "Từ khoá tìm kiếm", example = "an") @RequestParam(required = false) String q,
            @Parameter(description = "Lọc theo vai trò", example = "VISITOR") @RequestParam(required = false) String role,
            @Parameter(description = "Trang (từ 0)", example = "0") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số dòng mỗi trang", example = "20") @RequestParam(defaultValue = "20") int size) {
        Optional<ResponseEntity<?>> denied = requireAdmin();
        if (denied.isPresent()) {
            return denied.get();
        }
        String keyword = q == null || q.isBlank() ? null : q.trim();
        String roleFilter = role == null || role.isBlank() ? null : role.trim().toUpperCase();
        if (roleFilter != null && !VALID_ROLES.contains(roleFilter)) {
            return badRequest("Vai trò chỉ được là VISITOR hoặc ADMIN!");
        }
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        Page<User> result = userRepository.search(keyword, roleFilter,
                PageRequest.of(safePage, safeSize, Sort.by("id").ascending()));
        List<Map<String, Object>> rows = result.getContent().stream().map(AdminUserController::toRow).toList();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("content", rows);
        body.put("page", result.getNumber());
        body.put("size", result.getSize());
        body.put("totalElements", result.getTotalElements());
        body.put("totalPages", result.getTotalPages());
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "Đổi vai trò người dùng (chỉ ADMIN)",
            description = "role = VISITOR hoặc ADMIN. Không đổi được vai trò của chính mình. "
                    + "Vai trò mới có hiệu lực khi người đó đăng nhập lại hoặc làm mới token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đã đổi vai trò"),
            @ApiResponse(responseCode = "400", description = "Vai trò sai hoặc tự đổi vai trò của mình",
                    content = @Content(mediaType = JSON, examples = @ExampleObject(
                            value = "{\"message\": \"Bạn không thể tự đổi vai trò của chính mình!\"}"))),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập"),
            @ApiResponse(responseCode = "403", description = "Không phải ADMIN"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy người dùng")
    })
    @PatchMapping("/{id}/role")
    public ResponseEntity<?> changeRole(@PathVariable Long id, @RequestBody RoleRequest req) {
        Optional<ResponseEntity<?>> denied = requireAdmin();
        if (denied.isPresent()) {
            return denied.get();
        }
        String newRole = req == null || req.getRole() == null ? "" : req.getRole().trim().toUpperCase();
        if (!VALID_ROLES.contains(newRole)) {
            return badRequest("Vai trò chỉ được là VISITOR hoặc ADMIN!");
        }
        if (isSelf(id)) {
            return badRequest("Bạn không thể tự đổi vai trò của chính mình!");
        }
        Optional<User> found = userRepository.findById(id);
        if (found.isEmpty()) {
            return notFound();
        }
        User user = found.get();
        user.setRole(newRole);
        userRepository.save(user);
        return ResponseEntity.ok(result("Đã đổi vai trò của " + user.getUsername() + " thành " + newRole + "!", user));
    }

    @Operation(summary = "Khoá / mở khoá tài khoản (chỉ ADMIN)",
            description = "locked = true để khoá: người đó không đăng nhập, không làm mới token được và mọi phiên "
                    + "(refresh token) bị thu hồi. locked = false để mở khoá. Không tự khoá chính mình được.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đã cập nhật trạng thái",
                    content = @Content(mediaType = JSON, examples = @ExampleObject(value = EX_USER))),
            @ApiResponse(responseCode = "400", description = "Thiếu locked hoặc tự khoá chính mình",
                    content = @Content(mediaType = JSON, examples = @ExampleObject(
                            value = "{\"message\": \"Bạn không thể tự khoá tài khoản của chính mình!\"}"))),
            @ApiResponse(responseCode = "401", description = "Chưa đăng nhập"),
            @ApiResponse(responseCode = "403", description = "Không phải ADMIN"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy người dùng")
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> changeStatus(@PathVariable Long id, @RequestBody StatusRequest req) {
        Optional<ResponseEntity<?>> denied = requireAdmin();
        if (denied.isPresent()) {
            return denied.get();
        }
        if (req == null || req.getLocked() == null) {
            return badRequest("Vui lòng gửi locked = true (khoá) hoặc false (mở khoá)!");
        }
        boolean lock = req.getLocked();
        if (lock && isSelf(id)) {
            return badRequest("Bạn không thể tự khoá tài khoản của chính mình!");
        }
        Optional<User> found = userRepository.findById(id);
        if (found.isEmpty()) {
            return notFound();
        }
        User user = found.get();
        user.setLocked(lock);
        userRepository.save(user);
        if (lock) {
            refreshTokenService.revokeAllOf(user.getId());
        }
        String message = (lock ? "Đã khoá tài khoản " : "Đã mở khoá tài khoản ") + user.getUsername() + "!";
        return ResponseEntity.ok(result(message, user));
    }

    // ---------- Helpers ----------

    /** Rỗng nếu là ADMIN; ngược lại trả sẵn 401 (chưa đăng nhập) hoặc 403 (không phải ADMIN). */
    private static Optional<ResponseEntity<?>> requireAdmin() {
        Optional<AuthPrincipal> me = CurrentUser.get();
        if (me.isEmpty()) {
            return Optional.of(ResponseEntity.status(401)
                    .body(Map.of("message", "Bạn chưa đăng nhập hoặc phiên đăng nhập đã hết hạn!")));
        }
        if (!me.get().isAdmin()) {
            return Optional.of(ResponseEntity.status(403)
                    .body(Map.of("message", "Chỉ quản trị viên mới được quản lý người dùng!")));
        }
        return Optional.empty();
    }

    private static boolean isSelf(Long id) {
        return CurrentUser.userId().map(myId -> myId.equals(id)).orElse(false);
    }

    /** Dòng trong bảng quản lý: giống hồ sơ /api/auth/me, thêm locked. Không bao giờ trả passwordHash. */
    static Map<String, Object> toRow(User user) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("userId", user.getId());
        row.put("username", user.getUsername());
        row.put("email", user.getEmail());
        row.put("fullName", user.getFullName() != null ? user.getFullName() : "");
        row.put("role", user.getRole());
        row.put("preferredLanguage", user.getPreferredLanguage());
        row.put("locked", user.isLocked());
        row.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : "");
        return row;
    }

    private static Map<String, Object> result(String message, User user) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message);
        body.put("user", toRow(user));
        return body;
    }

    private static ResponseEntity<?> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }

    private static ResponseEntity<?> notFound() {
        return ResponseEntity.status(404).body(Map.of("message", "Không tìm thấy người dùng!"));
    }

    @Data
    public static class RoleRequest {
        @Schema(description = "Vai trò mới", example = "ADMIN", requiredMode = Schema.RequiredMode.REQUIRED)
        private String role;
    }

    @Data
    public static class StatusRequest {
        @Schema(description = "true = khoá, false = mở khoá", example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED)
        private Boolean locked;
    }
}
