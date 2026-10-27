package com.smartheritage.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * API Gateway, cổng 8080: Web Admin và Mobile chỉ gọi vào đây.
 * Kiểm tra JWT + phân quyền (JwtAuthGlobalFilter), rồi chuyển tiếp tới service (GatewayRoutesConfig).
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
