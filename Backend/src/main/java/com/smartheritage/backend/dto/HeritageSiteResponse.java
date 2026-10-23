package com.smartheritage.backend.dto;

import com.smartheritage.backend.entity.HeritageSite;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO trả về cho client – che giấu chi tiết Entity, an toàn hơn khi serialize.
 */
@Data
public class HeritageSiteResponse {

    private Long id;
    private String name;
    private String location;
    private String mapImageUrl;
    private BigDecimal latitude;
    private BigDecimal longitude;

    /** Chuyển đổi từ Entity sang DTO. */
    public static HeritageSiteResponse from(HeritageSite site) {
        HeritageSiteResponse dto = new HeritageSiteResponse();
        dto.setId(site.getId());
        dto.setName(site.getName());
        dto.setLocation(site.getLocation());
        dto.setMapImageUrl(site.getMapImageUrl());
        dto.setLatitude(site.getLatitude());
        dto.setLongitude(site.getLongitude());
        return dto;
    }
}
