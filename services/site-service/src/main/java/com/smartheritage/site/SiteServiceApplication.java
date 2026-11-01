package com.smartheritage.site;

import com.smartheritage.common.web.GlobalExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * site-service (Ân - Sprint 4): di tích, bản đồ và API tìm di tích gần (Haversine). Cổng 8082, schema site_db.
 * Giữ @Import(GlobalExceptionHandler.class) để lỗi trả JSON thống nhất {success:false, message}.
 */
@SpringBootApplication
@Import(GlobalExceptionHandler.class)
public class SiteServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SiteServiceApplication.class, args);
    }
}
