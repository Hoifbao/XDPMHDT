package com.smartheritage.auth;

import com.smartheritage.common.web.GlobalExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/** auth-service, cổng 8081. Client gọi qua Gateway: http://localhost:8080/api/auth/** */
@SpringBootApplication
@Import(GlobalExceptionHandler.class)
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
