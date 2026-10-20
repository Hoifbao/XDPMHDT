package com.smartheritage.backend.repository;

import com.smartheritage.backend.entity.HeritageLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HeritageLocationRepository extends JpaRepository<HeritageLocation, Long> {

    List<HeritageLocation> findByStatus(String status);

    List<HeritageLocation> findByCityIgnoreCase(String city);

    boolean existsByNameIgnoreCase(String name);
}
