package com.smartheritage.feedback.service;

import com.smartheritage.common.web.ApiException;
import com.smartheritage.feedback.client.ArtifactClient;
import com.smartheritage.feedback.client.UserProfileClient;
import com.smartheritage.feedback.dto.FeedbackRequest;
import com.smartheritage.feedback.dto.FeedbackResponse;
import com.smartheritage.feedback.dto.RatingSummaryResponse;
import com.smartheritage.feedback.entity.Feedback;
import com.smartheritage.feedback.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Nghiệp vụ đánh giá (số sao và bình luận) của du khách cho hiện vật.
 * Chuyển từ Backend/.../service/FeedbackService.java (tuần 3), khác ở chỗ:
 * <ul>
 *   <li>Không dùng ArtifactRepository / UserRepository: hỏi content-service qua ArtifactClient,
 *       tên người gửi lấy từ auth-service qua UserProfileClient rồi lưu vào reviewerName.</li>
 *   <li>Lỗi ném ApiException (GlobalExceptionHandler của common chuyển thành JSON đúng mã HTTP):
 *       400 dữ liệu sai, 404 không tìm thấy, 503 content-service không phản hồi.</li>
 * </ul>
 * Người gửi (userId) do Controller lấy từ header X-User-Id (CurrentUser.requireUserId()), KHÔNG nhận từ body.
 * Quyền (VISITOR/ADMIN) do Gateway kiểm tra, không kiểm tra lại ở đây.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class FeedbackService {

    static final String STATUS_PENDING = "PENDING";
    static final String STATUS_APPROVED = "APPROVED";
    static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final Set<String> ALL_STATUSES = Set.of(STATUS_PENDING, STATUS_APPROVED, STATUS_REJECTED);

    private static final int MIN_RATING = 1;
    private static final int MAX_RATING = 5;
    private static final int MAX_COMMENT_LENGTH = 500;
    static final String DEFAULT_REVIEWER_NAME = "Khách";

    private final FeedbackRepository feedbackRepository;
    private final ArtifactClient artifactClient;
    private final UserProfileClient userProfileClient;

    // ──────────────────────────── CREATE ───────────────────────────

    /**
     * Gửi đánh giá cho một hiện vật. Feedback mới luôn có trạng thái PENDING.
     * Thứ tự kiểm tra: dữ liệu request (400) → hiện vật không tồn tại (404)
     * → hiện vật không ACTIVE (400). content-service không phản hồi → 503.
     *
     * @param userId ID người gửi, lấy từ header X-User-Id do Gateway gắn (không phải từ body)
     */
    public FeedbackResponse createFeedback(Long artifactId, Long userId, FeedbackRequest request) {
        // 1. Kiểm tra dữ liệu request
        validateArtifactId(artifactId);
        validateUserId(userId);
        if (request == null) {
            throw ApiException.badRequest("Dữ liệu đánh giá không được để trống");
        }
        int rating = validateAndConvertRating(request.getRating());
        String comment = normalizeComment(request.getComment());

        // 2 + 3. Hiện vật phải tồn tại và đang hoạt động
        if (!isActive(artifactClient.getArtifactStatus(artifactId))) {
            throw ApiException.badRequest(
                    "Hiện vật ID " + artifactId + " không hoạt động, không thể gửi đánh giá");
        }

        // 4. Chụp lại tên hiển thị lúc gửi (lỗi thì để null -> "Khách")
        String reviewerName = userProfileClient.findCurrentUserFullName().orElse(null);

        Feedback feedback = Feedback.builder()
                .userId(userId)
                .artifactId(artifactId)
                .reviewerName(reviewerName)
                .rating(rating)
                .comment(comment)
                .status(STATUS_PENDING)
                .build();
        feedbackRepository.save(feedback);
        return toResponse(feedback);
    }

    // ──────────────────────────── READ ────────────────────────────

    /**
     * Các đánh giá ĐÃ ĐƯỢC DUYỆT của một hiện vật, mới nhất trước.
     * Hiện vật không tồn tại hoặc không ACTIVE thì coi như không tìm thấy (404), giữ đúng hành vi tuần 3.
     */
    @Transactional(readOnly = true)
    public List<FeedbackResponse> getApprovedFeedbacks(Long artifactId) {
        validateArtifactId(artifactId);
        if (!isActive(artifactClient.getArtifactStatus(artifactId))) {
            throw ApiException.notFound("Không tìm thấy hiện vật với ID: " + artifactId);
        }
        return feedbackRepository
                .findByArtifactIdAndStatusOrderByCreatedAtDescIdDesc(artifactId, STATUS_APPROVED)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Điểm trung bình và số lượt theo từng mức sao, chỉ tính đánh giá APPROVED.
     * Không gọi content-service: hiện vật chưa có đánh giá (hoặc không tồn tại) trả totalRatings = 0.
     */
    @Transactional(readOnly = true)
    public RatingSummaryResponse getRatingSummary(Long artifactId) {
        validateArtifactId(artifactId);
        Map<Integer, Long> distribution = new LinkedHashMap<>();
        for (int star = MAX_RATING; star >= MIN_RATING; star--) {
            distribution.put(star, 0L);
        }
        long total = 0;
        long sum = 0;
        for (Object[] row : feedbackRepository.countByRating(artifactId, STATUS_APPROVED)) {
            int star = ((Number) row[0]).intValue();
            long count = ((Number) row[1]).longValue();
            distribution.merge(star, count, Long::sum);
            total += count;
            sum += star * count;
        }
        Double average = total == 0 ? null
                : BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(total), 1, RoundingMode.HALF_UP).doubleValue();
        return RatingSummaryResponse.builder()
                .artifactId(artifactId)
                .averageRating(average)
                .totalRatings(total)
                .distribution(distribution)
                .build();
    }

    // ──────────────────────────── ADMIN ───────────────────────────

    /**
     * ADMIN xem danh sách đánh giá, mới nhất trước.
     * status rỗng/null → tất cả; PENDING | APPROVED | REJECTED (không phân biệt hoa thường) → lọc; giá trị khác → 400.
     */
    @Transactional(readOnly = true)
    public List<FeedbackResponse> getFeedbacksForAdmin(String status) {
        List<Feedback> feedbacks;
        if (status == null || status.isBlank()) {
            feedbacks = feedbackRepository.findAllByOrderByCreatedAtDescIdDesc();
        } else {
            String normalized = status.trim().toUpperCase(Locale.ROOT);
            if (!ALL_STATUSES.contains(normalized)) {
                throw ApiException.badRequest("Trạng thái chỉ nhận PENDING, APPROVED hoặc REJECTED");
            }
            feedbacks = feedbackRepository.findByStatusOrderByCreatedAtDescIdDesc(normalized);
        }
        return feedbacks.stream().map(this::toResponse).toList();
    }

    /**
     * ADMIN duyệt hoặc từ chối một đánh giá. Chỉ nhận APPROVED hoặc REJECTED
     * (không đặt lại PENDING). Đặt lại đúng trạng thái hiện tại vẫn thành công.
     */
    public FeedbackResponse updateStatus(Long feedbackId, String status) {
        if (feedbackId == null) {
            throw ApiException.badRequest("ID đánh giá không hợp lệ");
        }
        String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (!STATUS_APPROVED.equals(normalized) && !STATUS_REJECTED.equals(normalized)) {
            throw ApiException.badRequest("Trạng thái chỉ nhận APPROVED hoặc REJECTED");
        }
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> ApiException.notFound("Không tìm thấy đánh giá với ID: " + feedbackId));
        feedback.setStatus(normalized);
        feedbackRepository.save(feedback);
        return toResponse(feedback);
    }

    // ─────────────────────── PRIVATE HELPERS ───────────────────────

    private void validateArtifactId(Long artifactId) {
        if (artifactId == null || artifactId <= 0) {
            throw ApiException.badRequest("ID hiện vật không hợp lệ");
        }
    }

    /** Kiểm tra phòng thủ: userId đến từ header Gateway gắn nên bình thường luôn hợp lệ. */
    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw ApiException.badRequest("userId không hợp lệ");
        }
    }

    /** Chỉ chấp nhận số nguyên từ 1 đến 5; từ chối thiếu, NaN, vô cực và số thập phân (ví dụ 4.5). */
    private int validateAndConvertRating(Double rating) {
        if (rating == null || rating.isNaN() || rating.isInfinite()) {
            throw ApiException.badRequest("Số sao (rating) không hợp lệ");
        }
        double value = rating;
        if (value != Math.floor(value)) {
            throw ApiException.badRequest("Số sao (rating) phải là số nguyên");
        }
        if (value < MIN_RATING || value > MAX_RATING) {
            throw ApiException.badRequest(
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
            throw ApiException.badRequest("Bình luận tối đa " + MAX_COMMENT_LENGTH + " ký tự");
        }
        return trimmed;
    }

    /** Hoạt động khi status sau trim bằng ACTIVE (không phân biệt hoa thường); null/rỗng/giá trị lạ đều là không hoạt động. */
    private static boolean isActive(String status) {
        return status != null && STATUS_ACTIVE.equalsIgnoreCase(status.trim());
    }

    private static String displayName(String reviewerName) {
        return reviewerName == null || reviewerName.isBlank() ? DEFAULT_REVIEWER_NAME : reviewerName.trim();
    }

    private FeedbackResponse toResponse(Feedback feedback) {
        return FeedbackResponse.builder()
                .id(feedback.getId())
                .artifactId(feedback.getArtifactId())
                .rating(feedback.getRating())
                .comment(feedback.getComment())
                .reviewerName(displayName(feedback.getReviewerName()))
                .status(feedback.getStatus())
                .createdAt(feedback.getCreatedAt())
                .build();
    }
}
