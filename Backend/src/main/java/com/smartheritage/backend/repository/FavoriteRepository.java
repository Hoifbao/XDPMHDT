package com.smartheritage.backend.repository;

import com.smartheritage.backend.dto.FavoriteResponse;
import com.smartheritage.backend.entity.Favorite;
import com.smartheritage.backend.entity.FavoriteId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FavoriteRepository extends JpaRepository<Favorite, FavoriteId> {

    @Query(value = """
        select new com.smartheritage.backend.dto.FavoriteResponse(
            a.id, a.name, s.name, a.thumbnailUrl, f.savedAt
        )
        from Favorite f
        join f.artifact a
        join a.site s
        where f.user.id = :userId
        order by f.savedAt desc
        """,
        countQuery = "select count(f) from Favorite f where f.user.id = :userId")
    Page<FavoriteResponse> findResponsesByUserId(@Param("userId") Long userId, Pageable pageable);

    boolean existsByUser_IdAndArtifact_Id(Long userId, Long artifactId);
}
