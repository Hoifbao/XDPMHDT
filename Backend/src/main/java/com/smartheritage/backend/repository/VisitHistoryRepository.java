package com.smartheritage.backend.repository;

import com.smartheritage.backend.dto.VisitHistoryResponse;
import com.smartheritage.backend.entity.VisitHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface VisitHistoryRepository extends JpaRepository<VisitHistory, Long> {

    boolean existsByUser_IdAndClientEventId(Long userId, String clientEventId);

    @Query(value = """
        select new com.smartheritage.backend.dto.VisitHistoryResponse(
            v.id, a.id, a.name, s.name, a.thumbnailUrl,
            v.detectedAt, v.durationSeconds, v.source, v.clientEventId
        )
        from VisitHistory v
        join v.artifact a
        join a.site s
        where v.user.id = :userId
        order by v.detectedAt desc
        """,
        countQuery = "select count(v) from VisitHistory v where v.user.id = :userId")
    Page<VisitHistoryResponse> findHistoryByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query(value = """
        select new com.smartheritage.backend.dto.VisitHistoryResponse(
            v.id, a.id, a.name, s.name, a.thumbnailUrl,
            v.detectedAt, v.durationSeconds, v.source, v.clientEventId
        )
        from VisitHistory v
        join v.user u
        join v.artifact a
        join a.site s
        where (:fromTime is null or v.detectedAt >= :fromTime)
          and (:toTime is null or v.detectedAt < :toTime)
          and (:siteId is null or s.id = :siteId)
        order by v.detectedAt desc
        """,
        countQuery = """
        select count(v) from VisitHistory v
        join v.artifact a join a.site s
        where (:fromTime is null or v.detectedAt >= :fromTime)
          and (:toTime is null or v.detectedAt < :toTime)
          and (:siteId is null or s.id = :siteId)
        """)
    Page<VisitHistoryResponse> findAdminHistory(
        @Param("fromTime") LocalDateTime fromTime,
        @Param("toTime") LocalDateTime toTime,
        @Param("siteId") Long siteId,
        Pageable pageable
    );
}
