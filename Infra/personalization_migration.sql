-- Chỉ chạy MỘT LẦN trên smart_heritage_db đã được khởi tạo từ init_db.sql cũ (trước Sprint 4).
-- Database mới (docker compose down -v rồi up -d): KHÔNG chạy file này; Infra/db/99_init_db_legacy.sql và
-- Infra/db/05_personalization.sql đã có các cột tương ứng (Tường - Sprint 4).
-- Nếu Backend (spring.jpa.hibernate.ddl-auto=update) đã tự thêm cột và unique index:
--   chỉ chạy câu ALTER ... ALTER COLUMN cuối cùng. Kiểm tra trước bằng: SHOW CREATE TABLE visits;

ALTER TABLE visits
    ADD COLUMN client_event_id VARCHAR(36) NULL AFTER duration_seconds,
    ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'MANUAL' AFTER client_event_id;

CREATE UNIQUE INDEX uq_visits_user_event ON visits (user_id, client_event_id);

-- duration_seconds: bỏ mặc định 0 (NULL = không đo được thời gian dừng)
ALTER TABLE visits ALTER COLUMN duration_seconds DROP DEFAULT;
