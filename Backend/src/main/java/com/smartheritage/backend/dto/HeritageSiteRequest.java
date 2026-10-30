package com.smartheritage.backend.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO nhận dữ liệu từ client khi tạo mới hoặc cập nhật di tích.
 */
@Data
public class HeritageSiteRequest {

    private String name;
    private String location;
    private String mapImageUrl;
    private BigDecimal latitude;
    private BigDecimal longitude;

    /**
     * Chỉ dùng khi cập nhật (PUT). PUT là cập nhật một phần: latitude/longitude = null nghĩa là
     * "giữ tọa độ cũ", nên không có cách nào xóa tọa độ nếu chỉ gửi null. Gửi clearCoordinates = true
     * để xóa hẳn tọa độ (di tích không còn hiện trên bản đồ và không xuất hiện trong /nearby).
     * CMS tự gửi true khi đang sửa di tích mà người dùng xóa trống cả 2 ô tọa độ (Ân - W3).
     */
    private Boolean clearCoordinates;
}
