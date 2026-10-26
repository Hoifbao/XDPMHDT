package com.smartheritage.backend.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Đọc cấu hình app.jwt.* trong application.properties. */
@Component
@ConfigurationProperties(prefix = "app.jwt")
@Getter
@Setter
public class JwtProperties {
    /** Khóa ký HMAC-SHA256, tối thiểu 32 ký tự. */
    private String secret;
    /** Thời gian sống của access token (giây). */
    private long accessTokenTtlSeconds = 900;
    /** Thời gian sống của refresh token (giây). */
    private long refreshTokenTtlSeconds = 604800;
}
