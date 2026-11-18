-- =====================================================================
--  Tuần 7 (Tuấn): thêm index cho analytics_db đang có dữ liệu (không cần down -v).
--  Chỉ chạy MỘT LẦN, trên DB tạo trước tuần 7:
--    Get-Content Infra/analytics_w7_indexes.sql | docker exec -i smart_heritage_mysql mysql -uroot -prootpassword123
--  DB mới (volume trống): KHÔNG chạy file này, Infra/db/07_analytics.sql đã có sẵn các index.
--  File để ngoài Infra/db vì MySQL tự chạy mọi file trong thư mục đó khi khởi tạo (trùng index sẽ dừng MySQL).
--  Chạy lần 2 báo "Duplicate key name" là index đã có, không ảnh hưởng dữ liệu.
-- =====================================================================
USE analytics_db;

-- countVisitorsByLanguage (DISTINCT user_id) và lọc theo artifact khi Dashboard chọn di tích
ALTER TABLE fact_visits
    ADD KEY idx_fact_visits_user_date     (user_id, detected_at),
    ADD KEY idx_fact_visits_artifact_date (artifact_id, detected_at);

-- findRecentFeedbacks (ORDER BY created_at) và lọc ngày theo di tích
ALTER TABLE fact_feedbacks
    ADD KEY idx_fact_feedbacks_created_at    (created_at),
    ADD KEY idx_fact_feedbacks_artifact_date (artifact_id, created_at);
