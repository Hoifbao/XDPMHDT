package com.smartheritage.beacon.dto;

import jakarta.validation.constraints.DecimalMax;
import lombok.Data;

/**
 * Request cập nhật vị trí đặt beacon trên sơ đồ/bản đồ - Khoa, Sprint 5 (cho Ân).
 *
 * Đơn vị quy ước của bản đồ (x_coord, y_coord trong bảng ibeacons - Infra/db/04_beacon.sql),
 * KHÔNG phải kinh/vĩ độ. Frontend (WebAdmin ibeacons + mobile bản đồ của Ân) gửi và đọc
 * lại với tên posX / posY như các API list/CRUD hiện có.
 */
@Data
public class IBeaconPositionRequest {

    /** Toạ độ X (đơn vị bản đồ quy ước; null = chưa đặt vị trí) */
    @DecimalMax("99999999")
    private Double posX;

    /** Toạ độ Y (đơn vị bản đồ quy ước; null = chưa đặt vị trí) */
    @DecimalMax("99999999")
    private Double posY;
}
