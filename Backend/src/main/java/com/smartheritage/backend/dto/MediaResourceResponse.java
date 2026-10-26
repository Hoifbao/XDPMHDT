package com.smartheritage.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MediaResourceResponse {

    private Long id;
    private Long artifactId;
    private String mediaType;
    private String fileUrl;
    private String caption;
}
