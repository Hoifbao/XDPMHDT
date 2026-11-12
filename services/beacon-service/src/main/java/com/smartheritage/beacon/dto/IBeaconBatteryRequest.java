package com.smartheritage.beacon.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Body của PUT /api/ibeacons/{id}/battery - Khoa, W6.
 * Gói tin iBeacon BLE thường không chứa mức pin, nên ADMIN cập nhật/giả lập bằng API này.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IBeaconBatteryRequest {

    @NotNull(message = "batteryLevel là bắt buộc")
    @Min(value = 0, message = "Pin (batteryLevel) phải nằm trong khoảng 0–100")
    @Max(value = 100, message = "Pin (batteryLevel) phải nằm trong khoảng 0–100")
    private Integer batteryLevel;
}
