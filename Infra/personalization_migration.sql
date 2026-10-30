-- Chỉ chạy MỘT LẦN trên smart_heritage_db đã được khởi tạo từ init_db.sql cũ (trước Sprint 4).
-- Database mới (down -v rồi up -d): không chạy file này; Infra/db/99_init_db_legacy.sql và
-- Infra/db/05_personalization.sql đã có các cột tương ứng (Tường - Sprint 4).
-- Nếu Spring Boot ddl-auto=update đã tự thêm cột, kiểm tra SHOW COLUMNS trước khi chạy.

ALTER TABLE visits
    ADD COLUMN client_event_id CHAR(36) NULL AFTER duration_seconds,
    ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'MANUAL' AFTER client_event_id;

CREATE UNIQUE INDEX uq_visits_user_event ON visits (user_id, client_event_id);
