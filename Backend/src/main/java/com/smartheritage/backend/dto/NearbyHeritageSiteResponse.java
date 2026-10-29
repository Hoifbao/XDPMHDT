package com.smartheritage.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Di tích gần tọa độ tìm kiếm, kèm khoảng cách đường chim bay theo Haversine (km). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NearbyHeritageSiteResponse {

    private Long id;
    private String name;
    private String location;
    private String mapImageUrl;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private double distanceKm;

    public NearbyHeritageSiteResponse(HeritageSiteResponse site, double distanceKm) {
        this(site.getId(), site.getName(), site.getLocation(), site.getMapImageUrl(),
                site.getLatitude(), site.getLongitude(), distanceKm);
    }
}
