package com.smartheritage.common.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Test JwtService không cần Spring hay MySQL: mvn -pl services/common test */
class JwtServiceTest {

    private static JwtService service(String secret, long ttl) {
        JwtProperties props = new JwtProperties();
        props.setSecret(secret);
        props.setAccessTokenTtlSeconds(ttl);
        return new JwtService(props);
    }

    @Test
    void tokenHopLe_docRaDungUserVaRole() {
        JwtService jwt = service("test-secret-key-0123456789-0123456789-abcdef", 900);
        String token = jwt.generateAccessToken(1L, "admin", Roles.ADMIN);

        AuthPrincipal p = jwt.parseAccessToken(token).orElseThrow();
        assertEquals(1L, p.userId());
        assertEquals("admin", p.username());
        assertTrue(p.isAdmin());
    }

    @Test
    void tokenBiSua_hoacKhacKhoa_biTuChoi() {
        JwtService jwt = service("test-secret-key-0123456789-0123456789-abcdef", 900);
        JwtService other = service("another-secret-key-0123456789-0123456789-xyz", 900);
        String token = jwt.generateAccessToken(1L, "admin", Roles.ADMIN);

        assertTrue(other.parseAccessToken(token).isEmpty());
        assertTrue(jwt.parseAccessToken(token + "x").isEmpty());
        assertTrue(jwt.parseAccessToken("khong-phai-jwt").isEmpty());
        assertTrue(jwt.parseAccessToken(null).isEmpty());
    }

    @Test
    void tokenHetHan_biTuChoi() {
        JwtService jwt = service("test-secret-key-0123456789-0123456789-abcdef", -60);
        String token = jwt.generateAccessToken(1L, "admin", Roles.ADMIN);
        assertTrue(jwt.parseAccessToken(token).isEmpty());
    }

    @Test
    void khoaQuaNgan_baoLoiKhiKhoiDong() {
        assertThrows(IllegalStateException.class, () -> service("ngan", 900));
    }
}
