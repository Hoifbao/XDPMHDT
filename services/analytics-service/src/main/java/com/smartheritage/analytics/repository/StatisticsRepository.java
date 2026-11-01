package com.smartheritage.analytics.repository;

import com.smartheritage.analytics.dto.StatisticsResponse.DailyViews;
import com.smartheritage.analytics.dto.StatisticsResponse.LanguageCount;
import com.smartheritage.analytics.dto.StatisticsResponse.RecentFeedback;
import com.smartheritage.analytics.dto.StatisticsResponse.TopArtifact;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Các câu query thống kê (Tuấn), SQL native bằng JdbcTemplate, chỉ đọc analytics_db:
 *   fact_visits, fact_feedbacks (NiFi chép mỗi 1 phút), dim_artifact, dim_site, dim_user_lang (mỗi 5 phút).
 * Không JOIN sang DB của service khác.
 */
@Repository
@RequiredArgsConstructor
public class StatisticsRepository {

    private final JdbcTemplate jdbcTemplate;

    /** Bộ lọc của Dashboard; trường nào null thì không lọc theo trường đó. from, to tính cả 2 đầu. */
    public record Filter(Long siteId, LocalDate from, LocalDate to) {
    }

    /** Trả về total_views, total_visitors, avg_duration (NULL khi chưa có lượt xem) */
    public Map<String, Object> countViewsAndVisitors(Filter filter) {
        List<Object> args = new ArrayList<>();
        return jdbcTemplate.queryForMap("""
                SELECT COUNT(*)                          AS total_views,
                       COUNT(DISTINCT v.user_id)         AS total_visitors,
                       ROUND(AVG(v.duration_seconds), 1) AS avg_duration
                """ + visitsFrom(filter, args), args.toArray());
    }

    /** Lấy `days` ngày gần nhất có lượt xem rồi xếp lại ngày cũ trước để vẽ biểu đồ */
    public List<DailyViews> countDailyViews(Filter filter, int days) {
        List<Object> args = new ArrayList<>();
        String from = visitsFrom(filter, args);
        args.add(days);
        return jdbcTemplate.query("""
                SELECT day, views FROM (
                    SELECT DATE(v.detected_at) AS day, COUNT(*) AS views
                """ + from + """
                    GROUP BY DATE(v.detected_at)
                    ORDER BY day DESC
                    LIMIT ?
                ) recent
                ORDER BY day ASC
                """,
                (rs, i) -> DailyViews.builder()
                        .date(rs.getDate("day").toLocalDate())
                        .views(rs.getLong("views"))
                        .build(),
                args.toArray());
    }

    public List<TopArtifact> findTopArtifacts(Filter filter, int limit) {
        List<Object> args = new ArrayList<>();
        String from = visitsFrom(filter, args);
        args.add(limit);
        return jdbcTemplate.query("""
                SELECT v.artifact_id, a.code, a.name, a.site_id, s.name AS site_name,
                       COUNT(*) AS views
                """ + from + """
                GROUP BY v.artifact_id, a.code, a.name, a.site_id, s.name
                ORDER BY views DESC, v.artifact_id ASC
                LIMIT ?
                """,
                (rs, i) -> TopArtifact.builder()
                        .artifactId(rs.getLong("artifact_id"))
                        .artifactCode(rs.getString("code"))
                        .artifactName(rs.getString("name"))
                        .siteId(rs.getObject("site_id", Long.class))
                        .siteName(rs.getString("site_name"))
                        .views(rs.getLong("views"))
                        .build(),
                args.toArray());
    }

    /** Số du khách khác nhau theo ngôn ngữ (dim_user_lang); user chưa đồng bộ thì là 'unknown' */
    public List<LanguageCount> countVisitorsByLanguage(Filter filter) {
        List<Object> args = new ArrayList<>();
        return jdbcTemplate.query("""
                SELECT COALESCE(u.language_code, 'unknown') AS language_code,
                       COUNT(DISTINCT v.user_id)            AS visitors
                """ + visitsFrom(filter, args) + """
                GROUP BY language_code
                ORDER BY visitors DESC, language_code ASC
                """,
                (rs, i) -> LanguageCount.builder()
                        .languageCode(rs.getString("language_code"))
                        .visitors(rs.getLong("visitors"))
                        .build(),
                args.toArray());
    }

    /** Trả về total_count, avg_all, approved_count, avg_approved (avg là NULL khi chưa có feedback) */
    public Map<String, Object> summarizeRatings(Filter filter) {
        List<Object> args = new ArrayList<>();
        return jdbcTemplate.queryForMap("""
                SELECT COUNT(*)                                                         AS total_count,
                       ROUND(AVG(f.rating), 2)                                          AS avg_all,
                       COALESCE(SUM(f.status = 'APPROVED'), 0)                          AS approved_count,
                       ROUND(AVG(CASE WHEN f.status = 'APPROVED' THEN f.rating END), 2) AS avg_approved
                """ + feedbacksFrom(filter, args), args.toArray());
    }

    /** fact_feedbacks không có cột comment nên comment luôn null (Dashboard hiện "—") */
    public List<RecentFeedback> findRecentFeedbacks(Filter filter, int limit) {
        List<Object> args = new ArrayList<>();
        String from = feedbacksFrom(filter, args);
        args.add(limit);
        return jdbcTemplate.query("""
                SELECT f.feedback_id, f.created_at, a.name AS artifact_name, f.rating, f.status
                """ + from + """
                ORDER BY f.created_at DESC, f.feedback_id DESC
                LIMIT ?
                """,
                (rs, i) -> RecentFeedback.builder()
                        .id(rs.getLong("feedback_id"))
                        .createdAt(rs.getTimestamp("created_at") == null ? null
                                : rs.getTimestamp("created_at").toLocalDateTime())
                        .artifactName(rs.getString("artifact_name"))
                        .rating(rs.getInt("rating"))
                        .status(rs.getString("status"))
                        .build(),
                args.toArray());
    }

    // FROM + WHERE dùng chung cho mọi query lượt xem; tham số được thêm vào args theo đúng thứ tự dấu ?
    private static String visitsFrom(Filter filter, List<Object> args) {
        return """
                FROM fact_visits v
                LEFT JOIN dim_artifact  a ON a.artifact_id = v.artifact_id
                LEFT JOIN dim_site      s ON s.site_id = a.site_id
                LEFT JOIN dim_user_lang u ON u.user_id = v.user_id
                """ + where(filter, "v.detected_at", args);
    }

    private static String feedbacksFrom(Filter filter, List<Object> args) {
        return """
                FROM fact_feedbacks f
                LEFT JOIN dim_artifact a ON a.artifact_id = f.artifact_id
                """ + where(filter, "f.created_at", args);
    }

    private static String where(Filter filter, String timeColumn, List<Object> args) {
        StringBuilder sql = new StringBuilder("WHERE 1 = 1\n");
        if (filter.siteId() != null) {
            sql.append("AND a.site_id = ?\n");
            args.add(filter.siteId());
        }
        if (filter.from() != null) {
            sql.append("AND ").append(timeColumn).append(" >= ?\n");
            args.add(filter.from());
        }
        if (filter.to() != null) {
            // "đến ngày" tính cả ngày đó: < 00:00 của ngày hôm sau
            sql.append("AND ").append(timeColumn).append(" < ?\n");
            args.add(filter.to().plusDays(1));
        }
        return sql.toString();
    }
}
