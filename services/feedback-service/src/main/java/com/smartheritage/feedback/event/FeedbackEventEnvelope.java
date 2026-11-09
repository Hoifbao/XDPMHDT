package com.smartheritage.feedback.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Khung chung của mọi sự kiện (quy ước tuần 6): eventId, type, version, occurredAt, data.
 * Bên nhận dùng eventId để bỏ qua sự kiện đã xử lý.
 *
 * Bản tạm trong feedback-service, JSON giống EventEnvelope của module common.
 * Khi thư viện chung có trên main thì xóa lớp này và dùng EventEnvelope (xem FeedbackEventPublisher).
 */
public record FeedbackEventEnvelope<T>(UUID eventId, String type, int version, Instant occurredAt, T data) {

    static final int CURRENT_VERSION = 1;

    static <T> FeedbackEventEnvelope<T> of(String type, T data) {
        return new FeedbackEventEnvelope<>(UUID.randomUUID(), type, CURRENT_VERSION, Instant.now(), data);
    }
}
