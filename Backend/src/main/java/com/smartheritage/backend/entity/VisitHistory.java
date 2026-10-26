package com.smartheritage.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Maps to the existing `visits` table from Infra/init_db.sql. */
@Entity
@Table(
    name = "visits",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_visits_user_event", columnNames = {"user_id", "client_event_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
public class VisitHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "artifact_id", nullable = false)
    private Artifact artifact;

    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    /** UUID generated on the mobile device; reused on every retry. */
    @Column(name = "client_event_id", length = 36)
    private String clientEventId;

    /** BEACON, MAP or MANUAL. */
    @Column(name = "source", nullable = false, length = 20)
    private String source = "MANUAL";

    @PrePersist
    void beforeInsert() {
        if (detectedAt == null) detectedAt = LocalDateTime.now();
        if (durationSeconds == null) durationSeconds = 0;
        if (source == null || source.isBlank()) source = "MANUAL";
    }
}
