package com.smartheritage.backend.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** Payload khi Mobile ghi nhận một lượt tham quan hiện vật. */
@Data
public class VisitRequest {

    private Long artifactId;

    /** Thời gian xem (giây), mặc định 0 */
    private Integer durationSeconds;

    /** Thời điểm phát hiện; bỏ trống thì Backend lấy giờ hiện tại */
    private LocalDateTime detectedAt;
}
