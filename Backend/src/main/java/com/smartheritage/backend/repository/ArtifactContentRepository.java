package com.smartheritage.backend.repository;

import com.smartheritage.backend.entity.ArtifactContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArtifactContentRepository extends JpaRepository<ArtifactContent, Long> {

    List<ArtifactContent> findByArtifactId(Long artifactId);

    Optional<ArtifactContent> findByArtifactIdAndLanguageCode(Long artifactId, String languageCode);

    boolean existsByArtifactIdAndLanguageCode(Long artifactId, String languageCode);

    void deleteByArtifactId(Long artifactId);
}
