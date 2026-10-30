package com.smartheritage.content.repository;

import com.smartheritage.content.entity.ArtifactContent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArtifactContentRepository extends JpaRepository<ArtifactContent, Long> {

    List<ArtifactContent> findByArtifactId(Long artifactId);

    Optional<ArtifactContent> findByArtifactIdAndLanguageCode(Long artifactId, String languageCode);

    void deleteByArtifactId(Long artifactId);
}
