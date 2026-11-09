package com.smartheritage.feedback.event;

import com.smartheritage.feedback.entity.Feedback;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.function.Supplier;

/**
 * Gửi sự kiện đánh giá lên exchange sh.events, chỉ SAU KHI giao dịch DB đã lưu thành công.
 * Giao dịch bị rollback thì không gửi gì.
 *
 * RabbitMQ lỗi chỉ ghi log kèm eventId: đánh giá đã lưu nên API vẫn báo thành công.
 * Không có publisher confirm/outbox: RabbitMQ chết đúng lúc gửi thì sự kiện bị mất (xem README).
 *
 * Metric cho Prometheus: feedback_events_published_total{type, result="success"|"failed"}.
 *
 * Khi thư viện sự kiện của module common có trên main: giữ 2 hàm public, thay envelope và phần gửi
 * bằng hàm của thư viện, rồi xóa FeedbackEventEnvelope. FeedbackService không phải sửa.
 */
@Component
@RequiredArgsConstructor
public class FeedbackEventPublisher {

    static final String TYPE_CREATED = "FeedbackCreated";
    static final String TYPE_APPROVED = "FeedbackApproved";
    static final String ROUTING_CREATED = "feedback.created";
    static final String ROUTING_APPROVED = "feedback.approved";

    private static final Logger log = LoggerFactory.getLogger(FeedbackEventPublisher.class);

    static final String METRIC_PUBLISHED = "feedback.events.published";

    private final RabbitTemplate rabbitTemplate;
    private final MeterRegistry meterRegistry;

    public void publishCreated(Feedback feedback) {
        FeedbackEventData.Created data = new FeedbackEventData.Created(feedback.getId(), feedback.getUserId(),
                feedback.getArtifactId(), feedback.getRating(), feedback.getCreatedAt());
        FeedbackEventEnvelope<?> event = FeedbackEventEnvelope.of(TYPE_CREATED, data);
        runAfterCommit(() -> send(ROUTING_CREATED, event));
    }

    /**
     * Tên hiện vật được lấy SAU khi commit (artifactName.get() gọi content-service),
     * để giao dịch DB không bị giữ trong lúc chờ service khác.
     */
    public void publishApproved(Feedback feedback, Supplier<String> artifactName) {
        Long feedbackId = feedback.getId();
        Long userId = feedback.getUserId();
        Long artifactId = feedback.getArtifactId();
        runAfterCommit(() -> {
            FeedbackEventData.Approved data = new FeedbackEventData.Approved(feedbackId, userId, artifactId,
                    artifactName.get());
            send(ROUTING_APPROVED, FeedbackEventEnvelope.of(TYPE_APPROVED, data));
        });
    }

    private void runAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    private void send(String routingKey, FeedbackEventEnvelope<?> event) {
        try {
            rabbitTemplate.convertAndSend(RabbitEventConfig.EXCHANGE, routingKey, event);
            countPublished(event, "success");
            log.info("Đã gửi sự kiện {} eventId={}", event.type(), event.eventId());
        } catch (AmqpException e) {
            countPublished(event, "failed");
            log.warn("Không gửi được sự kiện {} eventId={}: {}", event.type(), event.eventId(), e.getMessage());
        }
    }

    private void countPublished(FeedbackEventEnvelope<?> event, String result) {
        meterRegistry.counter(METRIC_PUBLISHED, "type", event.type(), "result", result).increment();
    }
}
