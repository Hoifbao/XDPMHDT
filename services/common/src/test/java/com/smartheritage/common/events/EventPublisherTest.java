package com.smartheritage.common.events;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.net.ConnectException;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** TransactionTemplate thật với transaction manager giả để kiểm tra đúng thời điểm gửi. */
class EventPublisherTest {

    record SampleData(Long id, String name) {
    }

    private RabbitTemplate rabbitTemplate;
    private SimpleMeterRegistry meterRegistry;
    private EventPublisher publisher;
    private TransactionTemplate transaction;

    @BeforeEach
    void setUp() {
        rabbitTemplate = mock(RabbitTemplate.class);
        meterRegistry = new SimpleMeterRegistry();
        publisher = new EventPublisher(rabbitTemplate, meterRegistry);
        transaction = new TransactionTemplate(new FakeTransactionManager());
    }

    // ──────────── publishAfterCommit ────────────

    @Test
    void publishAfterCommit_chuaCommitChuaGui_commitXongGui1LanDungEnvelope() {
        UUID[] eventId = new UUID[1];
        transaction.executeWithoutResult(status -> {
            eventId[0] = publisher.publishAfterCommit("sample.created", "SampleCreated", new SampleData(1L, "A"));
            verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
        });

        EventEnvelope<?> event = sent("sample.created");
        assertThat(event.eventId()).isEqualTo(eventId[0]);
        assertThat(event.type()).isEqualTo("SampleCreated");
        assertThat(event.version()).isEqualTo(1);
        assertThat(event.occurredAt()).isNotNull();
        assertThat(event.data()).isEqualTo(new SampleData(1L, "A"));
        assertThat(count("SampleCreated", "success")).isEqualTo(1.0);
    }

