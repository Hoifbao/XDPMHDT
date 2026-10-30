package com.smartheritage.backend.repository;

import com.smartheritage.backend.entity.IBeacon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IBeaconRepository extends JpaRepository<IBeacon, Long> {

    Optional<IBeacon> findByUuidAndMajorAndMinor(String uuid, Integer major, Integer minor);

    boolean existsByUuidAndMajorAndMinor(String uuid, Integer major, Integer minor);

    /** Danh sách beacon của một di tích (lọc trên Web, bản đồ beacon tuần 5) */
    List<IBeacon> findBySiteId(Long siteId);

    /** Beacon đang gán cho một hiện vật (mỗi beacon gán tối đa 1 hiện vật) */
    List<IBeacon> findByArtifactId(Long artifactId);

    /** Beacon theo trạng thái (registry cho Mobile lấy ACTIVE) */
    List<IBeacon> findByStatus(String status);
}
