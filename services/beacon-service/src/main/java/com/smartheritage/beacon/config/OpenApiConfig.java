package com.smartheritage.beacon.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger của service: http://localhost:8084/swagger-ui.html */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI serviceOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Smart Heritage - beacon-service")
                .description("iBeacon CRUD, registry beacon ACTIVE cho Mobile quét BLE và gán hiện vật. "
                        + "Client gọi qua Gateway http://localhost:8080; Gateway kiểm tra quyền "
                        + "(GET công khai, thêm/sửa/xóa chỉ ADMIN). Gán hiện vật phải cùng di tích.")
                .version("1.0.0"))
                // Nút Authorize: dán accessToken từ POST /api/auth/login (Bearer JWT)
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP)
                                .scheme("bearer").bearerFormat("JWT")));
    }
}
