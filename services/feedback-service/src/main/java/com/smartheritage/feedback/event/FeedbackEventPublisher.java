package com.smartheritage.feedback.event;

import com.smartheritage.feedback.entity.Feedback;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Gửi sự kiện đánh giá lên exchange sh.events, chỉ SAU KHI giao dịch DB đã lưu thành công.
 * Giao dịch bị rollback thì không gửi gì.
 *
 * Gửi lỗi chỉ ghi log kèm eventId: đánh giá đã lưu nên API vẫn báo thành công.
 * Không có publisher confirm/outbox: RabbitMQ chết đúng lúc gửi thì sự kiện bị mất (xem README).
 *
 * Metric cho Prometheus: feedback_events_published_total{type, result="success"|"failed"}.
 *
 * Khi thư viện sự kiện của module common có trên main: giữ 2 hàm public, thay envelope và phần gửi
 * bằng hàm của thư viện, rồi xóa FeedbackEventEnvelope. FeedbackService không phải sửa.
 */
@Component
public class FeedbackEventPublisher {

    static final String TYPE_CREATED = "FeedbackCreated";
    static final String TYPE_APPROVED = "FeedbackApproved";
    static final String ROUTING_CREATED = "feedback.created";
    static final String ROUTING_APPROVED = "feedback.approved";

    private static final Logger log = LoggerFactory.getLogger(FeedbackEventPublisher.class);

    static final String METRIC_PUBLISHED = "feedback.events.published";
    static final String RESULT_SUCCESS = "success";
    static final String RESULT_FAILED = "failed";

    private final RabbitTemplate rabbitTemplate;
    private final MeterRegistry meterRegistry;

    public FeedbackEventPublisher(RabbitTemplate rabbitTemplate, MeterRegistry meterRegistry) {
        this.rabbitTemplate = rabbitTemplate;
        this.meterRegistry = meterRegistry;
        // Tạo sẵn counter = 0: alert dùng increase() mới bắt được cả lần lỗi đầu tiên
        for (String type : List.of(TYPE_CREATED, TYPE_APPROVED)) {
            for (String result : List.of(RESULT_SUCCESS, RESULT_FAILED)) {
                publishedCounter(type, result);
            }
        }
    }

    public void publishCreated(Feedback feedback) {
        FeedbackEventData.Created data = new FeedbackEventData.Created(feedback.getId(), feedback.getUserId(),
                feedback.getArtifactId(), feedback.getRating(), feedback.getCreatedAt());
        runAfterCommit(() -> send(ROUTING_CREATED, TYPE_CREATED, () -> data));
    }

    /**
     * Tên hiện vật được lấy SAU khi commit (artifactName.get() gọi content-service),
     * để giao dịch DB không bị giữ trong lúc chờ service khác.
     */
    public void publishApproved(Feedback feedback, Supplier<String> artifactName) {
        Long feedbackId = feedback.getId();
        Long userId = feedback.getUserId();
        Long artifactId = feedback.getArtifactId();
        runAfterCommit(() -> send(ROUTING_APPROVED, TYPE_APPROVED,
                () -> new FeedbackEventData.Approved(feedbackId, userId, artifactId, artifactName.get())));
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

    /** Bắt mọi RuntimeException: dữ liệu đã commit, lỗi lúc này không được làm API báo thất bại. */
    private void send(String routingKey, String type, Supplier<?> data) {
        UUID eventId = UUID.randomUUID();
        try {
            rabbitTemplate.convertAndSend(RabbitEventConfig.EXCHANGE, routingKey,
                    FeedbackEventEnvelope.of(eventId, type, data.get()));
            publishedCounter(type, RESULT_SUCCESS).increment();
            log.info("Đã gửi sự kiện {} eventId={}", type, eventId);
        } catch (RuntimeException e) {
            publishedCounter(type, RESULT_FAILED).increment();
            log.warn("Không gửi được sự kiện {} eventId={}: {}", type, eventId, e.getMessage());
        }
    }

    private Counter publishedCounter(String type, String result) {
        return meterRegistry.counter(METRIC_PUBLISHED, "type", type, "result", result);
    }
}