    @Test
    void publishAfterCommit_rollback_khongGuiVaKhongGoiSupplier() {
        AtomicInteger calls = new AtomicInteger();
        transaction.executeWithoutResult(status -> {
            publisher.publishAfterCommit("sample.created", "SampleCreated", () -> {
                calls.incrementAndGet();
                return new SampleData(1L, "A");
            });
            status.setRollbackOnly();
        });

        assertThat(calls).hasValue(0);
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void publishAfterCommit_supplierChiGoiSauCommit() {
        AtomicInteger calls = new AtomicInteger();
        transaction.executeWithoutResult(status -> {
            publisher.publishAfterCommit("sample.created", "SampleCreated", () -> {
                calls.incrementAndGet();
                return new SampleData(1L, "A");
            });
            assertThat(calls).hasValue(0);
        });

        assertThat(calls).hasValue(1);
    }

    @Test
    void publishAfterCommit_khongCoGiaoDich_guiNgay() {
        publisher.publishAfterCommit("sample.created", "SampleCreated", new SampleData(1L, "A"));

        sent("sample.created");
    }

    @Test
    void publishAfterCommit_rabbitLoi_khongNemLoiVaTangFailed() {
        doThrow(new AmqpConnectException(new ConnectException("refused")))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        assertThatCode(() -> transaction.executeWithoutResult(status ->
                publisher.publishAfterCommit("sample.created", "SampleCreated", new SampleData(1L, "A"))))
                .doesNotThrowAnyException();
        assertThat(count("SampleCreated", "failed")).isEqualTo(1.0);
        assertThat(count("SampleCreated", "success")).isZero();
    }

    @Test
    void publishAfterCommit_supplierLoi_khongNemLoiVaTangFailed() {
        assertThatCode(() -> transaction.executeWithoutResult(status ->
                publisher.publishAfterCommit("sample.created", "SampleCreated", () -> {
                    throw new IllegalStateException("service khác lỗi");
                })))
                .doesNotThrowAnyException();
        assertThat(count("SampleCreated", "failed")).isEqualTo(1.0);
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    void khongCoMeterRegistry_vanGuiDuoc() {
        EventPublisher noMetrics = new EventPublisher(rabbitTemplate, null);

        noMetrics.publishAfterCommit("sample.created", "SampleCreated", new SampleData(1L, "A"));

        sent("sample.created");
    }

    // ──────────── sendConfirmed ────────────

    @Test
    void sendConfirmed_ack_thanhCongVaGiuNguyenEventId() {
        EventEnvelope<SampleData> event = EventEnvelope.of("SampleUpdated", new SampleData(2L, "B"));
        answerConfirm(true, null, false);

        publisher.sendConfirmed("sample.updated", event, Duration.ofSeconds(1));

        ArgumentCaptor<CorrelationData> correlation = ArgumentCaptor.forClass(CorrelationData.class);
        verify(rabbitTemplate).convertAndSend(eq(Events.EXCHANGE), eq("sample.updated"), eq(event), correlation.capture());
        assertThat(correlation.getValue().getId()).isEqualTo(event.eventId().toString());
        assertThat(count("SampleUpdated", "success")).isEqualTo(1.0);
    }

    @Test
    void sendConfirmed_nack_nemLoi() {
        answerConfirm(false, "queue full", false);

        assertThatThrownBy(() -> publisher.sendConfirmed("sample.updated",
                EventEnvelope.of("SampleUpdated", new SampleData(2L, "B")), Duration.ofSeconds(1)))
                .isInstanceOf(EventPublishException.class)
                .hasMessageContaining("queue full");
        assertThat(count("SampleUpdated", "failed")).isEqualTo(1.0);
    }

    @Test
    void sendConfirmed_khongCoQueueNhan_nemLoi() {
        answerConfirm(true, null, true);

        assertThatThrownBy(() -> publisher.sendConfirmed("sample.updated",
                EventEnvelope.of("SampleUpdated", new SampleData(2L, "B")), Duration.ofSeconds(1)))
                .isInstanceOf(EventPublishException.class)
                .hasMessageContaining("Chưa có queue");
    }

    @Test
    void sendConfirmed_khongCoXacNhan_hetThoiGianThiNemLoi() {
        // Không hoàn tất future: giống khi chưa bật publisher-confirm-type
        assertThatThrownBy(() -> publisher.sendConfirmed("sample.updated",
                EventEnvelope.of("SampleUpdated", new SampleData(2L, "B")), Duration.ofMillis(50)))
                .isInstanceOf(EventPublishException.class);
        assertThat(count("SampleUpdated", "failed")).isEqualTo(1.0);
    }

    @Test
    void sendConfirmed_rabbitLoiKetNoi_nemLoi() {
        doThrow(new AmqpConnectException(new ConnectException("refused")))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class), any(CorrelationData.class));

        assertThatThrownBy(() -> publisher.sendConfirmed("sample.updated",
                EventEnvelope.of("SampleUpdated", new SampleData(2L, "B")), Duration.ofSeconds(1)))
                .isInstanceOf(EventPublishException.class);
    }

    // ──────────── helpers ────────────

    private void answerConfirm(boolean ack, String reason, boolean returned) {
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            if (returned) {
                correlation.setReturned(new ReturnedMessage(new Message(new byte[0], new MessageProperties()),
                        312, "NO_ROUTE", Events.EXCHANGE, "sample.updated"));
            }
            correlation.getFuture().complete(new CorrelationData.Confirm(ack, reason));
            return null;
        }).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class), any(CorrelationData.class));
    }

    private EventEnvelope<?> sent(String routingKey) {
        ArgumentCaptor<Object> message = ArgumentCaptor.forClass(Object.class);
        verify(rabbitTemplate).convertAndSend(eq(Events.EXCHANGE), eq(routingKey), message.capture());
        assertThat(message.getValue()).isInstanceOf(EventEnvelope.class);
        return (EventEnvelope<?>) message.getValue();
    }

    private double count(String type, String result) {
        Counter counter = meterRegistry.find(Events.METRIC_PUBLISHED).tag("type", type).tag("result", result).counter();
        return counter == null ? 0.0 : counter.count();
    }

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
