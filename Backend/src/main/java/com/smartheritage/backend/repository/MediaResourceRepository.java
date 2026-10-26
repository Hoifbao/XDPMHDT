package com.smartheritage.backend.repository;

import com.smartheritage.backend.entity.MediaResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MediaResourceRepository extends JpaRepository<MediaResource, Long> {

    List<MediaResource> findByArtifactId(Long artifactId);

    List<MediaResource> findByArtifactIdAndMediaType(Long artifactId, String mediaType);
}
