package com.smartheritage.feedback.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình gửi sự kiện: exchange kiểu topic sh.events và chuyển tin nhắn sang JSON.
 * Chỉ khai báo exchange; queue và dead-letter queue do từng bên nhận tự tạo.
 * Dùng ObjectMapper của Spring để ngày giờ ra chuỗi ISO, không phải dãy số.
 */
@Configuration
public class RabbitEventConfig {

    public static final String EXCHANGE = "sh.events";

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public MessageConverter eventMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
