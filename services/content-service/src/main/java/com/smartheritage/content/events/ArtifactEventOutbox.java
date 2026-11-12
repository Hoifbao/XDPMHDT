package com.smartheritage.content.events;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/** Stored atomically with the artifact. No FK: deletion events survive artifact deletion. */
@Entity
@Table(name = "artifact_event_outbox", indexes = @Index(name = "idx_artifact_outbox_pending", columnList = "published_at,occurred_at"))
@Getter
@NoArgsConstructor
public class ArtifactEventOutbox {
    @Id
    @Column(length = 36)
    private String eventId;
    @Column(nullable = false)
    private Instant occurredAt;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;
    private Instant publishedAt;

    ArtifactEventOutbox(String eventId, Instant occurredAt, String payload) {
        this.eventId = eventId;
        this.occurredAt = occurredAt;
        this.payload = payload;
    }

    void published() {
        publishedAt = Instant.now();
    }
}
