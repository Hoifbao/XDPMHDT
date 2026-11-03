package com.smartheritage.personalization.dto;

import java.time.LocalDateTime;

public record FavoriteResponse(Long artifactId,
                               String artifactName,
                               String thumbnailUrl,
                               Long siteId,
                               String siteName,
                               LocalDateTime savedAt) {
}
