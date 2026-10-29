package com.smartheritage.backend.service;

import com.smartheritage.backend.dto.FeedbackRequest;
import com.smartheritage.backend.dto.FeedbackResponse;
import com.smartheritage.backend.entity.Artifact;
import com.smartheritage.backend.entity.Feedback;
import com.smartheritage.backend.entity.User;
import com.smartheritage.backend.repository.ArtifactRepository;
import com.smartheritage.backend.repository.FeedbackRepository;
import com.smartheritage.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Nghiệp vụ đánh giá (số sao và bình luận) của du khách cho hiện vật.
 *
 * Quy ước lỗi (giống IBeaconService):
 *  - NoSuchElementException     : không tìm thấy dữ liệu, Controller trả 404.
 *  - IllegalArgumentException   : dữ liệu không hợp lệ, Controller trả 400.
 *
 * Người gửi (userId) do Controller lấy từ access token đã xác thực
 * (SecurityUtils.currentUserId()) rồi truyền vào, KHÔNG nhận từ body request.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class FeedbackService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_ACTIVE = "ACTIVE";

    private static final int MIN_RATING = 1;
    private static final int MAX_RATING = 5;
    private static final int MAX_COMMENT_LENGTH = 500;
    private static final String DEFAULT_REVIEWER_NAME = "Khách";

    private final FeedbackRepository feedbackRepository;
    private final ArtifactRepository artifactRepository;
    private final UserRepository userRepository;

    // ──────────────────────────── CREATE ───────────────────────────

    /**
     * Gửi đánh giá cho một hiện vật. Feedback mới luôn có trạng thái PENDING.
     * Thứ tự kiểm tra: dữ liệu request (400) → hiện vật không tồn tại (404)
     * → hiện vật không ACTIVE (400) → người dùng không tồn tại (404).
     *
     * @param userId ID người gửi, lấy từ token đã xác thực (không phải từ body)
     */
    public FeedbackResponse createFeedback(Long artifactId, Long userId, FeedbackRequest request) {
        // 1. Kiểm tra dữ liệu request
        validateArtifactId(artifactId);
        validateUserId(userId);
        if (request == null) {
            throw new IllegalArgumentException("Dữ liệu đánh giá không được để trống");
        }
        int rating = validateAndConvertRating(request.getRating());
        String comment = normalizeComment(request.getComment());

        // 2. Hiện vật phải tồn tại
        Artifact artifact = findArtifactOrThrow(artifactId);

        // 3. Hiện vật phải đang hoạt động
        if (!isActive(artifact)) {
            throw new IllegalArgumentException(
                    "Hiện vật ID " + artifactId + " không hoạt động, không thể gửi đánh giá");
        }

        // 4. Người dùng (từ token) phải còn tồn tại
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Không tìm thấy người dùng với ID: " + userId));

        Feedback feedback = Feedback.builder()
                .user(user)
                .artifactId(artifactId)
                .rating(rating)
                .comment(comment)
                .status(STATUS_PENDING)
                .build();
        feedbackRepository.save(feedback);
        return toResponse(feedback);
    }

    // ──────────────────────────── READ ────────────────────────────

    /**
     * Lấy các đánh giá ĐÃ ĐƯỢC DUYỆT của một hiện vật, mới nhất trước
     * (createdAt giảm dần, nếu trùng thì id giảm dần).
     * Hiện vật không tồn tại hoặc không ACTIVE thì coi như không tìm thấy (404).
     */
    @Transactional(readOnly = true)
    public List<FeedbackResponse> getApprovedFeedbacks(Long artifactId) {
        validateArtifactId(artifactId);
        Artifact artifact = findArtifactOrThrow(artifactId);
        if (!isActive(artifact)) {
            throw new NoSuchElementException("Không tìm thấy hiện vật với ID: " + artifactId);
        }
        return feedbackRepository
                .findByArtifactIdAndStatusOrderByCreatedAtDescIdDesc(artifactId, STATUS_APPROVED)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // ─────────────────────── PRIVATE HELPERS ───────────────────────

    private void validateArtifactId(Long artifactId) {
        if (artifactId == null) {
            throw new IllegalArgumentException("ID hiện vật không hợp lệ");
        }
    }

    /** Kiểm tra phòng thủ: userId đến từ token nên bình thường luôn hợp lệ. */
    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("userId không hợp lệ");
        }
    }

    /** Chỉ chấp nhận số nguyên từ 1 đến 5; từ chối thiếu, NaN, vô cực và số thập phân (ví dụ 4.5). */
    private int validateAndConvertRating(Double rating) {
        if (rating == null || rating.isNaN() || rating.isInfinite()) {
            throw new IllegalArgumentException("Số sao (rating) không hợp lệ");
        }
        double value = rating;
        if (value != Math.floor(value)) {
            throw new IllegalArgumentException("Số sao (rating) phải là số nguyên");
        }
        if (value < MIN_RATING || value > MAX_RATING) {
            throw new IllegalArgumentException(
                    "Số sao (rating) phải nằm trong khoảng " + MIN_RATING + "–" + MAX_RATING);
        }
        return (int) value;
    }

    /** Trim bình luận; rỗng sau trim thì lưu null; tối đa 500 ký tự. */
    private String normalizeComment(String comment) {
        if (comment == null) {
            return null;
        }
        String trimmed = comment.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > MAX_COMMENT_LENGTH) {
            throw new IllegalArgumentException(
                    "Bình luận tối đa " + MAX_COMMENT_LENGTH + " ký tự");
        }
        return trimmed;
    }

    private Artifact findArtifactOrThrow(Long artifactId) {
        return artifactRepository.findById(artifactId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Không tìm thấy hiện vật với ID: " + artifactId));
    }

    /** Hoạt động khi status sau trim bằng ACTIVE (không phân biệt hoa thường); null/rỗng/giá trị lạ đều là không hoạt động. */
    private boolean isActive(Artifact artifact) {
        String status = artifact.getStatus();
        return status != null && STATUS_ACTIVE.equalsIgnoreCase(status.trim());
    }

    /** Tên hiển thị: fullName đã trim; null hoặc rỗng thì dùng "Khách". Không dùng email/username. */
    private String resolveReviewerName(User user) {
        String fullName = user != null ? user.getFullName() : null;
        if (fullName == null || fullName.trim().isEmpty()) {
            return DEFAULT_REVIEWER_NAME;
        }
        return fullName.trim();
    }

    private FeedbackResponse toResponse(Feedback feedback) {
        return FeedbackResponse.builder()
                .id(feedback.getId())
                .artifactId(feedback.getArtifactId())
                .rating(feedback.getRating())
                .comment(feedback.getComment())
                .reviewerName(resolveReviewerName(feedback.getUser()))
                .status(feedback.getStatus())
                .createdAt(feedback.getCreatedAt())
                .build();
    }
}