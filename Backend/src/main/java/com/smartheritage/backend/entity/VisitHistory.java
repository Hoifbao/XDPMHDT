package com.smartheritage.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Lịch sử tham quan của du khách (Phú).
 * Ánh xạ bảng visits trong Infra/init_db.sql – quan hệ Visits (Users N-N Artifacts) của ERD.
 * Mỗi lần app phát hiện iBeacon của hiện vật sẽ ghi một dòng.
 */
@Entity
@Table(name = "visits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisitHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_visits_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "artifact_id", nullable = false, foreignKey = @ForeignKey(name = "fk_visits_artifact"))
    private Artifact artifact;

    /** Thời điểm phát hiện hiện vật (quét được iBeacon) */
    @Column(name = "detected_at", nullable = false)
    @Builder.Default
    private LocalDateTime detectedAt = LocalDateTime.now();

    /** Thời gian dừng lại xem hiện vật (giây) */
    @Column(name = "duration_seconds")
    @Builder.Default
    private Integer durationSeconds = 0;
}
