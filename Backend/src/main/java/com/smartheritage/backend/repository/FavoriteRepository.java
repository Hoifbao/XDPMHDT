package com.smartheritage.backend.repository;

import com.smartheritage.backend.entity.Favorite;
import com.smartheritage.backend.entity.FavoriteId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, FavoriteId> {

    /** Danh sách yêu thích của một user, mới nhất trước */
    List<Favorite> findByUser_IdOrderBySavedAtDesc(Long userId);
}
