package com.smartheritage.beacon.events;

import java.time.Instant;

/**
 * Phần data của sự kiện BeaconBatteryLow (routing key beacon.battery.low) - Khoa, W6.
 * beacon-service vừa GỬI vừa NHẬN sự kiện này (gửi từ tác vụ định kỳ, nhận để FCM topic Admin).
 *
 * @param beaconId     id beacon trong beacon_db.ibeacons
 * @param siteId       di tích nơi đặt beacon
 * @param batteryLevel mức pin tại lúc phát sự kiện (%)
 * @param checkedAt    thời điểm quét phát hiện pin thấp
 */
public record BeaconBatteryLowData(Long beaconId, Long siteId, Integer batteryLevel, Instant checkedAt) {
}
