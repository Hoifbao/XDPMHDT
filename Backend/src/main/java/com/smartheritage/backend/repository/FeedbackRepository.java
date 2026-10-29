package com.smartheritage.backend.repository;

import com.smartheritage.backend.entity.Feedback;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository -> Feedback (đánh giá và bình luận của du khách về hiện vật).
 */
@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    /**
     * Danh sách feedback của một hiện vật theo trạng thái, mới nhất trước:
     * - Sắp xếp theo createdAt giảm dần, nếu trùng thì theo id giảm dần.
     * - Lấy kèm user (EntityGraph) để đọc tên người đánh giá mà không phát sinh.
     * - Thêm truy vấn hay lỗi (lazy loading).
     * - Truyền status = "APPROVED" để chỉ lấy các feedback đã được duyệt.
     */
    @EntityGraph(attributePaths = "user")
    List<Feedback> findByArtifactIdAndStatusOrderByCreatedAtDescIdDesc(Long artifactId, String status);
}