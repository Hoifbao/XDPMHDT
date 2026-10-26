package com.smartheritage.backend.dto;

import java.time.LocalDateTime;

public record VisitHistoryResponse(
    Long id,
    Long artifactId,
    String artifactName,
    String siteName,
    String thumbnailUrl,
    LocalDateTime detectedAt,
    Integer durationSeconds,
    String source,
    String clientEventId
) {}
