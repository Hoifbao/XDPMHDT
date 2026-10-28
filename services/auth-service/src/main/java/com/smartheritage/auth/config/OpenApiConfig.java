package com.smartheritage.auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger của auth-service: http://localhost:8081/swagger-ui.html */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI authServiceOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Smart Heritage - auth-service")
                .description("Đăng ký, đăng nhập, làm mới token, đăng xuất, hồ sơ người dùng. "
                        + "Client gọi qua Gateway http://localhost:8080. "
                        + "API /me và /users/{id} đọc người dùng từ header X-User-* do Gateway gắn, "
                        + "nên test 2 API này qua Gateway (gọi thẳng cổng 8081 sẽ trả 401).")
                .version("1.0.0"));
    }
}
