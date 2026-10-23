package com.smartheritage.backend.dto;

import lombok.Data;

/**
 * Payload khi tạo mới hoặc cập nhật iBeacon.
 */
@Data
public class IBeaconRequest {

    private String uuid;

    private Integer major;

    private Integer minor;

    /** ID của hiện vật được gán (có thể null) */
    private Long artifactId;

    /** ACTIVE | INACTIVE */
    private String status;
}
