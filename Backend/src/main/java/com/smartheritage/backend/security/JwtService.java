package com.smartheritage.backend.security;

import com.smartheritage.backend.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Tạo và kiểm tra access token (JWT, ký HS256).
 * Payload: sub = username, uid = id người dùng, role = VISITOR | ADMIN, typ = access.
 * Refresh token KHÔNG phải JWT mà là chuỗi ngẫu nhiên lưu trong DB (xem RefreshTokenService),
 * để có thể thu hồi khi đăng xuất.
 */
@Service
public class JwtService {

    private static final String CLAIM_UID = "uid";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TYPE = "typ";
    private static final String TYPE_ACCESS = "access";
    private static final String ISSUER = "smart-heritage";

    private final SecretKey key;
    private final long accessTtlSeconds;

    public JwtService(JwtProperties props) {
        if (props.getSecret() == null || props.getSecret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("app.jwt.secret phải dài tối thiểu 32 ký tự");
        }
        this.key = Keys.hmacShaKeyFor(props.getSecret().getBytes(StandardCharsets.UTF_8));
        this.accessTtlSeconds = props.getAccessTokenTtlSeconds();
    }

    public long getAccessTtlSeconds() {
        return accessTtlSeconds;
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(ISSUER)
                .subject(user.getUsername())
                .claim(CLAIM_UID, user.getId())
                .claim(CLAIM_ROLE, user.getRole())
                .claim(CLAIM_TYPE, TYPE_ACCESS)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTtlSeconds)))
                .signWith(key)
                .compact();
    }

    /** Trả về người dùng nếu token hợp lệ (đúng chữ ký, chưa hết hạn, đúng loại); ngược lại rỗng. */
    public Optional<AuthPrincipal> parseAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(ISSUER)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))) {
                return Optional.empty();
            }
            Number uid = claims.get(CLAIM_UID, Number.class);
            String role = claims.get(CLAIM_ROLE, String.class);
            if (uid == null || role == null) {
                return Optional.empty();
            }
            return Optional.of(new AuthPrincipal(uid.longValue(), claims.getSubject(), role));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
