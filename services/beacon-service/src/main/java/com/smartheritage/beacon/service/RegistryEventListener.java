package com.smartheritage.beacon.service;

import com.smartheritage.beacon.client.ArtifactClient;
import com.smartheritage.beacon.config.NotificationRabbitConfig;
import com.smartheritage.beacon.events.ArtifactUpdatedData;
import com.smartheritage.common.events.EventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * Nhận sự kiện ArtifactUpdated của content-service (Xuân Anh) - Khoa, W6 (task_w6_chi_tiet, Thứ 4).
 *
 * Luồng: Admin sửa hiện vật trên CMS -> content-service phát artifact.updated lên sh.events
 * -> queue registry.artifact-updated -> listener này HỦY cache tên hiện vật của ArtifactClient
 * -> lượt GET /api/ibeacons (registry) kế tiếp tự lấy tên mới từ content-service
 * thay vì hiển thị tên cũ đến khi hết TTL 5 phút.
 *
 * Quy ước bên nhận (services/README.md): record data tự khai báo (chỉ lấy artifactId),
 * chống trùng theo eventId bằng ProcessedEvents, lỗi quá 3 lần -> DLQ.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistryEventListener {

    private final ProcessedEvents processedEvents;
    private final ArtifactClient artifactClient;

    @RabbitListener(queues = NotificationRabbitConfig.QUEUE_REGISTRY_ARTIFACT_UPDATED)
    public void onArtifactUpdated(EventEnvelope<ArtifactUpdatedData> event) {
        ArtifactUpdatedData data = event.data();
        log.info("[EVENTS] Nhận {}: eventId={}, artifactId={}", event.type(), event.eventId(),
                data == null ? null : data.artifactId());

        if (data == null || data.artifactId() == null) {
            log.warn("[EVENTS] Sự kiện {} thiếu data.artifactId -> bỏ vào dead-letter queue",
                    event.eventId());
            throw new IllegalArgumentException("ArtifactUpdated thiếu data.artifactId: " + event.eventId());
        }
        if (!processedEvents.markIfNew(event.eventId(), event.type())) {
            log.info("[EVENTS] eventId {} đã xử lý rồi -> bỏ qua", event.eventId());
            return;
        }

        boolean invalidated = artifactClient.invalidate(data.artifactId());
        // Registry của beacon-service đọc tên qua ArtifactClient -> hủy cache = registry cần tải lại
        log.info("[EVENTS] eventId {}: registry cần tải lại hiện vật {} (hủy cache: {})",
                event.eventId(), data.artifactId(), invalidated ? "có" : "không có cache cũ");
    }
}
