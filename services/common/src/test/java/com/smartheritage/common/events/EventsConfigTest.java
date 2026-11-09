package com.smartheritage.common.events;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Kiểm tra định dạng JSON và việc bên nhận đọc được bằng record của chính mình. */
class EventsConfigTest {

    /** Data phía gửi (ví dụ feedback-service). */
    record SenderData(Long feedbackId, Long userId, LocalDateTime createdAt) {
    }

    /** Data phía nhận (ví dụ analytics-service): class khác, chỉ lấy trường cần dùng. */
    record ReceiverData(Long feedbackId) {
    }

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    private final MessageConverter converter = new EventsConfig().shEventsMessageConverter(objectMapper);

    private static final UUID EVENT_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final Instant OCCURRED_AT = Instant.parse("2026-11-10T01:00:00Z");

    @Test
    void guiDi_jsonDu5TruongNgayDangChuoi_khongCoTypeId() throws Exception {
        Message message = converter.toMessage(EventEnvelope.of(EVENT_ID, "FeedbackCreated", OCCURRED_AT,
                new SenderData(7L, 2L, LocalDateTime.of(2026, 11, 9, 8, 30))), new MessageProperties());

        JsonNode json = objectMapper.readTree(new String(message.getBody(), StandardCharsets.UTF_8));
        assertThat(json.fieldNames()).toIterable()
                .containsExactlyInAnyOrder("eventId", "type", "version", "occurredAt", "data");
        assertThat(json.get("occurredAt").asText()).isEqualTo("2026-11-10T01:00:00Z");
        assertThat(json.get("data").get("createdAt").asText()).isEqualTo("2026-11-09T08:30:00");
        assertThat(message.getMessageProperties().getHeaders()).doesNotContainKey("__TypeId__");
        assertThat(message.getMessageProperties().getContentType()).isEqualTo(MessageProperties.CONTENT_TYPE_JSON);
    }

    @Test
    void benNhan_docBangRecordRiengTheoKieuThamSoListener() {
        Message message = converter.toMessage(EventEnvelope.of(EVENT_ID, "FeedbackCreated", OCCURRED_AT,
                new SenderData(7L, 2L, LocalDateTime.of(2026, 11, 9, 8, 30))), new MessageProperties());
        // @RabbitListener void on(EventEnvelope<ReceiverData> e) -> Spring gắn kiểu suy ra như sau
        message.getMessageProperties().setInferredArgumentType(
                new TypeReference<EventEnvelope<ReceiverData>>() { }.getType());

        Object received = converter.fromMessage(message);

        assertThat(received).isInstanceOf(EventEnvelope.class);
        EventEnvelope<?> event = (EventEnvelope<?>) received;
        assertThat(event.eventId()).isEqualTo(EVENT_ID);
        assertThat(event.occurredAt()).isEqualTo(OCCURRED_AT);
        assertThat(event.data()).isEqualTo(new ReceiverData(7L));
    }

    @Test
    void exchangeLaTopicBenVungTenShEvents() {
        var exchange = new EventsConfig().shEventsExchange();

        assertThat(exchange.getName()).isEqualTo("sh.events");
        assertThat(exchange.getType()).isEqualTo("topic");
        assertThat(exchange.isDurable()).isTrue();
        assertThat(exchange.isAutoDelete()).isFalse();
    }
}
