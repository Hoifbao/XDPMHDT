package com.smartheritage.feedback;

import com.smartheritage.common.web.GlobalExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * feedback-service (Tường - Sprint 4): đánh giá và phản hồi của du khách cho hiện vật.
 * Cổng 8086, schema feedback_db. Tuần 5 mới route qua Gateway.
 */
@SpringBootApplication
@Import(GlobalExceptionHandler.class)
public class FeedbackServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FeedbackServiceApplication.class, args);
    }
}
