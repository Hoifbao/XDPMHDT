package com.smartheritage.backend.repository;

import com.smartheritage.backend.dto.StatisticsResponse.DailyViews;
import com.smartheritage.backend.dto.StatisticsResponse.LanguageCount;
import com.smartheritage.backend.dto.StatisticsResponse.RecentFeedback;
import com.smartheritage.backend.dto.StatisticsResponse.TopArtifact;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * Các câu query thống kê (Tuấn), viết SQL native bằng JdbcTemplate:
 * gom một chỗ để dễ đối chiếu với Infra/init_db.sql
 * và không phải sửa repository của các bạn khác.
 */
@Repository
@RequiredArgsConstructor
public class StatisticsRepository {

    private final JdbcTemplate jdbcTemplate;

    /** Trả về total_views, total_visitors, avg_duration (NULL khi chưa có lượt xem) */
    public Map<String, Object> countViewsAndVisitors() {
        return jdbcTemplate.queryForMap("""
                SELECT COUNT(*)                         AS total_views,
                       COUNT(DISTINCT user_id)          AS total_visitors,
                       ROUND(AVG(duration_seconds), 1)  AS avg_duration
                FROM visits
                """);
    }

    /** Lấy `days` ngày gần nhất có lượt xem rồi xếp lại ngày cũ trước để vẽ biểu đồ */
    public List<DailyViews> countDailyViews(int days) {
        return jdbcTemplate.query("""
                SELECT day, views FROM (
                    SELECT DATE(detected_at) AS day, COUNT(*) AS views
                    FROM visits
                    GROUP BY DATE(detected_at)
                    ORDER BY day DESC
                    LIMIT ?
                ) recent
                ORDER BY day ASC
                """,
                (rs, i) -> DailyViews.builder()
                        .date(rs.getDate("day").toLocalDate())
                        .views(rs.getLong("views"))
                        .build(),
                days);
    }

    public List<RecentFeedback> findRecentFeedbacks(int limit) {
        return jdbcTemplate.query("""
                SELECT f.id, f.created_at, a.name AS artifact_name, f.rating, f.comment, f.status
                FROM feedbacks f
                JOIN artifacts a ON a.id = f.artifact_id
                ORDER BY f.created_at DESC, f.id DESC
                LIMIT ?
                """,
                (rs, i) -> RecentFeedback.builder()
                        .id(rs.getLong("id"))
                        .createdAt(rs.getTimestamp("created_at") == null ? null
                                : rs.getTimestamp("created_at").toLocalDateTime())
                        .artifactName(rs.getString("artifact_name"))
                        .rating(rs.getInt("rating"))
                        .comment(rs.getString("comment"))
                        .status(rs.getString("status"))
                        .build(),
                limit);
    }

    public List<TopArtifact> findTopArtifacts(int limit) {
        return jdbcTemplate.query("""
                SELECT a.id, a.artifact_code, a.name, s.id AS site_id, s.name AS site_name,
                       COUNT(v.id) AS views
                FROM visits v
                JOIN artifacts a      ON a.id = v.artifact_id
                JOIN heritage_sites s ON s.id = a.site_id
                GROUP BY a.id, a.artifact_code, a.name, s.id, s.name
                ORDER BY views DESC, a.id ASC
                LIMIT ?
                """,
                (rs, i) -> TopArtifact.builder()
                        .artifactId(rs.getLong("id"))
                        .artifactCode(rs.getString("artifact_code"))
                        .artifactName(rs.getString("name"))
                        .siteId(rs.getLong("site_id"))
                        .siteName(rs.getString("site_name"))
                        .views(rs.getLong("views"))
                        .build(),
                limit);
    }

    public List<LanguageCount> countVisitorsByLanguage() {
        return jdbcTemplate.query("""
                SELECT COALESCE(u.preferred_language, 'unknown') AS language_code,
                       COUNT(*) AS visitors
                FROM users u
                WHERE u.id IN (SELECT DISTINCT user_id FROM visits)
                GROUP BY language_code
                ORDER BY visitors DESC, language_code ASC
                """,
                (rs, i) -> LanguageCount.builder()
                        .languageCode(rs.getString("language_code"))
                        .visitors(rs.getLong("visitors"))
                        .build());
    }

    /** Trả về total_count, avg_all, approved_count, avg_approved (avg là NULL khi chưa có feedback) */
    public Map<String, Object> summarizeRatings() {
        return jdbcTemplate.queryForMap("""
                SELECT COUNT(*)                                                     AS total_count,
                       ROUND(AVG(rating), 2)                                        AS avg_all,
                       COALESCE(SUM(status = 'APPROVED'), 0)                        AS approved_count,
                       ROUND(AVG(CASE WHEN status = 'APPROVED' THEN rating END), 2) AS avg_approved
                FROM feedbacks
                """);
    }
}
