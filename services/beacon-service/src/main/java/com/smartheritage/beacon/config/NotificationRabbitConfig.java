package com.smartheritage.beacon.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Queue, dead-letter queue và binding của beacon-service - Khoa, W6.
 *
 * Bên nhận tự tạo queue riêng (theo services/feedback-service/README.md):
 *   - exchange sh.events (topic) do common/events khai báo khi EventsImportConfig được nạp.
 *   - queue notification.feedback-approved bind routing key feedback.approved.
 *   - hàng đợi lỗi: xử lý lỗi quá 3 lần (application.yml) -> sh.events.dlx
 *     -> notification.feedback-approved.dlq, không mất tin để đối chiếu sau.
 */
@Configuration
public class NotificationRabbitConfig {

    public static final String QUEUE_FEEDBACK_APPROVED = "notification.feedback-approved";
    public static final String QUEUE_FEEDBACK_APPROVED_DLQ = "notification.feedback-approved.dlq";
    public static final String DLX = "sh.events.dlx";
    public static final String ROUTING_KEY_FEEDBACK_APPROVED = "feedback.approved";

    @Bean
    public Queue feedbackApprovedQueue() {
        return org.springframework.amqp.core.QueueBuilder.durable(QUEUE_FEEDBACK_APPROVED)
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey(QUEUE_FEEDBACK_APPROVED_DLQ)
                .build();
    }

    @Bean
    public Binding feedbackApprovedBinding(TopicExchange shEventsExchange) {
        return BindingBuilder.bind(feedbackApprovedQueue()).to(shEventsExchange)
                .with(ROUTING_KEY_FEEDBACK_APPROVED);
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(DLX, true, false);
    }

    @Bean
    public Queue feedbackApprovedDlq() {
        return org.springframework.amqp.core.QueueBuilder.durable(QUEUE_FEEDBACK_APPROVED_DLQ).build();
    }

    @Bean
    public Binding feedbackApprovedDlqBinding() {
        return BindingBuilder.bind(feedbackApprovedDlq()).to(deadLetterExchange())
                .with(QUEUE_FEEDBACK_APPROVED_DLQ);
    }
}
