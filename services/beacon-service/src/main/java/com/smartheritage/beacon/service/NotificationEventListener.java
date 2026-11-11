package com.smartheritage.beacon.service;

import com.smartheritage.beacon.config.NotificationRabbitConfig;
import com.smartheritage.beacon.entity.Device;
import com.smartheritage.beacon.events.FeedbackApprovedData;
import com.smartheritage.beacon.repository.DeviceRepository;
import com.smartheritage.common.events.EventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Nhận sự kiện FeedbackApproved và gửi thông báo FCM - Khoa, W6 (XDPMHDT-64).
 *
 * Luồng: Admin duyệt đánh giá (feedback-service, Tường) -> exchange sh.events
 * -> queue notification.feedback-approved -> listener này -> tìm FCM token theo userId
 * (beacon_db.devices) -> "Đánh giá của bạn về <tên hiện vật> đã được duyệt".
 *
 * Điểm quan trọng:
 *  - Bỏ qua tin trùng theo eventId (ProcessedEvents) - du khách không nhận thông báo 2 lần.
 *  - artifactName thiếu -> "Hiện vật #<id>" (quy ước của feedback-service).
 *  - FCM chưa cấu hình (thiếu FIREBASE_SERVICE_ACCOUNT) -> vẫn đánh dấu đã xử lý và ghi log,
 *    giống hành vi FcmService ở tuần 5: không làm hỏng luồng demo, không retry vô hạn.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEventListener {

    static final String NOTIFICATION_TITLE = "Smart Heritage";

    private final ProcessedEvents processedEvents;
    private final DeviceRepository deviceRepository;
    private final FcmService fcmService;

    /**
     * Bên nhận chỉ khai báo record data của mình (không import class feedback-service);
     * kiểu dữ liệu suy ra từ tham số của @RabbitListener nhờ MessageConverter JSON của common.
     */
    @RabbitListener(queues = NotificationRabbitConfig.QUEUE_FEEDBACK_APPROVED)
    public void onFeedbackApproved(EventEnvelope<FeedbackApprovedData> event) {
        FeedbackApprovedData data = event.data();
        log.info("[EVENTS] Nhận {}: eventId={}, feedbackId={}, userId={}",
                event.type(), event.eventId(), data == null ? null : data.feedbackId(),
                data == null ? null : data.userId());

        if (data == null || data.userId() == null) {
            // Tin hỏng - ném lỗi để vào DLQ, không retry vô hạn
            log.warn("[EVENTS] Sự kiện {} thiếu data.userId -> bỏ vào dead-letter queue", event.eventId());
            throw new IllegalArgumentException("FeedbackApproved thiếu data hoặc userId: " + event.eventId());
        }
        if (!processedEvents.markIfNew(event.eventId(), event.type())) {
            log.info("[EVENTS] eventId {} đã xử lý rồi -> bỏ qua", event.eventId());
            return;
        }

        String artifactName = resolveArtifactName(data);
        String body = "Đánh giá của bạn về " + artifactName + " đã được duyệt";

        List<Device> devices = deviceRepository.findByUserIdOrderByUpdatedAtDesc(data.userId());
        if (devices.isEmpty()) {
            log.info("[EVENTS] userId {} chưa đăng ký thiết bị -> không gửi FCM (eventId={})",
                    data.userId(), event.eventId());
            return;
        }
        int sent = 0;
        for (Device device : devices) {
            if (fcmService.sendToToken(device.getFcmToken(), NOTIFICATION_TITLE, body)) {
                sent++;
            }
        }
        log.info("[EVENTS] eventId {}: gửi thông báo thành công {}/{} thiết bị của userId {}",
                event.eventId(), sent, devices.size(), data.userId());
    }

    /** artifactName null hoặc rỗng -> "Hiện vật #<id>" (quy ước feedback-service). */
    static String resolveArtifactName(FeedbackApprovedData data) {
        if (data.artifactName() == null || data.artifactName().isBlank()) {
            return "Hiện vật #" + data.artifactId();
        }
        return data.artifactName();
    }
}
