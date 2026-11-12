package com.smartheritage.beacon.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Sự kiện đã xử lý (bảng processed_events - Khoa, W6).
 *
 * Chống nhận trùng: RabbitMQ có thể giao một tin nhiều lần ("at least once"),
 * beacon-service lưu eventId đã xử lý; gặp lại thì bỏ qua để du khách
 * không nhận hai thông báo cho cùng một đánh giá (services/feedback-service/README.md).
 */
@Entity
@Table(name = "processed_events",
        uniqueConstraints = @UniqueConstraint(name = "uq_processed_events_event_id", columnNames = "event_id"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** eventId của EventEnvelope (UUID) - duy nhất; CHAR để tra cứu được trên SQL, không nhị phân */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "event_id", nullable = false, columnDefinition = "CHAR(36)")
    private UUID eventId;

    /** Loại sự kiện, ví dụ feedback.approved - chỉ để tra cứu, không tham gia chống trùng */
    @Column(nullable = false, length = 100)
    private String eventType;

    @Column(name = "processed_at", updatable = false)
    @Builder.Default
    private LocalDateTime processedAt = LocalDateTime.now();
}
