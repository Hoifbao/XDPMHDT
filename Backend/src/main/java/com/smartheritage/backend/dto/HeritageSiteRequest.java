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
    private Boolean clearCoordinates;
}
