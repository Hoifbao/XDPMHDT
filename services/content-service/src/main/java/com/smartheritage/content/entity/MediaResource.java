package com.smartheritage.content.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Ảnh/audio/video dùng chung của hiện vật (bảng content_db.media_resources). */
@Entity
@Table(name = "media_resources")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "artifact_id", nullable = false)
    private Long artifactId;

    /** IMAGE | AUDIO | VIDEO */
    @Column(name = "media_type", nullable = false, length = 20)
    private String mediaType;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Column(length = 255)
    private String caption;
}
