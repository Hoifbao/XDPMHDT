package com.smartheritage.backend.dto;

import java.time.LocalDateTime;

public record FavoriteResponse(
    Long artifactId,
    String artifactName,
    String siteName,
    String thumbnailUrl,
    LocalDateTime savedAt
) {}
