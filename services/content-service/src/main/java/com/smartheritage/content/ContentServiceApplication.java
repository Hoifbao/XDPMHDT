package com.smartheritage.content;

import com.smartheritage.common.web.GlobalExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * content-service (Xuân Anh - Sprint 4): hiện vật, nội dung đa ngôn ngữ, media.
 * Cổng 8083, schema content_db. Client gọi qua Gateway http://localhost:8080.
 */
@SpringBootApplication
@Import(GlobalExceptionHandler.class)
public class ContentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContentServiceApplication.class, args);
    }
}
