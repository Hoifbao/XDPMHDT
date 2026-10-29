package com.smartheritage.common.web;

import com.smartheritage.common.security.AuthPrincipal;
import com.smartheritage.common.security.Roles;
import com.smartheritage.common.security.UserHeaders;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Người dùng đang gọi API, đọc từ header X-User-* mà Gateway đã gắn (Sprint 4).
 * Service KHÔNG tự kiểm tra JWT nữa. Dùng trong Service/Controller (chỉ service Spring MVC):
 * <pre>
 *   Long userId = CurrentUser.requireUserId();         // 401 nếu chưa đăng nhập
 *   Optional&lt;AuthPrincipal&gt; me = CurrentUser.get();   // rỗng nếu là khách
 *   boolean admin = CurrentUser.isAdmin();
 * </pre>
 * Lưu ý: gọi thẳng service (vd http://localhost:8082) không qua Gateway thì không có header,
 * nên các API cần đăng nhập sẽ trả 401. Test qua http://localhost:8080.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Optional<AuthPrincipal> get() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (!(attrs instanceof ServletRequestAttributes servletAttrs)) {
            return Optional.empty();
        }
        HttpServletRequest request = servletAttrs.getRequest();
        String id = request.getHeader(UserHeaders.USER_ID);
        String role = request.getHeader(UserHeaders.USER_ROLE);
        if (id == null || id.isBlank() || role == null || role.isBlank()) {
            return Optional.empty();
        }
        try {
            String name = request.getHeader(UserHeaders.USER_NAME);
            String username = name == null ? "" : URLDecoder.decode(name, StandardCharsets.UTF_8);
            return Optional.of(new AuthPrincipal(Long.parseLong(id.trim()), username, role.trim()));
        } catch (IllegalArgumentException e) { // gồm NumberFormatException
            return Optional.empty();
        }
    }

    public static Optional<Long> userId() {
        return get().map(AuthPrincipal::userId);
    }

    /** Lấy userId, chưa đăng nhập thì ném 401. */
    public static Long requireUserId() {
        return userId().orElseThrow(() -> ApiException.unauthorized(
                "Bạn chưa đăng nhập hoặc phiên đăng nhập đã hết hạn!"));
    }

    public static boolean isAdmin() {
        return get().map(p -> Roles.ADMIN.equals(p.role())).orElse(false);
    }
}
