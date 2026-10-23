package com.smartheritage.backend.dto;

import lombok.Data;

/**
 * Payload khi tạo mới hoặc cập nhật iBeacon.
 */
@Data
public class IBeaconRequest {

    private String uuid;
    private Integer major;
    private Integer minor;

    /** ID di tích nơi đặt beacon (bắt buộc) */
    private Long siteId;

    /** ID của hiện vật được gán (có thể null). Hiện vật phải thuộc cùng di tích. */
    private Long artifactId;

    /** Toạ độ trên sơ đồ di tích (tuỳ chọn), lưu vào cột x_coord / y_coord.
     *  Đặt tên posX/posY vì Jackson đọc getXCoord() thành "xcoord" (sai tên JSON). */
    private Double posX;
    private Double posY;

    /** Phần trăm pin 0–100 (tuỳ chọn) */
    private Integer batteryLevel;

    /** ACTIVE | INACTIVE (mặc định ACTIVE) */
    private String status;
}
