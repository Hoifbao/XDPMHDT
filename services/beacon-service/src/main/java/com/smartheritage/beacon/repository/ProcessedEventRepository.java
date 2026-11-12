package com.smartheritage.beacon.repository;

import com.smartheritage.beacon.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * ProcessedEventRepository của beacon-service - Khoa, W6.
 * Chỉ cần kiểm tra eventId đã tồn tại (insert trùng bị chặn bởi unique key).
 */
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {

    boolean existsByEventId(UUID eventId);
}
