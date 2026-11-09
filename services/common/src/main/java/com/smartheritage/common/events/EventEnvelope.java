package com.smartheritage.common.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Khung chung của mọi sự kiện: eventId, type, version, occurredAt, data.
 * JSON giống hệt FeedbackEventEnvelope (feedback-service) đang chạy trên main.
 *
 * Bên gửi: dùng EventPublisher, không tự tạo envelope.
 * Bên nhận: khai báo record data của service mình rồi nhận EventEnvelope<DataCuaMinh>,
 * dùng eventId để bỏ qua sự kiện đã xử lý (RabbitMQ có thể giao một tin nhiều lần).
 *
 * @param <T> phần data riêng của từng loại sự kiện
 */
public record EventEnvelope<T>(UUID eventId, String type, int version, Instant occurredAt, T data) {

    public static final int CURRENT_VERSION = 1;

    /** Sự kiện mới: eventId ngẫu nhiên, occurredAt = bây giờ. */
    public static <T> EventEnvelope<T> of(String type, T data) {
        return of(UUID.randomUUID(), type, Instant.now(), data);
    }

    /** Dùng khi eventId và thời điểm đã có sẵn, ví dụ đọc lại từ bảng outbox để gửi lại. */
    public static <T> EventEnvelope<T> of(UUID eventId, String type, Instant occurredAt, T data) {
        return new EventEnvelope<>(eventId, type, CURRENT_VERSION, occurredAt, data);
    }
}
