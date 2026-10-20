package com.smartheritage.backend.dto;

import lombok.Builder;
import lombok.Data;

/**
 * Dữ liệu tóm tắt hiện vật – dùng cho danh sách (không kèm contents).
 */
@Data
@Builder
public class ArtifactSummaryResponse {

    private Long id;
    private Long locationId;
    private String locationName;
    private String name;
    private String artifactCode;
    private String thumbnailUrl;
    private String period;
    private String material;
    private String beaconUuid;
    private String status;
}
