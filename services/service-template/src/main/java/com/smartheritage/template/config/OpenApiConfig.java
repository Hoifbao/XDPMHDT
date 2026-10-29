package com.smartheritage.template.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger của service: http://localhost:&lt;cổng&gt;/swagger-ui.html. Đổi title/description khi copy. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI serviceOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Smart Heritage - service-template")
                .description("Mẫu service. Client gọi qua Gateway http://localhost:8080; "
                        + "API cần đăng nhập đọc người dùng từ header X-User-* do Gateway gắn.")
                .version("1.0.0"));
    }
}
