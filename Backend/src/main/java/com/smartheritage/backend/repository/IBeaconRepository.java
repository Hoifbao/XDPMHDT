package com.smartheritage.backend.repository;

import com.smartheritage.backend.entity.IBeacon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IBeaconRepository extends JpaRepository<IBeacon, Long> {

    Optional<IBeacon> findByUuidAndMajorAndMinor(String uuid, Integer major, Integer minor);

    boolean existsByUuidAndMajorAndMinor(String uuid, Integer major, Integer minor);
}
