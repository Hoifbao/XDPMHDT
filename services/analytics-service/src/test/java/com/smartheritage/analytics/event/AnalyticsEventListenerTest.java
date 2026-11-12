package com.smartheritage.analytics.event;

import com.smartheritage.analytics.event.AnalyticsEventListener.FeedbackCreatedData;
import com.smartheritage.analytics.event.AnalyticsEventListener.VisitRecordedData;
import com.smartheritage.analytics.repository.RealtimeCounterRepository;
import com.smartheritage.common.events.EventEnvelope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsEventListenerTest {

    private static final LocalDateTime AT = LocalDateTime.of(2026, 11, 10, 23, 30);

    @Mock
    private RealtimeCounterRepository repository;

    @InjectMocks
    private AnalyticsEventListener listener;

    @Test
    void feedbackMoi_congVaoNgayCuaCreatedAt() {
        EventEnvelope<FeedbackCreatedData> event =
                EventEnvelope.of("FeedbackCreated", new FeedbackCreatedData(1L, 7L, 4, AT));
        when(repository.markProcessed(event.eventId(), "FeedbackCreated")).thenReturn(true);

        listener.onFeedbackCreated(event);

        verify(repository).addFeedback(LocalDate.of(2026, 11, 10), 7L, 4);
    }

    @Test
    void feedbackTrungEventId_khongCongLanHai() {
        EventEnvelope<FeedbackCreatedData> event =
                EventEnvelope.of("FeedbackCreated", new FeedbackCreatedData(1L, 7L, 4, AT));
        when(repository.markProcessed(event.eventId(), "FeedbackCreated")).thenReturn(false);

        listener.onFeedbackCreated(event);

        verify(repository, never()).addFeedback(any(), anyLong(), anyInt());
    }

    @Test
    void feedbackRatingSai_vaoDlqVaKhongGhiGi() {
        EventEnvelope<FeedbackCreatedData> event =
                EventEnvelope.of("FeedbackCreated", new FeedbackCreatedData(1L, 7L, 6, AT));

        assertThrows(AmqpRejectAndDontRequeueException.class, () -> listener.onFeedbackCreated(event));
        verifyNoInteractions(repository);
    }

    @Test
    void visitMoi_congLuotXem() {
        EventEnvelope<VisitRecordedData> event =
                EventEnvelope.of("VisitRecorded", new VisitRecordedData(9L, 7L, AT));
        when(repository.markProcessed(event.eventId(), "VisitRecorded")).thenReturn(true);

        listener.onVisitRecorded(event);

        verify(repository).addView(LocalDate.of(2026, 11, 10), 7L);
    }

    @Test
    void visitThieuData_vaoDlq() {
        EventEnvelope<VisitRecordedData> event = EventEnvelope.of("VisitRecorded", null);

        assertThrows(AmqpRejectAndDontRequeueException.class, () -> listener.onVisitRecorded(event));
        verifyNoInteractions(repository);
    }
}
