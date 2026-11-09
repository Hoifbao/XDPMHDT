package com.smartheritage.common.events;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.lang.Nullable;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * Gửi sự kiện lên exchange sh.events. Có 2 cách:
 *
 * 1. publishAfterCommit: gửi SAU KHI giao dịch DB commit, rollback thì không gửi.
 *    Lỗi RabbitMQ chỉ ghi log + metric failed, API vẫn thành công (sự kiện có thể mất).
 *    Hợp với feedback-service, personalization...
 *
 * 2. sendConfirmed: gửi ngay và CHỜ RabbitMQ xác nhận; lỗi thì ném EventPublishException.
 *    Dùng cho bảng outbox (content-service): chỉ đánh dấu đã gửi khi hàm này không ném lỗi.
 *    Cần bật trong application.yml của service:
 *      spring.rabbitmq.publisher-confirm-type: correlated
 *      spring.rabbitmq.publisher-returns: true
 *      spring.rabbitmq.template.mandatory: true
 *
 * Metric: sh_events_published_total{type, result="success"|"failed"}.
 */
public class EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    @Nullable
    private final MeterRegistry meterRegistry;

    public EventPublisher(RabbitTemplate rabbitTemplate, @Nullable MeterRegistry meterRegistry) {
        this.rabbitTemplate = rabbitTemplate;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Gửi sau commit; không ở trong giao dịch thì gửi ngay.
     * eventId và occurredAt lấy lúc gọi hàm (lúc nghiệp vụ xảy ra); data chỉ được tạo lúc gửi,
     * nên có thể gọi service khác trong Supplier mà không giữ giao dịch DB.
     *
     * @return eventId để ghi log hoặc trả về cho client nếu cần
     */
    public UUID publishAfterCommit(String routingKey, String type, Supplier<?> data) {
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.now();
        Runnable send = () -> sendQuietly(routingKey, type, eventId, occurredAt, data);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            send.run();
        } else {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        }
        return eventId;
    }

    /** Như trên, với data đã có sẵn. */
    public UUID publishAfterCommit(String routingKey, String type, Object data) {
        return publishAfterCommit(routingKey, type, () -> data);
    }

    /**
     * Gửi ngay và chờ RabbitMQ xác nhận (publisher confirm). Không ack, không có queue nào nhận
     * (returned) hoặc quá timeout thì ném EventPublishException để bên gọi giữ lại và gửi lại sau.
     * Gửi lại cùng eventId để bên nhận bỏ trùng được.
     */
    public void sendConfirmed(String routingKey, EventEnvelope<?> event, Duration timeout) {
        CorrelationData correlation = new CorrelationData(event.eventId().toString());
        try {
            rabbitTemplate.convertAndSend(Events.EXCHANGE, routingKey, event, correlation);
            CorrelationData.Confirm confirm = correlation.getFuture()
                    .get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!confirm.isAck()) {
                throw new EventPublishException("RabbitMQ từ chối sự kiện " + describe(event)
                        + ": " + confirm.getReason());
            }
            if (correlation.getReturned() != null) {
                throw new EventPublishException("Chưa có queue nào nhận routing key " + routingKey
                        + " cho sự kiện " + describe(event));
            }
            count(event.type(), Events.RESULT_SUCCESS);
            log.info("Đã gửi (có xác nhận) sự kiện {}", describe(event));
        } catch (EventPublishException e) {
            count(event.type(), Events.RESULT_FAILED);
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            count(event.type(), Events.RESULT_FAILED);
            throw new EventPublishException("Bị ngắt khi chờ xác nhận sự kiện " + describe(event), e);
        } catch (ExecutionException | TimeoutException | RuntimeException e) {
            count(event.type(), Events.RESULT_FAILED);
            throw new EventPublishException("Không gửi được sự kiện " + describe(event), e);
        }
    }

    /** Bắt mọi RuntimeException (kể cả lỗi trong Supplier): dữ liệu đã commit, không được làm API thất bại. */
    private void sendQuietly(String routingKey, String type, UUID eventId, Instant occurredAt, Supplier<?> data) {
        try {
            EventEnvelope<?> event = EventEnvelope.of(eventId, type, occurredAt, data.get());
            rabbitTemplate.convertAndSend(Events.EXCHANGE, routingKey, event);
            count(type, Events.RESULT_SUCCESS);
            log.info("Đã gửi sự kiện {} eventId={}", type, eventId);
        } catch (RuntimeException e) {
            count(type, Events.RESULT_FAILED);
            log.warn("Không gửi được sự kiện {} eventId={}: {}", type, eventId, e.getMessage());
        }
    }

    private void count(String type, String result) {
        if (meterRegistry != null) {
            meterRegistry.counter(Events.METRIC_PUBLISHED, "type", type, "result", result).increment();
        }
    }

    private static String describe(EventEnvelope<?> event) {
        return event.type() + " eventId=" + event.eventId();
    }
}
