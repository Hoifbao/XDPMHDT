package com.smartheritage.content.events;

import java.time.Instant;

/**
 * Integration boundary implemented using common EventEnvelope and EventPublisher.
 * Implementations send the envelope to sh.events / artifact.updated,
 * preserve eventId on retry and return only after broker confirm and successful routing.
 * A failure must propagate so that the outbox entry remains pending.
 */
public interface ArtifactEventTransport {
    void send(String eventId, Instant occurredAt, ArtifactUpdated data);
}
