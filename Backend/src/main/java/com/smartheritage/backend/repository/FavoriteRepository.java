
package com.smartheritage.backend.repository;

import com.smartheritage.backend.dto.FavoriteResponse;
import com.smartheritage.backend.entity.Favorite;
import com.smartheritage.backend.entity.FavoriteId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FavoriteRepository
        extends JpaRepository<Favorite, FavoriteId> {

    /**
     * Lấy danh sách yêu thích dạng DTO, có phân trang.
     * Sắp xếp theo thời gian lưu mới nhất trước.
     */
    @Query(
        value = """
            select new com.smartheritage.backend.dto.FavoriteResponse(
                a.id, a.name, s.name, a.thumbnailUrl, f.savedAt
            )
            from Favorite f
            join f.artifact a
            join a.site s
            where f.user.id = :userId
            order by f.savedAt desc
            """,
        countQuery = """
            select count(f)
            from Favorite f
            where f.user.id = :userId
            """
    )
    Page<FavoriteResponse> findResponsesByUserId(
            @Param("userId") Long userId,
            Pageable pageable);

    /**
     * Kiểm tra người dùng đã yêu thích hiện vật hay chưa.
     */
    boolean existsByUser_IdAndArtifact_Id(
            Long userId,
            Long artifactId);

    /**
     * Lấy danh sách entity Favorite của một người dùng,
     * sắp xếp theo thời gian lưu mới nhất trước.
     */
    List<Favorite> findByUser_IdOrderBySavedAtDesc(Long userId);
}
