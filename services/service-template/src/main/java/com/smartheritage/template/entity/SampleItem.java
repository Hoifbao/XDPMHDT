package com.smartheritage.template.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity mẫu. Khi tách service: XÓA file này, chuyển entity của mình sang (vd HeritageSite).
 * Quy tắc tách: KHÔNG @ManyToOne sang entity của service khác; dùng cột Long (vd Long siteId, Long userId).
 */
@Entity
@Table(name = "sample_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SampleItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    /** Ví dụ tham chiếu sang service khác bằng id, không dùng quan hệ JPA. */
    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
