package com.smartheritage.backend.service;

import com.smartheritage.backend.dto.StatisticsResponse;
import com.smartheritage.backend.repository.StatisticsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Thống kê cho Dashboard (Tuấn): lượt xem, số khách, top hiện vật,
 * phân bố ngôn ngữ, điểm đánh giá trung bình, thời gian dừng TB,
 * lượt xem theo ngày và phản hồi gần đây.
 */
@Service
@RequiredArgsConstructor
public class StatisticsService {

    public static final int MAX_TOP = 50;
    private static final int DAILY_DAYS = 7;
    private static final int RECENT_FEEDBACKS = 6;

    private final StatisticsRepository statisticsRepository;

    @Transactional(readOnly = true)
    public StatisticsResponse getOverview(int top) {
        if (top < 1 || top > MAX_TOP) {
            throw new IllegalArgumentException("top phải trong khoảng 1 - " + MAX_TOP);
        }

        Map<String, Object> visits = statisticsRepository.countViewsAndVisitors();
        Map<String, Object> ratings = statisticsRepository.summarizeRatings();

        return StatisticsResponse.builder()
                .totalViews(toLong(visits.get("total_views")))
                .totalVisitors(toLong(visits.get("total_visitors")))
                .topArtifacts(statisticsRepository.findTopArtifacts(top))
                .languageDistribution(statisticsRepository.countVisitorsByLanguage())
                .averageRatingApproved(toDouble(ratings.get("avg_approved")))
                .approvedFeedbackCount(toLong(ratings.get("approved_count")))
                .averageRatingAll(toDouble(ratings.get("avg_all")))
                .totalFeedbackCount(toLong(ratings.get("total_count")))
                .averageDurationSeconds(toDouble(visits.get("avg_duration")))
                .dailyViews(statisticsRepository.countDailyViews(DAILY_DAYS))
                .recentFeedbacks(statisticsRepository.findRecentFeedbacks(RECENT_FEEDBACKS))
                .build();
    }

    // MySQL trả COUNT/SUM/AVG dạng Long hoặc BigDecimal tuỳ hàm, nên đọc qua Number
    private static long toLong(Object value) {
        return value == null ? 0 : ((Number) value).longValue();
    }

    private static Double toDouble(Object value) {
        return value == null ? null : ((Number) value).doubleValue();
    }
}
