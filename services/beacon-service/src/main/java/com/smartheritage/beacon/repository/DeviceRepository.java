package com.smartheritage.beacon.repository;

import com.smartheritage.beacon.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * DeviceRepository của beacon-service - Khoa, Sprint 5.
 * Tìm theo token (upsert) và theo user (liệt kê/ xoá khi đăng xuất).
 */
public interface DeviceRepository extends JpaRepository<Device, Long> {

    Optional<Device> findByFcmToken(String fcmToken);

    List<Device> findByUserIdOrderByUpdatedAtDesc(Long userId);

    void deleteByUserId(Long userId);

    long countByUserId(Long userId);
}
