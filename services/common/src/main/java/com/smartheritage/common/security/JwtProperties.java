package com.smartheritage.common.security;

/**
 * Cấu hình JWT. Mỗi service gắn với khóa app.jwt.* trong application.yml bằng:
 * <pre>
 * &#64;Bean &#64;ConfigurationProperties(prefix = "app.jwt")
 * JwtProperties jwtProperties() { return new JwtProperties(); }
 * </pre>
 * Gateway, auth-service và Backend cũ PHẢI dùng chung app.jwt.secret.
 */
public class JwtProperties {
    /** Khóa ký HMAC-SHA256, tối thiểu 32 ký tự. */
    private String secret;
    /** Thời gian sống của access token (giây). */
    private long accessTokenTtlSeconds = 900;
    /** Thời gian sống của refresh token (giây). */
    private long refreshTokenTtlSeconds = 604800;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getAccessTokenTtlSeconds() {
        return accessTokenTtlSeconds;
    }

    public void setAccessTokenTtlSeconds(long accessTokenTtlSeconds) {
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
    }

    public long getRefreshTokenTtlSeconds() {
        return refreshTokenTtlSeconds;
    }

    public void setRefreshTokenTtlSeconds(long refreshTokenTtlSeconds) {
        this.refreshTokenTtlSeconds = refreshTokenTtlSeconds;
    }
}
