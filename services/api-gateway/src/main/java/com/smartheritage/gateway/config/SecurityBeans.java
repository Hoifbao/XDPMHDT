package com.smartheritage.gateway.config;

import com.smartheritage.common.security.JwtProperties;
import com.smartheritage.common.security.JwtService;
import com.smartheritage.gateway.security.AccessRules;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityBeans {

    /** app.jwt.* phải TRÙNG khóa với auth-service và Backend cũ. */
    @Bean
    @ConfigurationProperties(prefix = "app.jwt")
    public JwtProperties jwtProperties() {
        return new JwtProperties();
    }

    @Bean
    public JwtService jwtService(JwtProperties jwtProperties) {
        return new JwtService(jwtProperties);
    }

    @Bean
    public AccessRules accessRules() {
        return new AccessRules();
    }
}
