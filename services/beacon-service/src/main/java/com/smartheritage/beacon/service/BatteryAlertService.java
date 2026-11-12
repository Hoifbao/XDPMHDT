package com.smartheritage.beacon.service;

import com.smartheritage.beacon.entity.IBeacon;
import com.smartheritage.beacon.events.BeaconBatteryLowData;
import com.smartheritage.beacon.repository.IBeaconRepository;
import com.smartheritage.common.events.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Cảnh báo pin beacon - Khoa, W6.
 *
 * Tác vụ định kỳ 15 phút (BatteryAlertScheduler) quét beacon ACTIVE pin < 20%:
 *   - beacon CHƯA từng cảnh báo (battery_alerted_at null) -> phát sự kiện beacon.battery.low
 *     lên sh.events (FCM cho Admin do chính beacon-service nhận) và đánh dấu đã cảnh báo.
 *   - beacon đã cảnh báo mà pin hồi lại >= 20% (sạc/thay pin) -> xóa cờ, cho phép cảnh báo lần sau.
 * INACTIVE không cảnh báo (tránh báo ảo cho beacon đã tháo).
 *
 * Dùng EventPublisher của thư viện chung (publishAfterCommit - không có giao dịch thì gửi ngay),
 * metric sh_events_published_total{type="beacon.battery.low"} tự có để Bảo đưa lên Grafana.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatteryAlertService {

    /** Ngưỡng pin thấp theo task_w6_chi_tiet.docx (Thứ 3): < 20% -> cảnh báo. */
    public static final int LOW_BATTERY_THRESHOLD = 20;

    public static final String ROUTING_KEY = "beacon.battery.low";
    public static final String EVENT_TYPE = "beacon.battery.low";

    private final IBeaconRepository beaconRepository;
    private final EventPublisher eventPublisher;

    /**
     * Quét 1 lượt. @return số beacon vừa phát sự kiện cảnh báo (để API chạy thủ công trả về cho demo).
     */
    @Transactional
    public int checkAndAlert() {
        List<IBeacon> lowActive = beaconRepository
                .findByStatusAndBatteryLevelLessThan("ACTIVE", LOW_BATTERY_THRESHOLD);
        int alerted = 0;
        for (IBeacon beacon : lowActive) {
            if (beacon.getBatteryAlertedAt() != null) {
                continue; // đã cảnh báo từ trước, pin chưa được sạc lại -> không gửi lặp
            }
            BeaconBatteryLowData data = new BeaconBatteryLowData(
                    beacon.getId(), beacon.getSiteId(), beacon.getBatteryLevel(), Instant.now());
            eventPublisher.publishAfterCommit(ROUTING_KEY, EVENT_TYPE, data);
            beacon.setBatteryAlertedAt(LocalDateTime.now());
            beaconRepository.save(beacon);
            alerted++;
            log.info("[BATTERY] Pin beacon #{} (site {}) còn {}% -> đã phát sự kiện beacon.battery.low",
                    beacon.getId(), beacon.getSiteId(), beacon.getBatteryLevel());
        }

        // Beacon đã cảnh báo mà pin hồi lại -> xóa cờ để lần sắp cạn tiếp theo vẫn được báo
        List<IBeacon> recovered = beaconRepository
                .findByBatteryLevelGreaterThanEqualAndBatteryAlertedAtNotNull(LOW_BATTERY_THRESHOLD);
        for (IBeacon beacon : recovered) {
            beacon.setBatteryAlertedAt(null);
            beaconRepository.save(beacon);
            log.info("[BATTERY] Pin beacon #{} đã hồi lại ({}%) -> xóa cờ cảnh báo", 
                    beacon.getId(), beacon.getBatteryLevel());
        }
        return alerted;
    }
}
