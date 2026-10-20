package com.smartheritage.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình thông tin hiển thị trên Swagger UI và tài liệu OpenAPI (springdoc).
 * Chỉ khai báo thông tin chung của API; không ảnh hưởng đến Spring Security
 * hay các endpoint nghiệp vụ.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI smartHeritageOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Smart Heritage API")
                        .description("REST API của hệ thống du lịch di sản thông minh Smart Heritage "
                                + "sử dụng iBeacon, phục vụ ứng dụng di động của du khách "
                                + "và nền tảng quản trị Web Admin.")
                        .version("1.0.0"));
    }
}