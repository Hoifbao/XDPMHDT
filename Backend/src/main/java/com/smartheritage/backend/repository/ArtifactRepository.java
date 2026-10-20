package com.smartheritage.backend.repository;

import com.smartheritage.backend.entity.Artifact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArtifactRepository extends JpaRepository<Artifact, Long> {

    List<Artifact> findByLocationId(Long locationId);

    List<Artifact> findByStatus(String status);

    List<Artifact> findByLocationIdAndStatus(Long locationId, String status);

    Optional<Artifact> findByBeaconUuid(String beaconUuid);

    Optional<Artifact> findByArtifactCode(String artifactCode);

    boolean existsByArtifactCode(String artifactCode);

    boolean existsByBeaconUuid(String beaconUuid);

    @Query("SELECT a FROM Artifact a WHERE LOWER(a.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Artifact> searchByName(@Param("keyword") String keyword);
}
