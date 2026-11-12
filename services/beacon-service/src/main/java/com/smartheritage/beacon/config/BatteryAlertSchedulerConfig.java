package com.smartheritage.beacon.config;

import com.smartheritage.beacon.service.BatteryAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Tác vụ định kỳ quét pin beacon - Khoa, W6 (task_w6_chi_tiet.docx, Thứ 3).
 *
 * Mặc định chạy mỗi 15 phút (app.beacon.battery-check.interval-ms). Demo muốn thấy ngay
 * thì gọi POST /api/ibeacons/battery-check (ADMIN) chạy 1 lượt ngay lập tức.
 * Tắt bằng app.beacon.battery-check.enabled=false (ví dụ khi test).
 */
@Slf4j
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.beacon.battery-check.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class BatteryAlertSchedulerConfig {

    private final BatteryAlertService batteryAlertService;

    /** fixedDelay: nghỉ giữa 2 lượt tính từ lúc LƯỢT TRƯỚC XONG - không dồn hàng khi DB chậm. */
    @Scheduled(fixedDelayString = "${app.beacon.battery-check.interval-ms:900000}", initialDelay = 60_000)
    public void checkBatteryPeriodically() {
        try {
            int alerted = batteryAlertService.checkAndAlert();
            if (alerted > 0) {
                log.info("[BATTERY] Lượt quét định kỳ: cảnh báo {} beacon pin thấp", alerted);
            }
        } catch (Exception e) {
            // Lỗi 1 lượt không làm chết scheduler - lượt sau chạy lại bình thường
            log.warn("[BATTERY] Lượt quét pin lỗi: {}", e.getMessage());
        }
    }
}
