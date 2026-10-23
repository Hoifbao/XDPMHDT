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
public class IBeaconResponse {

    private Long id;
    private String uuid;
    private Integer major;
    private Integer minor;
    private Long artifactId;
    private String artifactName;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
