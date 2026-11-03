package com.smartheritage.personalization.repository;

import com.smartheritage.personalization.entity.VisitHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;

@Repository
public interface VisitHistoryRepository extends JpaRepository<VisitHistory, Long> {
    boolean existsByUserIdAndClientEventId(Long userId, String clientEventId);

    Page<VisitHistory> findByUserIdOrderByDetectedAtDesc(Long userId, Pageable pageable);

    @Query("""
            select v from VisitHistory v
            where (:fromTime is null or v.detectedAt >= :fromTime)
              and (:toTime is null or v.detectedAt < :toTime)
            order by v.detectedAt desc
            """)
    Page<VisitHistory> findAdminHistory(@Param("fromTime") LocalDateTime fromTime,
                                        @Param("toTime") LocalDateTime toTime,
                                        Pageable pageable);

    @Query("""
            select v from VisitHistory v
            where (:fromTime is null or v.detectedAt >= :fromTime)
              and (:toTime is null or v.detectedAt < :toTime)
              and v.artifactId in :artifactIds
            order by v.detectedAt desc
            """)
    Page<VisitHistory> findAdminHistoryByArtifacts(@Param("fromTime") LocalDateTime fromTime,
                                                   @Param("toTime") LocalDateTime toTime,
                                                   @Param("artifactIds") Collection<Long> artifactIds,
                                                   Pageable pageable);
}
