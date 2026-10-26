package com.smartheritage.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình thông tin hiển thị trên Swagger UI và tài liệu OpenAPI (springdoc).
 * Khai báo thông tin chung của API và nút "Authorize" (Bearer JWT - Bảo, Sprint 3):
 * đăng nhập qua POST /api/auth/login, copy accessToken, bấm Authorize rồi dán vào.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI smartHeritageOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Smart Heritage API")
                        .description("REST API của hệ thống du lịch di sản thông minh Smart Heritage "
                                + "sử dụng iBeacon, phục vụ ứng dụng di động của du khách "
                                + "và nền tảng quản trị Web Admin.")
                        .version("1.0.0"))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Dán accessToken lấy từ POST /api/auth/login (không cần gõ chữ Bearer)")))
                // Gửi token cho mọi API trên Swagger; API công khai sẽ bỏ qua token
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}