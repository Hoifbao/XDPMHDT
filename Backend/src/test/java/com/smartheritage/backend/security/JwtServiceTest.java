package com.smartheritage.backend.security;

import com.smartheritage.backend.entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Test JwtService không cần khởi động Spring hay MySQL: mvn test -Dtest=JwtServiceTest */
class JwtServiceTest {

    private static JwtService service(String secret, long ttl) {
        JwtProperties props = new JwtProperties();
        props.setSecret(secret);
        props.setAccessTokenTtlSeconds(ttl);
        return new JwtService(props);
    }

    private static User admin() {
        return User.builder().id(1L).username("admin").email("admin@example.com")
                .passwordHash("x").role(Roles.ADMIN).build();
    }

    @Test
    void tokenHopLe_docRaDungUserVaRole() {
        JwtService jwt = service("test-secret-key-0123456789-0123456789-abcdef", 900);
        String token = jwt.generateAccessToken(admin());

        AuthPrincipal p = jwt.parseAccessToken(token).orElseThrow();
        assertEquals(1L, p.userId());
        assertEquals("admin", p.username());
        assertTrue(p.isAdmin());
    }

    @Test
    void tokenBiSua_hoacKhacKhoa_biTuChoi() {
        JwtService jwt = service("test-secret-key-0123456789-0123456789-abcdef", 900);
        JwtService other = service("another-secret-key-0123456789-0123456789-xyz", 900);
        String token = jwt.generateAccessToken(admin());

        assertTrue(other.parseAccessToken(token).isEmpty());
        assertTrue(jwt.parseAccessToken(token + "x").isEmpty());
        assertTrue(jwt.parseAccessToken("khong-phai-jwt").isEmpty());
    }

    @Test
    void tokenHetHan_biTuChoi() {
        JwtService jwt = service("test-secret-key-0123456789-0123456789-abcdef", -60);
        String token = jwt.generateAccessToken(admin());
        assertTrue(jwt.parseAccessToken(token).isEmpty());
    }

    @Test
    void khoaQuaNgan_baoLoiKhiKhoiDong() {
        assertThrows(IllegalStateException.class, () -> service("ngan", 900));
    }
}
