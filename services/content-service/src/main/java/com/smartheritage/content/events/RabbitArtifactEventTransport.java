package com.smartheritage.content.events;

import com.smartheritage.common.events.EventEnvelope;
import com.smartheritage.common.events.EventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/** Outbox delivery succeeds only after broker ACK and successful routing. */
@Component
public class RabbitArtifactEventTransport implements ArtifactEventTransport {
    public static final String ROUTING_KEY = "artifact.updated";
    public static final String EVENT_TYPE = "ArtifactUpdated";

    private final EventPublisher publisher;
    private final Duration confirmTimeout;

    public RabbitArtifactEventTransport(EventPublisher publisher,
            @Value("${app.events.confirm-timeout-ms:5000}") long timeoutMs) {
        if (timeoutMs <= 0) throw new IllegalArgumentException("confirm-timeout-ms must be positive");
        this.publisher = publisher;
        this.confirmTimeout = Duration.ofMillis(timeoutMs);
    }

    @Override
    public void send(String eventId, Instant occurredAt, ArtifactUpdated data) {
        publisher.sendConfirmed(ROUTING_KEY,
                EventEnvelope.of(UUID.fromString(eventId), EVENT_TYPE, occurredAt, data), confirmTimeout);
    }
}
