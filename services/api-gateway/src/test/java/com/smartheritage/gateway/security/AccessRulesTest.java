package com.smartheritage.gateway.security;

import com.smartheritage.common.security.AuthPrincipal;
import com.smartheritage.common.security.Roles;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import static com.smartheritage.gateway.security.AccessRules.Decision.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Kiểm tra luật RBAC của Gateway khớp kết quả test tuần 3 (401/403).
 * Chạy (sau khi mvn -DskipTests install ở thư mục gốc): mvn -pl services/api-gateway test
 */
class AccessRulesTest {

    private final AccessRules rules = new AccessRules();
    private final AuthPrincipal admin = new AuthPrincipal(1L, "admin", Roles.ADMIN);
    private final AuthPrincipal visitor = new AuthPrincipal(2L, "visitor1", Roles.VISITOR);

    @Test
    void getCongKhai() {
        assertEquals(ALLOW, rules.check(HttpMethod.GET, "/api/artifacts", null));
        assertEquals(ALLOW, rules.check(HttpMethod.GET, "/api/heritage-sites/nearby", null));
        assertEquals(ALLOW, rules.check(HttpMethod.GET, "/api/ibeacons/registry", null));
    }

    @Test
    void dangNhapDangKyCongKhai() {
        assertEquals(ALLOW, rules.check(HttpMethod.POST, "/api/auth/login", null));
        assertEquals(ALLOW, rules.check(HttpMethod.POST, "/api/auth/register", null));
        assertEquals(ALLOW, rules.check(HttpMethod.POST, "/api/auth/refresh", null));
        assertEquals(ALLOW, rules.check(HttpMethod.POST, "/api/auth/logout", null));
    }

    @Test
    void ghiDuLieu_khongToken401_visitor403_admin200() {
        assertEquals(UNAUTHORIZED, rules.check(HttpMethod.DELETE, "/api/artifacts/5", null));
        assertEquals(FORBIDDEN, rules.check(HttpMethod.DELETE, "/api/artifacts/5", visitor));
        assertEquals(ALLOW, rules.check(HttpMethod.DELETE, "/api/artifacts/5", admin));
        assertEquals(FORBIDDEN, rules.check(HttpMethod.POST, "/api/heritage-sites", visitor));
        assertEquals(ALLOW, rules.check(HttpMethod.PUT, "/api/ibeacons/1/assign", admin));
    }

    @Test
    void hoSo_canDangNhap_chiXemCuaMinh() {
        assertEquals(UNAUTHORIZED, rules.check(HttpMethod.GET, "/api/auth/me", null));
        assertEquals(ALLOW, rules.check(HttpMethod.GET, "/api/auth/me", visitor));
        assertEquals(UNAUTHORIZED, rules.check(HttpMethod.GET, "/api/users/2/favorites", null));
        assertEquals(ALLOW, rules.check(HttpMethod.GET, "/api/users/2/favorites", visitor));
        assertEquals(FORBIDDEN, rules.check(HttpMethod.GET, "/api/users/1/favorites", visitor));
        assertEquals(ALLOW, rules.check(HttpMethod.GET, "/api/users/2/visits", admin));
    }

    @Test
    void me_admin_thongKe() {
        assertEquals(UNAUTHORIZED, rules.check(HttpMethod.GET, "/api/me/favorites", null));
        assertEquals(ALLOW, rules.check(HttpMethod.POST, "/api/me/visits/batch", visitor));
        assertEquals(FORBIDDEN, rules.check(HttpMethod.GET, "/api/admin/visits", visitor));
        assertEquals(ALLOW, rules.check(HttpMethod.GET, "/api/admin/visits", admin));
        assertEquals(UNAUTHORIZED, rules.check(HttpMethod.GET, "/api/statistics/summary", null));
        assertEquals(FORBIDDEN, rules.check(HttpMethod.GET, "/api/statistics/summary", visitor));
    }

    @Test
    void guiDanhGia_visitorDuoc_khachKhong() {
        assertEquals(UNAUTHORIZED, rules.check(HttpMethod.POST, "/api/artifacts/3/feedbacks", null));
        assertEquals(ALLOW, rules.check(HttpMethod.POST, "/api/artifacts/3/feedbacks", visitor));
        assertEquals(ALLOW, rules.check(HttpMethod.GET, "/api/artifacts/3/feedbacks", null));
    }

    @Test
    void demoVaPreflightCongKhai() {
        assertEquals(ALLOW, rules.check(HttpMethod.POST, "/api/demo/redis/cache", null));
        assertEquals(ALLOW, rules.check(HttpMethod.OPTIONS, "/api/artifacts/5", null));
        assertEquals(ALLOW, rules.check(HttpMethod.GET, "/swagger-ui.html", null));
    }
}
