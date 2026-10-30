
package com.smartheritage.backend.repository;

import com.smartheritage.backend.dto.VisitHistoryResponse;
import com.smartheritage.backend.entity.VisitHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VisitHistoryRepository
        extends JpaRepository<VisitHistory, Long> {

    /**
     * Kiểm tra sự kiện tham quan đã được ghi nhận hay chưa.
     */
    boolean existsByUser_IdAndClientEventId(
            Long userId,
            String clientEventId);

    /**
     * Lấy lịch sử tham quan của người dùng dưới dạng DTO,
     * có phân trang và sắp xếp mới nhất trước.
     */
    @Query(
        value = """
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
        countQuery = """
            select count(v)
            from VisitHistory v
            where v.user.id = :userId
            """
    )
    Page<VisitHistoryResponse> findHistoryByUserId(
            @Param("userId") Long userId,
            Pageable pageable);

    /**
     * Lịch sử tham quan dành cho quản trị viên.
     * Có thể lọc theo khoảng thời gian và địa điểm.
     */
    @Query(
        value = """
            select new com.smartheritage.backend.dto.VisitHistoryResponse(
                v.id, a.id, a.name, s.name, a.thumbnailUrl,
                v.detectedAt, v.durationSeconds, v.source, v.clientEventId
            )
            from VisitHistory v
            join v.artifact a
            join a.site s
            where (:fromTime is null or v.detectedAt >= :fromTime)
              and (:toTime is null or v.detectedAt < :toTime)
              and (:siteId is null or s.id = :siteId)
            order by v.detectedAt desc
            """,
        countQuery = """
            select count(v)
            from VisitHistory v
            join v.artifact a
            join a.site s
            where (:fromTime is null or v.detectedAt >= :fromTime)
              and (:toTime is null or v.detectedAt < :toTime)
              and (:siteId is null or s.id = :siteId)
            """
    )
    Page<VisitHistoryResponse> findAdminHistory(
            @Param("fromTime") LocalDateTime fromTime,
            @Param("toTime") LocalDateTime toTime,
            @Param("siteId") Long siteId,
            Pageable pageable);

    /**
     * Lấy danh sách entity của một người dùng,
     * sắp xếp lượt tham quan mới nhất trước.
     */
    List<VisitHistory> findByUser_IdOrderByDetectedAtDesc(Long userId);
}
