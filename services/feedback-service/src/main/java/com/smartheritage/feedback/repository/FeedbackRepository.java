package com.smartheritage.feedback.repository;

import com.smartheritage.feedback.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository -> Feedback. Mọi danh sách đều mới nhất trước (createdAt giảm dần, trùng thì id giảm dần).
 */
@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    /** Đánh giá cũ của người dùng cho hiện vật (tối đa 1 nhờ uq_feedbacks_user_artifact). */
    Optional<Feedback> findByUserIdAndArtifactId(Long userId, Long artifactId);

    /** Đánh giá của một hiện vật theo trạng thái (Visitor xem: status = APPROVED). */
    List<Feedback> findByArtifactIdAndStatusOrderByCreatedAtDescIdDesc(Long artifactId, String status);

    /** ADMIN lọc theo trạng thái. */
    List<Feedback> findByStatusOrderByCreatedAtDescIdDesc(String status);

    /** ADMIN xem tất cả. */
    List<Feedback> findAllByOrderByCreatedAtDescIdDesc();

    /** Số lượt theo từng mức sao của một hiện vật: mỗi phần tử là [rating (Integer), số lượt (Long)]. */
    @Query("SELECT f.rating, COUNT(f) FROM Feedback f "
            + "WHERE f.artifactId = :artifactId AND f.status = :status GROUP BY f.rating")
    List<Object[]> countByRating(@Param("artifactId") Long artifactId, @Param("status") String status);
}
