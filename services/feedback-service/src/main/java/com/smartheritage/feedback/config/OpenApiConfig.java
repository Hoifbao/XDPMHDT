package com.smartheritage.feedback.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger của service: http://localhost:8086/swagger-ui.html */
@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI serviceOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Smart Heritage - feedback-service")
                .description("Đánh giá (số sao + bình luận) của du khách, kiểm duyệt của ADMIN, điểm trung bình. "
                        + "Client gọi qua Gateway http://localhost:8080 (từ tuần 5). "
                        + "API cần đăng nhập đọc người dùng từ header X-User-Id, X-User-Role do Gateway gắn; "
                        + "khi test thẳng cổng 8086 thì tự thêm 2 header này.")
                .version("1.0.0"))
                // Nút Authorize (dùng chung ở Swagger tại Gateway): dán accessToken từ POST /api/auth/login
                .components(new Components().addSecuritySchemes(BEARER,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP)
                                .scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
