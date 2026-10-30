-- Chỉ chạy MỘT LẦN trên database đã được khởi tạo từ init_db.sql trước bản sửa.
-- Database mới: không chạy file này; init_db.sql đã có các cột tương ứng.
-- Nếu Spring Boot ddl-auto=update đã tự thêm cột, kiểm tra SHOW COLUMNS trước khi chạy.

ALTER TABLE visits
    ADD COLUMN client_event_id CHAR(36) NULL AFTER duration_seconds,
    ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'MANUAL' AFTER client_event_id;

CREATE UNIQUE INDEX uq_visits_user_event ON visits (user_id, client_event_id);
