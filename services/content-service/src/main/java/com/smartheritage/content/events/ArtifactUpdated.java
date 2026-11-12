package com.smartheritage.content.events;

import java.time.Instant;
import java.util.List;

/** Domain payload only. The RabbitMQ adapter must wrap this in common's EventEnvelope. */
public record ArtifactUpdated(Long artifactId, Long siteId, List<String> changedFields, Instant updatedAt) {
    public ArtifactUpdated {
        changedFields = List.copyOf(changedFields);
    }
}
