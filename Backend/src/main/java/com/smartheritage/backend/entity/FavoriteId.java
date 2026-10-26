
package com.smartheritage.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Composite primary key for the likes table.
 * Maps to likes(user_id, artifact_id).
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteId implements Serializable {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "artifact_id")
    private Long artifactId;
}
