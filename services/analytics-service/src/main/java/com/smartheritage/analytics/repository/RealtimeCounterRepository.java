package com.smartheritage.analytics.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Ghi bảng đếm thời gian thực (Tuần 6, Tuấn): rt_processed_events, rt_daily_counters.
 * Chỉ phục vụ số "hôm nay"; không đụng tới fact_* của NiFi.
 */
@Repository
@RequiredArgsConstructor
public class RealtimeCounterRepository {

    private final JdbcTemplate jdbcTemplate;

    /** Ghi eventId; trả false nếu eventId đã có (tin trùng, bên gọi bỏ qua). */
    public boolean markProcessed(UUID eventId, String eventType) {
        return jdbcTemplate.update(
                "INSERT IGNORE INTO rt_processed_events (event_id, event_type) VALUES (?, ?)",
                eventId.toString(), eventType) == 1;
    }

    public void addView(LocalDate day, long artifactId) {
        jdbcTemplate.update("""
                INSERT INTO rt_daily_counters (day, artifact_id, views) VALUES (?, ?, 1)
                ON DUPLICATE KEY UPDATE views = views + 1
                """, day, artifactId);
    }

    public void addFeedback(LocalDate day, long artifactId, int rating) {
        jdbcTemplate.update("""
                INSERT INTO rt_daily_counters (day, artifact_id, feedbacks, rating_sum) VALUES (?, ?, 1, ?)
                ON DUPLICATE KEY UPDATE feedbacks = feedbacks + 1, rating_sum = rating_sum + VALUES(rating_sum)
                """, day, artifactId, rating);
    }
}
