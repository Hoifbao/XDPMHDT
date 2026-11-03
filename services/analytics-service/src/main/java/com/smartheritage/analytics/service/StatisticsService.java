package com.smartheritage.analytics.service;

import com.smartheritage.analytics.dto.StatisticsResponse;
import com.smartheritage.analytics.repository.StatisticsRepository;
import com.smartheritage.analytics.repository.StatisticsRepository.Filter;
import com.smartheritage.common.web.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

/**
 * Thống kê cho Dashboard (Tuấn): lượt xem, số khách, top hiện vật,
 * phân bố ngôn ngữ, điểm đánh giá trung bình, thời gian dừng TB,
 * lượt xem theo ngày và phản hồi gần đây. Lọc được theo di tích và khoảng ngày.
 */
@Service
@RequiredArgsConstructor
public class StatisticsService {

    public static final int MAX_TOP = 50;
    private static final int DAILY_DAYS = 7;
    // Có chọn khoảng ngày thì vẽ tối đa 31 ngày có dữ liệu trong khoảng đó
    private static final int DAILY_DAYS_IN_RANGE = 31;
    private static final int RECENT_FEEDBACKS = 6;

    private final StatisticsRepository statisticsRepository;

    @Transactional(readOnly = true)
    public StatisticsResponse getOverview(int top, Long siteId, LocalDate from, LocalDate to) {
        if (top < 1 || top > MAX_TOP) {
            throw ApiException.badRequest("top phải trong khoảng 1 - " + MAX_TOP);
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw ApiException.badRequest("Từ ngày phải trước hoặc bằng Đến ngày");
        }

        Filter filter = new Filter(siteId, from, to);
        int days = (from != null || to != null) ? DAILY_DAYS_IN_RANGE : DAILY_DAYS;
        Map<String, Object> visits = statisticsRepository.countViewsAndVisitors(filter);
        Map<String, Object> ratings = statisticsRepository.summarizeRatings(filter);

        return StatisticsResponse.builder()
                .totalViews(toLong(visits.get("total_views")))
                .totalVisitors(toLong(visits.get("total_visitors")))
                .topArtifacts(statisticsRepository.findTopArtifacts(filter, top))
                .languageDistribution(statisticsRepository.countVisitorsByLanguage(filter))
                .averageRatingApproved(toDouble(ratings.get("avg_approved")))
                .approvedFeedbackCount(toLong(ratings.get("approved_count")))
                .averageRatingAll(toDouble(ratings.get("avg_all")))
                .totalFeedbackCount(toLong(ratings.get("total_count")))
                .averageDurationSeconds(toDouble(visits.get("avg_duration")))
                .dailyViews(statisticsRepository.countDailyViews(filter, days))
                .recentFeedbacks(statisticsRepository.findRecentFeedbacks(filter, RECENT_FEEDBACKS))
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
