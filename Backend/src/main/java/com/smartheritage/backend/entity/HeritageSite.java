package com.smartheritage.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Entity HeritageSite – ánh xạ tới bảng heritage_sites.
 * Lưu thông tin khu di tích bao gồm tọa độ địa lý (latitude, longitude)
 * phục vụ hiển thị bản đồ tương tác (US03, US13).
 */
@Entity
@Table(name = "heritage_sites")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HeritageSite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 255)
    private String location;

    @Column(name = "map_image_url", length = 500)
    private String mapImageUrl;

    /** Vĩ độ – WGS-84, tối đa 7 chữ số thập phân (~1.1 cm chính xác). */
    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    /** Kinh độ – WGS-84, tối đa 7 chữ số thập phân. */
    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;
}
