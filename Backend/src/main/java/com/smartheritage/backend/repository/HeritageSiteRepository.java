package com.smartheritage.backend.repository;

import com.smartheritage.backend.entity.HeritageSite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HeritageSiteRepository extends JpaRepository<HeritageSite, Long> {

    /** Kiểm tra tên di tích đã tồn tại chưa (dùng khi tạo mới). */
    boolean existsByName(String name);

    /** Tìm kiếm di tích theo tên (không phân biệt hoa thường). */
    List<HeritageSite> findByNameContainingIgnoreCase(String keyword);
}
