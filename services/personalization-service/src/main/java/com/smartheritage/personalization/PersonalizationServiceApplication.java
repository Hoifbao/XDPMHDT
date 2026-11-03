package com.smartheritage.personalization;

import com.smartheritage.common.web.GlobalExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(GlobalExceptionHandler.class)
public class PersonalizationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PersonalizationServiceApplication.class, args);
    }
}
