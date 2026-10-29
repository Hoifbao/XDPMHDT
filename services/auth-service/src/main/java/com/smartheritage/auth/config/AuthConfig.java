package com.smartheritage.auth.config;

import com.smartheritage.common.security.JwtProperties;
import com.smartheritage.common.security.JwtService;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AuthConfig {

    /** Đọc app.jwt.* trong application.yml (phải trùng khóa với Gateway và Backend cũ). */
    @Bean
    @ConfigurationProperties(prefix = "app.jwt")
    public JwtProperties jwtProperties() {
        return new JwtProperties();
    }

    @Bean
    public JwtService jwtService(JwtProperties jwtProperties) {
        return new JwtService(jwtProperties);
    }

    /** BCrypt giống Backend cũ, nên tài khoản có sẵn (admin / Test@1234) đăng nhập được. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
