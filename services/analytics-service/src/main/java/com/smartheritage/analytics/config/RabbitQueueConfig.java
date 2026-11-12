package com.smartheritage.analytics.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Queue riêng của analytics-service trên exchange sh.events (Tuần 6, Tuấn).
 * Mỗi loại sự kiện một queue vì kiểu data khác nhau. Tin xử lý lỗi quá 3 lần
 * (retry trong application.yml) hoặc JSON hỏng thì vào analytics.dlq, không bị mất.
 */
@Configuration
public class RabbitQueueConfig {

    public static final String VISIT_RECORDED_QUEUE = "analytics.visit-recorded";
    public static final String FEEDBACK_CREATED_QUEUE = "analytics.feedback-created";

    static final String DEAD_LETTER_EXCHANGE = "analytics.dlx";
    static final String DEAD_LETTER_QUEUE = "analytics.dlq";

    @Bean
    public FanoutExchange analyticsDeadLetterExchange() {
        return new FanoutExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    public Queue analyticsDeadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding analyticsDeadLetterBinding() {
        return BindingBuilder.bind(analyticsDeadLetterQueue()).to(analyticsDeadLetterExchange());
    }

    @Bean
    public Queue visitRecordedQueue() {
        return QueueBuilder.durable(VISIT_RECORDED_QUEUE).deadLetterExchange(DEAD_LETTER_EXCHANGE).build();
    }

    @Bean
    public Queue feedbackCreatedQueue() {
        return QueueBuilder.durable(FEEDBACK_CREATED_QUEUE).deadLetterExchange(DEAD_LETTER_EXCHANGE).build();
    }

    @Bean
    public Binding visitRecordedBinding(TopicExchange shEventsExchange) {
        return BindingBuilder.bind(visitRecordedQueue()).to(shEventsExchange).with("visit.recorded");
    }

    @Bean
    public Binding feedbackCreatedBinding(TopicExchange shEventsExchange) {
        return BindingBuilder.bind(feedbackCreatedQueue()).to(shEventsExchange).with("feedback.created");
    }
}
