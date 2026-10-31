package com.smartheritage.site.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger của service: http://localhost:8082/swagger-ui.html */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI serviceOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Smart Heritage - site-service")
                .description("Di tích và tìm di tích gần vị trí (Haversine). Client gọi qua Gateway "
                        + "http://localhost:8080; API cần đăng nhập đọc người dùng từ header X-User-* do Gateway gắn.")
                .version("1.0.0"));
    }
}
