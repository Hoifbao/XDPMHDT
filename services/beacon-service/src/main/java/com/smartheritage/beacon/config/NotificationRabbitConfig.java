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

    // W6: beacon-service vừa GỬI vừa NHẬN beacon.battery.low (gửi từ BatteryAlertService)
    public static final String QUEUE_BEACON_BATTERY_LOW = "notification.beacon-battery-low";
    public static final String QUEUE_BEACON_BATTERY_LOW_DLQ = "notification.beacon-battery-low.dlq";
    public static final String ROUTING_KEY_BEACON_BATTERY_LOW = "beacon.battery.low";

    // W6: content-service phát artifact.updated khi hiện vật đổi -> beacon hủy cache tên hiện vật
    public static final String QUEUE_REGISTRY_ARTIFACT_UPDATED = "registry.artifact-updated";
    public static final String QUEUE_REGISTRY_ARTIFACT_UPDATED_DLQ = "registry.artifact-updated.dlq";
    public static final String ROUTING_KEY_ARTIFACT_UPDATED = "artifact.updated";

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
    public Queue beaconBatteryLowQueue() {
        return org.springframework.amqp.core.QueueBuilder.durable(QUEUE_BEACON_BATTERY_LOW)
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey(QUEUE_BEACON_BATTERY_LOW_DLQ)
                .build();
    }

    @Bean
    public Binding beaconBatteryLowBinding(TopicExchange shEventsExchange) {
        return BindingBuilder.bind(beaconBatteryLowQueue()).to(shEventsExchange)
                .with(ROUTING_KEY_BEACON_BATTERY_LOW);
    }

    @Bean
    public Queue registryArtifactUpdatedQueue() {
        return org.springframework.amqp.core.QueueBuilder.durable(QUEUE_REGISTRY_ARTIFACT_UPDATED)
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey(QUEUE_REGISTRY_ARTIFACT_UPDATED_DLQ)
                .build();
    }

    @Bean
    public Binding registryArtifactUpdatedBinding(TopicExchange shEventsExchange) {
        return BindingBuilder.bind(registryArtifactUpdatedQueue()).to(shEventsExchange)
                .with(ROUTING_KEY_ARTIFACT_UPDATED);
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

    @Bean
    public Queue beaconBatteryLowDlq() {
        return org.springframework.amqp.core.QueueBuilder.durable(QUEUE_BEACON_BATTERY_LOW_DLQ).build();
    }

    @Bean
    public Binding beaconBatteryLowDlqBinding() {
        return BindingBuilder.bind(beaconBatteryLowDlq()).to(deadLetterExchange())
                .with(QUEUE_BEACON_BATTERY_LOW_DLQ);
    }

    @Bean
    public Queue registryArtifactUpdatedDlq() {
        return org.springframework.amqp.core.QueueBuilder.durable(QUEUE_REGISTRY_ARTIFACT_UPDATED_DLQ).build();
    }

    @Bean
    public Binding registryArtifactUpdatedDlqBinding() {
        return BindingBuilder.bind(registryArtifactUpdatedDlq()).to(deadLetterExchange())
                .with(QUEUE_REGISTRY_ARTIFACT_UPDATED_DLQ);
    }
}
