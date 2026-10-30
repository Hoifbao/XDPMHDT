package com.smartheritage.backend.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Lịch sử tham quan hiện vật của người dùng.
 * Ánh xạ tới bảng visits trong cơ sở dữ liệu.
 */
@Entity
@Table(
    name = "visits",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_visits_user_event",
        columnNames = {"user_id", "client_event_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
public class VisitHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "user_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_visits_user")
    )
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "artifact_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_visits_artifact")
    )
    private Artifact artifact;

    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    /**
     * UUID do thiết bị di động tạo để nhận diện sự kiện.
     * Được dùng lại khi ứng dụng gửi lại cùng một sự kiện.
     */
    @Column(name = "client_event_id", length = 36)
    private String clientEventId;

    /**
     * Nguồn ghi nhận: BEACON, MAP hoặc MANUAL.
     */
    @Column(name = "source", nullable = false, length = 20)
    private String source = "MANUAL";

    /**
     * Constructor hỗ trợ Builder và giữ các giá trị mặc định.
     */
    @Builder
    public VisitHistory(
            Long id,
            User user,
            Artifact artifact,
            LocalDateTime detectedAt,
            Integer durationSeconds,
            String clientEventId,
            String source) {
        this.id = id;
        this.user = user;
        this.artifact = artifact;
        this.detectedAt = detectedAt != null
                ? detectedAt
                : LocalDateTime.now();
        this.durationSeconds = durationSeconds != null
                ? durationSeconds
                : 0;
        this.clientEventId = clientEventId;
        this.source = source == null || source.isBlank()
                ? "MANUAL"
                : source;
    }

    /**
     * Bổ sung giá trị mặc định trước khi ghi vào cơ sở dữ liệu.
     */
    @PrePersist
    void beforeInsert() {
        if (detectedAt == null) {
            detectedAt = LocalDateTime.now();
        }

        if (durationSeconds == null) {
            durationSeconds = 0;
        }

        if (source == null || source.isBlank()) {
            source = "MANUAL";
        }
    }
}