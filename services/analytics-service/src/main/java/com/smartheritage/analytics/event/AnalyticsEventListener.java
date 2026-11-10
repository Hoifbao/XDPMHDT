package com.smartheritage.analytics.event;

import com.smartheritage.analytics.config.RabbitQueueConfig;
import com.smartheritage.analytics.repository.RealtimeCounterRepository;
import com.smartheritage.common.events.EventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Nhận sự kiện từ sh.events và cộng vào rt_daily_counters (Tuần 6, Tuấn).
 * Mỗi tin chạy trong 1 transaction: ghi eventId + cộng số đếm cùng commit hoặc cùng rollback,
 * nên RabbitMQ giao lại tin (lỗi giữa chừng, retry) cũng không đếm sai.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyticsEventListener {

    /** Chỉ các trường dùng tới; trường thừa trong JSON bị bỏ qua. Không import class của service khác. */
    public record VisitRecordedData(Long visitId, Long artifactId, LocalDateTime detectedAt) {
    }

    public record FeedbackCreatedData(Long feedbackId, Long artifactId, Integer rating, LocalDateTime createdAt) {
    }

    private final RealtimeCounterRepository counterRepository;

    @Transactional
    @RabbitListener(queues = RabbitQueueConfig.VISIT_RECORDED_QUEUE)
    public void onVisitRecorded(EventEnvelope<VisitRecordedData> event) {
        VisitRecordedData data = requireData(event);
        if (data.artifactId() == null || data.detectedAt() == null) {
            throw reject(event, "thiếu artifactId hoặc detectedAt");
        }
        if (!counterRepository.markProcessed(event.eventId(), event.type())) {
            log.info("Bỏ qua sự kiện trùng eventId={}", event.eventId());
            return;
        }
        counterRepository.addView(data.detectedAt().toLocalDate(), data.artifactId());
    }

    @Transactional
    @RabbitListener(queues = RabbitQueueConfig.FEEDBACK_CREATED_QUEUE)
    public void onFeedbackCreated(EventEnvelope<FeedbackCreatedData> event) {
        FeedbackCreatedData data = requireData(event);
        if (data.artifactId() == null || data.createdAt() == null
                || data.rating() == null || data.rating() < 1 || data.rating() > 5) {
            throw reject(event, "thiếu artifactId/createdAt hoặc rating ngoài 1-5");
        }
        if (!counterRepository.markProcessed(event.eventId(), event.type())) {
            log.info("Bỏ qua sự kiện trùng eventId={}", event.eventId());
            return;
        }
        counterRepository.addFeedback(data.createdAt().toLocalDate(), data.artifactId(), data.rating());
    }

    private static <T> T requireData(EventEnvelope<T> event) {
        if (event == null || event.eventId() == null || event.data() == null) {
            throw reject(event, "thiếu eventId hoặc data");
        }
        return event.data();
    }

    // Dữ liệu sai thì retry cũng không khỏi: đưa thẳng vào analytics.dlq
    private static AmqpRejectAndDontRequeueException reject(EventEnvelope<?> event, String reason) {
        String eventId = event == null ? null : String.valueOf(event.eventId());
        return new AmqpRejectAndDontRequeueException("Sự kiện không hợp lệ eventId=" + eventId + ": " + reason);
    }
}
