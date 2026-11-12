package com.smartheritage.beacon.config;

import com.smartheritage.common.events.EventsConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Nạp cấu hình sự kiện RabbitMQ dùng chung (exchange sh.events, MessageConverter JSON).
 *
 * Đặt @Import ở lớp riêng thay vì trên BeaconServiceApplication (giống feedback-service):
 * @WebMvcTest luôn nạp lớp Application nhưng không có RabbitTemplate, nên nếu import ở đó
 * thì test lớp web không khởi động được. Lớp @Configuration này chỉ được nạp khi chạy
 * service thật hoặc @SpringBootTest.
 */
@Configuration
@Import(EventsConfig.class)
public class EventsImportConfig {
}
