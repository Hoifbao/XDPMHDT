package com.smartheritage.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FavoriteResponse {

    private Long artifactId;
    private String artifactName;
    private String thumbnailUrl;
    private Long siteId;
    private String siteName;
    private LocalDateTime savedAt;
}
