package com.smartheritage.personalization.repository;

import com.smartheritage.personalization.entity.Favorite;
import com.smartheritage.personalization.entity.FavoriteId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, FavoriteId> {
    Page<Favorite> findByIdUserIdOrderBySavedAtDesc(Long userId, Pageable pageable);
}
