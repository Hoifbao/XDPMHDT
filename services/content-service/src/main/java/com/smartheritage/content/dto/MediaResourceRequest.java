package com.smartheritage.content.dto;

import lombok.Data;

/**
 * Payload khi thêm tài nguyên đa phương tiện cho hiện vật.
 * Tuần 2: chỉ truyền URL dạng text (chưa upload file thật).
 */
@Data
public class MediaResourceRequest {

    /** IMAGE | AUDIO | VIDEO */
    private String mediaType;

    private String fileUrl;

    private String caption;
}
