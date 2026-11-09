package com.smartheritage.common.events;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình sự kiện dùng chung. Service nào gửi/nhận sự kiện thì:
 *   1. Thêm spring-boot-starter-amqp vào pom của service (common chỉ khai báo optional).
 *   2. Thêm @Import(EventsConfig.class) vào lớp Application (giống GlobalExceptionHandler).
 *   3. Xóa cấu hình exchange/MessageConverter riêng của service (nếu có), vì 2 MessageConverter
 *      cùng lúc thì Spring Boot không gắn cái nào vào RabbitTemplate.
 *
 * Gateway và service không import lớp này thì không bị ảnh hưởng.
 */
@Configuration
public class EventsConfig {

    @Bean
    public TopicExchange shEventsExchange() {
        return new TopicExchange(Events.EXCHANGE, true, false);
    }

    /**
     * JSON bằng ObjectMapper của Spring (ngày giờ dạng chuỗi ISO). Không gắn header __TypeId__
     * (tên class của bên gửi), để bên nhận không phải có class đó: kiểu dữ liệu lấy từ tham số @RabbitListener.
     */
    @Bean
    public MessageConverter shEventsMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        converter.setJavaTypeMapper(new NoTypeIdJavaTypeMapper());
        return converter;
    }

    @Bean
    public EventPublisher eventPublisher(RabbitTemplate rabbitTemplate, ObjectProvider<MeterRegistry> meterRegistry) {
        return new EventPublisher(rabbitTemplate, meterRegistry.getIfAvailable());
    }

    /** Khi gửi không ghi __TypeId__; khi nhận vẫn dùng kiểu suy ra từ @RabbitListener (mặc định INFERRED). */
    static class NoTypeIdJavaTypeMapper extends DefaultJackson2JavaTypeMapper {
        @Override
        public void fromJavaType(JavaType javaType, MessageProperties properties) {
            // cố ý để trống
        }
    }
}
