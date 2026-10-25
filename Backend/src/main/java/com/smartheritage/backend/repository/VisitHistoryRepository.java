package com.smartheritage.backend.repository;

import com.smartheritage.backend.entity.VisitHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VisitHistoryRepository extends JpaRepository<VisitHistory, Long> {

    /** Lịch sử tham quan của một user, mới nhất trước */
    List<VisitHistory> findByUser_IdOrderByDetectedAtDesc(Long userId);
}
