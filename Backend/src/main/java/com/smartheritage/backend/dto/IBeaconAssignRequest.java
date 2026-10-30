package com.smartheritage.backend.dto;

import lombok.Data;

/**
 * Payload gán / bỏ gán hiện vật cho iBeacon.
 */
@Data
public class IBeaconAssignRequest {

    /** ID hiện vật cần gán. null = bỏ gán. */
    private Long artifactId;
}
