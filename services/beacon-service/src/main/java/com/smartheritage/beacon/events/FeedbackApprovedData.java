package com.smartheritage.beacon.events;

/**
 * Phần data của sự kiện FeedbackApproved (routing key feedback.approved) - Khoa, W6.
 *
 * Khai báo tại beacon-service, KHÔNG import class của feedback-service:
 * message không có header __TypeId__, bên nhận đọc bằng record của service mình
 * (xem services/feedback-service/README.md mục "Sự kiện RabbitMQ").
 *
 * @param feedbackId   id đánh giá trong feedback_db
 * @param userId       người gửi đánh giá - dùng để tìm FCM token trong beacon_db.devices
 * @param artifactId   id hiện vật trong content_db
 * @param artifactName tên hiện vật bên feedback-service chụp lại; thiếu -> "Hiện vật #<artifactId>"
 */
public record FeedbackApprovedData(Long feedbackId, Long userId, Long artifactId, String artifactName) {
}
