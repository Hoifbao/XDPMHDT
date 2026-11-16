package com.smartheritage.auth.repository;

import com.smartheritage.auth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    /**
     * Bảo W7: danh sách cho trang quản lý người dùng.
     * q (tìm trong username, email, họ tên, không phân biệt hoa thường) và role đều có thể null = bỏ qua.
     */
    @Query("SELECT u FROM User u WHERE "
            + "(:q IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :q, '%'))) "
            + "AND (:role IS NULL OR u.role = :role)")
    Page<User> search(@Param("q") String q, @Param("role") String role, Pageable pageable);
}
