package com.smartheritage.feedback.event;

import java.time.LocalDateTime;

/** Phần data của 2 sự kiện do feedback-service gửi (task tuần 6, mục II). */
public final class FeedbackEventData {

    private FeedbackEventData() {
    }

    /** Du khách vừa tạo đánh giá mới. Tuấn đếm thời gian thực, Khoa báo Admin. */
    public record Created(Long feedbackId, Long userId, Long artifactId, Integer rating, LocalDateTime createdAt) {
    }

    /** Admin vừa duyệt đánh giá. Khoa tìm FCM token theo userId để báo du khách. */
    public record Approved(Long feedbackId, Long userId, Long artifactId, String artifactName) {
    }
}
