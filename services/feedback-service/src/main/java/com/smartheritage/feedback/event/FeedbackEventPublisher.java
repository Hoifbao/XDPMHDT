package com.smartheritage.feedback.event;

import com.smartheritage.feedback.entity.Feedback;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Gửi sự kiện đánh giá lên exchange sh.events, chỉ SAU KHI giao dịch DB đã lưu thành công.
 * Giao dịch bị rollback thì không gửi gì.
 *
 * RabbitMQ lỗi chỉ ghi log kèm eventId: đánh giá đã lưu nên API vẫn báo thành công.
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

    private final RabbitTemplate rabbitTemplate;

    public void publishCreated(Feedback feedback) {
        FeedbackEventData.Created data = new FeedbackEventData.Created(feedback.getId(), feedback.getUserId(),
                feedback.getArtifactId(), feedback.getRating(), feedback.getCreatedAt());
        sendAfterCommit(ROUTING_CREATED, FeedbackEventEnvelope.of(TYPE_CREATED, data));
    }

    public void publishApproved(Feedback feedback, String artifactName) {
        FeedbackEventData.Approved data = new FeedbackEventData.Approved(feedback.getId(), feedback.getUserId(),
                feedback.getArtifactId(), artifactName);
        sendAfterCommit(ROUTING_APPROVED, FeedbackEventEnvelope.of(TYPE_APPROVED, data));
    }

    private void sendAfterCommit(String routingKey, FeedbackEventEnvelope<?> event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            send(routingKey, event);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                send(routingKey, event);
            }
        });
    }

    private void send(String routingKey, FeedbackEventEnvelope<?> event) {
        try {
            rabbitTemplate.convertAndSend(RabbitEventConfig.EXCHANGE, routingKey, event);
            log.info("Đã gửi sự kiện {} eventId={}", event.type(), event.eventId());
        } catch (AmqpException e) {
            log.warn("Không gửi được sự kiện {} eventId={}: {}", event.type(), event.eventId(), e.getMessage());
        }
    }
}
