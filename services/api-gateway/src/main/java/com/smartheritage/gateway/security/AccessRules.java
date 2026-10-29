package com.smartheritage.gateway.security;

import com.smartheritage.common.security.AuthPrincipal;
import com.smartheritage.common.security.Roles;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.Set;

/**
 * Luật phân quyền RBAC tại Gateway - chép nguyên từ SecurityConfig của Backend (tuần 3).
 * Xét từ trên xuống, khớp luật nào dùng luật đó:
 *  1. OPTIONS (preflight CORS)                                  -> cho qua
 *  2. POST /api/auth/register|login|refresh|logout              -> công khai
 *  3. /api/auth/me, /api/auth/users/**                          -> phải đăng nhập
 *  4. /api/users/{userId}/**                                    -> chính chủ hoặc ADMIN
 *  5. /api/me/**                                                -> phải đăng nhập
 *  6. /api/admin/**                                             -> chỉ ADMIN
 *  7. POST /api/artifacts/{id}/feedbacks                        -> VISITOR hoặc ADMIN
 *  8. /api/statistics/**                                        -> chỉ ADMIN
 *  9. /api/demo/**                                              -> công khai (demo Redis)
 * 10. GET /api/**                                               -> công khai
 * 11. POST/PUT/PATCH/DELETE /api/**                             -> chỉ ADMIN
 * 12. Còn lại (Swagger, trang lỗi...)                           -> cho qua
 * Thiếu/sai/hết hạn token -> 401; đúng token nhưng không đủ quyền -> 403.
 * Cần luật mới: thêm vào đây + AccessRulesTest, rồi cập nhật Docs/api/auth-jwt.md.
 */
public class AccessRules {

    public enum Decision { ALLOW, UNAUTHORIZED, FORBIDDEN }

    private static final PathPatternParser PARSER = new PathPatternParser();

    private static final Set<HttpMethod> WRITE_METHODS =
            Set.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE);

    private final PathPattern authPublic = p("/api/auth/{action:register|login|refresh|logout}");
    private final PathPattern authMe = p("/api/auth/me");
    private final PathPattern authUsers = p("/api/auth/users/**");
    private final PathPattern usersOwner = p("/api/users/{userId}/**");
    private final PathPattern me = p("/api/me/**");
    private final PathPattern admin = p("/api/admin/**");
    private final PathPattern feedbackSubmit = p("/api/artifacts/*/feedbacks");
    private final PathPattern statistics = p("/api/statistics/**");
    private final PathPattern demo = p("/api/demo/**");
    private final PathPattern api = p("/api/**");

    /**
     * @param method HTTP method của request
     * @param path   đường dẫn (không gồm query string), vd /api/artifacts/5
     * @param user   người dùng lấy từ token hợp lệ, hoặc null nếu là khách
     */
    public Decision check(HttpMethod method, String path, AuthPrincipal user) {
        PathContainer pc = PathContainer.parsePath(path);

        if (HttpMethod.OPTIONS.equals(method)) {
            return Decision.ALLOW;
        }
        if (HttpMethod.POST.equals(method) && authPublic.matches(pc)) {
            return Decision.ALLOW;
        }
        if (authMe.matches(pc) || authUsers.matches(pc)) {
            return loggedIn(user);
        }
        PathPattern.PathMatchInfo owner = usersOwner.matchAndExtract(pc);
        if (owner != null) {
            if (user == null) {
                return Decision.UNAUTHORIZED;
            }
            String userId = owner.getUriVariables().get("userId");
            boolean isOwner = userId != null && userId.equals(String.valueOf(user.userId()));
            return user.isAdmin() || isOwner ? Decision.ALLOW : Decision.FORBIDDEN;
        }
        if (me.matches(pc)) {
            return loggedIn(user);
        }
        if (admin.matches(pc)) {
            return adminOnly(user);
        }
        if (HttpMethod.POST.equals(method) && feedbackSubmit.matches(pc)) {
            if (user == null) {
                return Decision.UNAUTHORIZED;
            }
            return Roles.VISITOR.equals(user.role()) || user.isAdmin() ? Decision.ALLOW : Decision.FORBIDDEN;
        }
        if (statistics.matches(pc)) {
            return adminOnly(user);
        }
        if (demo.matches(pc)) {
            return Decision.ALLOW;
        }
        if (api.matches(pc)) {
            if (HttpMethod.GET.equals(method)) {
                return Decision.ALLOW;
            }
            if (WRITE_METHODS.contains(method)) {
                return adminOnly(user);
            }
        }
        return Decision.ALLOW;
    }

    private static Decision loggedIn(AuthPrincipal user) {
        return user == null ? Decision.UNAUTHORIZED : Decision.ALLOW;
    }

    private static Decision adminOnly(AuthPrincipal user) {
        if (user == null) {
            return Decision.UNAUTHORIZED;
        }
        return user.isAdmin() ? Decision.ALLOW : Decision.FORBIDDEN;
    }

    private static PathPattern p(String pattern) {
        return PARSER.parse(pattern);
    }
}
