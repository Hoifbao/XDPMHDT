package com.smartheritage.feedback.event;

import com.smartheritage.common.events.EventPublisher;
import com.smartheritage.common.events.Events;
import com.smartheritage.feedback.entity.Feedback;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Supplier;

/**
 * Sự kiện đánh giá của feedback-service, gửi qua EventPublisher của thư viện chung (common/events):
 * gửi SAU KHI giao dịch DB commit, rollback thì không gửi; lỗi chỉ ghi log + metric failed, API vẫn thành công.
 *
 * Metric (do thư viện đếm): sh_events_published_total{type, result="success"|"failed"}.
 * Lớp này chỉ tạo sẵn counter = 0 cho 2 loại sự kiện của feedback, để alert dùng increase()
 * bắt được cả lần lỗi đầu tiên.
 */
@Component
public class FeedbackEventPublisher {

    static final String TYPE_CREATED = "FeedbackCreated";
    static final String TYPE_APPROVED = "FeedbackApproved";
    static final String ROUTING_CREATED = "feedback.created";
    static final String ROUTING_APPROVED = "feedback.approved";

    private final EventPublisher events;

    public FeedbackEventPublisher(EventPublisher events, MeterRegistry meterRegistry) {
        this.events = events;
        for (String type : List.of(TYPE_CREATED, TYPE_APPROVED)) {
            for (String result : List.of(Events.RESULT_SUCCESS, Events.RESULT_FAILED)) {
                meterRegistry.counter(Events.METRIC_PUBLISHED, "type", type, "result", result);
            }
        }
    }

    public void publishCreated(Feedback feedback) {
        FeedbackEventData.Created data = new FeedbackEventData.Created(feedback.getId(), feedback.getUserId(),
                feedback.getArtifactId(), feedback.getRating(), feedback.getCreatedAt());
        events.publishAfterCommit(ROUTING_CREATED, TYPE_CREATED, data);
    }

    /**
     * Tên hiện vật được lấy SAU khi commit (artifactName.get() gọi content-service),
     * để giao dịch DB không bị giữ trong lúc chờ service khác.
     */
    public void publishApproved(Feedback feedback, Supplier<String> artifactName) {
        Long feedbackId = feedback.getId();
        Long userId = feedback.getUserId();
        Long artifactId = feedback.getArtifactId();
        events.publishAfterCommit(ROUTING_APPROVED, TYPE_APPROVED,
                () -> new FeedbackEventData.Approved(feedbackId, userId, artifactId, artifactName.get()));
    }
}
