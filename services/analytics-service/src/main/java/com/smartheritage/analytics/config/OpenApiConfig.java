package com.smartheritage.analytics.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger của service: http://localhost:8087/swagger-ui.html */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI serviceOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Smart Heritage - analytics-service")
                .description("Thống kê Dashboard, đọc analytics_db do NiFi đồng bộ. "
                        + "Client gọi qua Gateway http://localhost:8080; Gateway chỉ cho ADMIN gọi /api/statistics/**.")
                .version("1.0.0"));
    }
}
