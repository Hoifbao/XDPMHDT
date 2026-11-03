package com.smartheritage.personalization.dto;

import java.time.LocalDateTime;

public record VisitHistoryResponse(Long id,
                                   Long artifactId,
                                   String artifactName,
                                   String thumbnailUrl,
                                   Long siteId,
                                   String siteName,
                                   LocalDateTime detectedAt,
                                   Integer durationSeconds,
                                   String source,
                                   String clientEventId) {
}
