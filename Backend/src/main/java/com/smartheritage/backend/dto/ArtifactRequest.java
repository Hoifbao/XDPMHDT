package com.smartheritage.backend.dto;

import lombok.Data;
import java.util.List;

/**
 * Payload khi tạo mới hoặc cập nhật hiện vật.
 */
@Data
public class ArtifactRequest {

    /** ID của địa điểm di tích */
    private Long locationId;

    private String name;

    private String artifactCode;

    private String description;

    private String thumbnailUrl;

    private String period;

    private String material;

    private String dimensions;

    private String beaconUuid;

    /** ACTIVE | INACTIVE */
    private String status;

    /** Nội dung đa ngôn ngữ (tuỳ chọn, tạo kèm hiện vật) */
    private List<ArtifactContentRequest> contents;
}
