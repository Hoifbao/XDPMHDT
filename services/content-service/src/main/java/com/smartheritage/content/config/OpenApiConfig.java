package com.smartheritage.content.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger của service: http://localhost:8083/swagger-ui.html */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI serviceOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Smart Heritage - content-service")
                .description("Hiện vật, nội dung đa ngôn ngữ (?lang=vi|en có fallback) và media. "
                        + "Client gọi qua Gateway http://localhost:8080; Gateway kiểm tra quyền "
                        + "(GET công khai, thêm/sửa/xóa chỉ ADMIN).")
                .version("1.0.0"));
    }
}
