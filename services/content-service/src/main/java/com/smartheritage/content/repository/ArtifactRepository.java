package com.smartheritage.content.repository;

import com.smartheritage.content.entity.Artifact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ArtifactRepository extends JpaRepository<Artifact, Long> {

    List<Artifact> findBySiteId(Long siteId);

    Optional<Artifact> findByBeaconUuid(String beaconUuid);

    boolean existsByArtifactCode(String artifactCode);

    boolean existsByBeaconUuid(String beaconUuid);

    @Query("SELECT a FROM Artifact a WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Artifact> searchByName(@Param("keyword") String keyword);
}
