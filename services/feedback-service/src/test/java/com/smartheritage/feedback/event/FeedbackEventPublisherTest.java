package com.smartheritage.feedback.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.smartheritage.feedback.entity.Feedback;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.ConnectException;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** Dùng TransactionTemplate thật với transaction manager giả để kiểm tra đúng thời điểm gửi. */
class FeedbackEventPublisherTest {

    private RabbitTemplate rabbitTemplate;
    private FeedbackEventPublisher publisher;
    private TransactionTemplate transaction;
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        rabbitTemplate = mock(RabbitTemplate.class);
        meterRegistry = new SimpleMeterRegistry();
        publisher = new FeedbackEventPublisher(rabbitTemplate, meterRegistry);
        transaction = new TransactionTemplate(new FakeTransactionManager());
    }

    @Test
    void publishCreated_sauCommit_gui1LanDungEnvelope() {
        transaction.executeWithoutResult(status -> {
            publisher.publishCreated(feedback());
            // Chưa commit thì chưa được gửi
            verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
        });

        FeedbackEventEnvelope<?> event = sentEvent("feedback.created");
        assertThat(event.eventId()).isNotNull();
        assertThat(event.type()).isEqualTo("FeedbackCreated");
        assertThat(event.version()).isEqualTo(1);
        assertThat(event.occurredAt()).isNotNull();
        assertThat(event.data()).isEqualTo(new FeedbackEventData.Created(7L, 2L, 1L, 4, CREATED_AT));
    }

    @Test
    void publishApproved_sauCommit_coUserIdVaTenHienVat() {
        transaction.executeWithoutResult(status -> publisher.publishApproved(feedback(), () -> "Khuê Văn Các"));

        FeedbackEventEnvelope<?> event = sentEvent("feedback.approved");
        assertThat(event.type()).isEqualTo("FeedbackApproved");
        assertThat(event.data()).isEqualTo(new FeedbackEventData.Approved(7L, 2L, 1L, "Khuê Văn Các"));
    }

    @Test
    void publishApproved_tenHienVatChiLaySauCommit() {
        AtomicInteger calls = new AtomicInteger();
        transaction.executeWithoutResult(status -> {
            publisher.publishApproved(feedback(), () -> {
                calls.incrementAndGet();
                return "Khuê Văn Các";
            });
            // Trong giao dịch chưa được gọi content-service
            assertThat(calls).hasValue(0);
        });

        assertThat(calls).hasValue(1);
    }

    @Test
    void publishApproved_rollback_khongLayTenVaKhongGui() {
        AtomicInteger calls = new AtomicInteger();
        transaction.executeWithoutResult(status -> {
            publisher.publishApproved(feedback(), () -> {
                calls.incrementAndGet();
                return "X";
            });
            status.setRollbackOnly();
        });

        assertThat(calls).hasValue(0);
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void giaoDichRollback_khongGuiSuKien() {
        transaction.executeWithoutResult(status -> {
            publisher.publishCreated(feedback());
            status.setRollbackOnly();
        });

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void rabbitMqLoi_khongNemLoiRaNgoai() {
        doThrow(new AmqpConnectException(new ConnectException("refused")))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        assertThatCode(() -> transaction.executeWithoutResult(status -> publisher.publishCreated(feedback())))
                .doesNotThrowAnyException();
        assertThat(publishedCount("FeedbackCreated", "failed")).isEqualTo(1.0);
        assertThat(publishedCount("FeedbackCreated", "success")).isZero();
    }

    @Test
    void guiThanhCong_tangMetricSuccess() {
        transaction.executeWithoutResult(status -> publisher.publishCreated(feedback()));

        assertThat(publishedCount("FeedbackCreated", "success")).isEqualTo(1.0);
    }

    private double publishedCount(String type, String result) {
        var counter = meterRegistry.find("feedback.events.published").tag("type", type).tag("result", result).counter();
        return counter == null ? 0.0 : counter.count();
    }

    @Test
    void envelope_raJsonDu5TruongVaNgayDangChuoi() {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        FeedbackEventEnvelope<FeedbackEventData.Created> event = FeedbackEventEnvelope.of("FeedbackCreated",
                new FeedbackEventData.Created(7L, 2L, 1L, 4, CREATED_AT));

        JsonNode json = mapper.valueToTree(event);

        assertThat(json.fieldNames()).toIterable()
                .containsExactlyInAnyOrder("eventId", "type", "version", "occurredAt", "data");
        assertThat(json.get("data").get("createdAt").asText()).isEqualTo("2026-11-09T08:30:00");
    }

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 11, 9, 8, 30);

    private static Feedback feedback() {
        return Feedback.builder().id(7L).userId(2L).artifactId(1L).rating(4).createdAt(CREATED_AT).build();
    }

    private FeedbackEventEnvelope<?> sentEvent(String routingKey) {
        ArgumentCaptor<Object> message = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(eq("sh.events"), eq(routingKey), message.capture());
        assertThat(message.getValue()).isInstanceOf(FeedbackEventEnvelope.class);
        return (FeedbackEventEnvelope<?>) message.getValue();
    }

    /** Transaction manager không có DB: chỉ để Spring chạy đúng vòng đời commit/rollback. */
    private static final class FakeTransactionManager extends AbstractPlatformTransactionManager {
        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }
}
