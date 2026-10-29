package com.smartheritage.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Dữ liệu trả về của một đánh giá.
 * Không trả userId, email hay username của người đánh giá;
 * chỉ trả tên hiển thị (reviewerName).
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FeedbackResponse {

    private Long id;

    /** ID hiện vật được đánh giá */
    private Long artifactId;

    /** Số sao, từ 1 đến 5 */
    private Integer rating;

    /** Bình luận, có thể null */
    private String comment;

    /** Tên hiển thị của người đánh giá: fullName đã trim, nếu null/rỗng thì "Khách" */
    private String reviewerName;

    /** PENDING | APPROVED | REJECTED */
    private String status;

    private LocalDateTime createdAt;
}