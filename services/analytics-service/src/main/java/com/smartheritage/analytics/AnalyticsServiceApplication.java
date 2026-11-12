package com.smartheritage.analytics;

import com.smartheritage.common.events.EventsConfig;
import com.smartheritage.common.web.GlobalExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * analytics-service (Tuấn - Sprint 5): số liệu Dashboard Thống kê.
 * Cổng 8087, chỉ đọc analytics_db (NiFi đồng bộ từ DB của các service khác sang).
 * Tuần 6: nhận sự kiện RabbitMQ để đếm số liệu thời gian thực trong ngày (bảng rt_*).
 */
@SpringBootApplication
@Import({GlobalExceptionHandler.class, EventsConfig.class})
public class AnalyticsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnalyticsServiceApplication.class, args);
    }
}
