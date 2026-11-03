package com.smartheritage.personalization.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteId implements Serializable {
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "artifact_id", nullable = false)
    private Long artifactId;
}
