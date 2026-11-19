package com.smartheritage.content.events;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import java.util.concurrent.atomic.AtomicBoolean;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class ArtifactEventDispatcher {
    private final ArtifactEventOutboxRepository outbox;
    private final ArtifactEventDelivery delivery;
    private final ObjectProvider<ArtifactEventTransport> transport;
    private final AtomicBoolean failureReported = new AtomicBoolean();

    public record Pending(String eventId) {}

    @PostConstruct
    void reportConfiguration() {
        if (transport.getIfAvailable() == null) {
            log.warn("ArtifactUpdated delivery NOT configured: changes remain in artifact_event_outbox; "
                    + "no ArtifactEventTransport bean is available");
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterCommit(Pending event) {
        tryDelivery(event.eventId());
    }

    @Scheduled(fixedDelayString = "${app.events.retry-delay-ms:10000}",
            initialDelayString = "${app.events.retry-delay-ms:10000}")
    public void retryPending() {
        if (transport.getIfAvailable() == null) return;
        for (var entry : outbox.findTop50ByPublishedAtIsNullOrderByOccurredAtAsc()) {
            if (!tryDelivery(entry.getEventId())) break;
        }
    }

    private boolean tryDelivery(String id) {
        try {
            delivery.deliver(id);
            if (failureReported.getAndSet(false)) log.info("ArtifactUpdated delivery recovered; retrying pending events");
            return true;
        } catch (Exception exception) {
            if (failureReported.compareAndSet(false, true)) {
                String reason = String.valueOf(exception.getMessage()).replaceAll("[\\r\\n]+", " ");
                log.warn("ArtifactUpdated delivery unavailable; eventId={} remains pending: {}", id,
                        reason.substring(0, Math.min(reason.length(), 240)));
            }
            return false;
        }
    }
}
