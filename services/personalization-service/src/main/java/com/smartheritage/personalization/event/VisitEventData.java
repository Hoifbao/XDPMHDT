package com.smartheritage.personalization.event;

import java.time.LocalDateTime;

/** Exact payload expected by analytics-service for routing key visit.recorded.
 *  detectedAt is local Vietnamese time (Asia/Ho_Chi_Minh), without an offset. */
public record VisitEventData(Long visitId, Long artifactId, LocalDateTime detectedAt) {
}
