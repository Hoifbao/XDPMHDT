package com.smartheritage.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Số liệu tổng quan cho Dashboard Thống kê (Tuấn).
 * Lượt xem = số dòng bảng visits (mỗi lần app quét được iBeacon của hiện vật).
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StatisticsResponse {

    /** COUNT(*) bảng visits */
    private long totalViews;

    /** Số user khác nhau đã có lượt xem (DISTINCT visits.user_id) */
    private long totalVisitors;

    /** Hiện vật xem nhiều nhất: nhiều lượt trước, bằng lượt thì id nhỏ trước */
    private List<TopArtifact> topArtifacts;

    /** preferred_language của các khách đã có lượt xem */
    private List<LanguageCount> languageDistribution;

    /** Điểm TB chỉ tính feedback APPROVED, làm tròn 2 chữ số; null nếu chưa có */
    private Double averageRatingApproved;
    private long approvedFeedbackCount;

    /** Điểm TB tính mọi trạng thái (PENDING, APPROVED, REJECTED); null nếu chưa có */
    private Double averageRatingAll;
    private long totalFeedbackCount;

    /** AVG(visits.duration_seconds), làm tròn 1 chữ số; null nếu chưa có lượt xem */
    private Double averageDurationSeconds;

    /** Lượt xem theo ngày: tối đa 7 ngày gần nhất CÓ dữ liệu, ngày cũ trước */
    private List<DailyViews> dailyViews;

    /** Feedback mới nhất (mọi trạng thái) cho bảng "Phản hồi gần đây" */
    private List<RecentFeedback> recentFeedbacks;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DailyViews {
        private LocalDate date;
        private long views;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RecentFeedback {
        private Long id;
        private LocalDateTime createdAt;
        private String artifactName;
        private int rating;
        private String comment;
        private String status;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TopArtifact {
        private Long artifactId;
        private String artifactCode;
        private String artifactName;
        private Long siteId;
        private String siteName;
        private long views;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LanguageCount {
        private String languageCode;
        private long visitors;
    }
}
