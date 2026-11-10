-- =====================================================================
--  SmartHeritage - analytics_db (Tuấn - Sprint 4)
--  Database : analytics_db   |   Engine : MySQL 8.x
--
--  Kho dữ liệu thống kê do Apache NiFi đổ vào từ các schema nghiệp vụ.
--  Thiết kế: Tuấn (Sprint 4, nhánh feature/tuan-w4-nifi). Tuần 5 thêm dim_site, dim_user_lang.
--  Ghép vào Infra/db: Tường - Sprint 4. Sửa bảng thì gửi Tường cập nhật file này.
--
--  Quy ước:
--   - Không có khóa ngoại sang schema khác (mỗi service một DB riêng).
--     Các cột *_id giữ nguyên id của schema nguồn.
--   - Khóa chính fact_* = id của bảng nguồn => NiFi chạy lại không nhân đôi dòng.
--   - User MySQL riêng: analytics_user (tạo trong 00_create_schemas.sql).
--   - Docker tự chạy file này khi volume MySQL trống. Chạy tay trên DB đang có:
--       docker exec -i smart_heritage_mysql mysql -uroot -prootpassword123 < Infra/db/07_analytics.sql
-- =====================================================================

CREATE DATABASE IF NOT EXISTS analytics_db
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE analytics_db;
SET NAMES utf8mb4;

-- Mỗi lượt du khách đứng gần hiện vật (nguồn: visits + artifacts + users)
CREATE TABLE IF NOT EXISTS fact_visits (
    visit_id         BIGINT      NOT NULL,             -- = visits.id
    user_id          BIGINT      NOT NULL,
    artifact_id      BIGINT      NOT NULL,
    site_id          BIGINT      NULL,                 -- artifacts.site_id
    language_code    VARCHAR(10) NULL,                 -- users.preferred_language
    detected_at      DATETIME    NOT NULL,
    duration_seconds INT         NULL,
    synced_at        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),  -- lúc NiFi ghi vào
    PRIMARY KEY (visit_id),
    KEY idx_fact_visits_detected_at (detected_at),
    KEY idx_fact_visits_artifact (artifact_id),
    KEY idx_fact_visits_site (site_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Mỗi đánh giá của du khách (nguồn: feedbacks)
CREATE TABLE IF NOT EXISTS fact_feedbacks (
    feedback_id BIGINT      NOT NULL,                  -- = feedbacks.id
    artifact_id BIGINT      NOT NULL,
    rating      INT         NOT NULL,
    status      VARCHAR(20) NOT NULL,                  -- PENDING | APPROVED | REJECTED
    created_at  DATETIME(6) NULL,
    PRIMARY KEY (feedback_id),
    KEY idx_fact_feedbacks_artifact (artifact_id),
    CONSTRAINT chk_fact_feedbacks_rating CHECK (rating BETWEEN 1 AND 5)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Thông tin hiện vật để hiển thị trên Dashboard (nguồn: artifacts + heritage_sites)
CREATE TABLE IF NOT EXISTS dim_artifact (
    artifact_id BIGINT       NOT NULL,                 -- = artifacts.id
    code        VARCHAR(50)  NULL,                     -- artifacts.artifact_code
    name        VARCHAR(200) NOT NULL,
    site_id     BIGINT       NULL,
    site_name   VARCHAR(150) NULL,                     -- heritage_sites.name
    PRIMARY KEY (artifact_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Tuần 5 (Tuấn): thông tin di tích để lọc Dashboard theo di tích (nguồn: site_db.heritage_sites)
CREATE TABLE IF NOT EXISTS dim_site (
    site_id   BIGINT        NOT NULL,                 -- = heritage_sites.id
    name      VARCHAR(150)  NOT NULL,
    location  VARCHAR(255)  NULL,
    latitude  DECIMAL(10,7) NULL,
    longitude DECIMAL(10,7) NULL,
    PRIMARY KEY (site_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Tuần 5 (Tuấn): ngôn ngữ của du khách để thống kê theo ngôn ngữ
-- (nguồn: users, chỉ id + preferred_language; không chép email, mật khẩu)
CREATE TABLE IF NOT EXISTS dim_user_lang (
    user_id       BIGINT      NOT NULL,               -- = users.id
    language_code VARCHAR(10) NOT NULL DEFAULT 'vi',  -- users.preferred_language
    PRIMARY KEY (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Tổng hợp theo ngày cho biểu đồ lượt xem (tính từ fact_visits)
CREATE TABLE IF NOT EXISTS agg_daily_views (
    day      DATE NOT NULL,
    views    INT  NOT NULL DEFAULT 0,                  -- số lượt (COUNT visit_id)
    visitors INT  NOT NULL DEFAULT 0,                  -- số du khách khác nhau (COUNT DISTINCT user_id)
    PRIMARY KEY (day)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Tuần 6 (Tuấn): đếm thời gian thực từ sự kiện RabbitMQ (visit.recorded, feedback.created).
-- Chỉ dùng cho số "hôm nay" trên Dashboard. Thống kê theo khoảng ngày và xuất PDF/Excel
-- vẫn đọc fact_* (NiFi), KHÔNG cộng 2 nguồn với nhau để tránh đếm gấp đôi.

-- eventId đã xử lý: RabbitMQ có thể giao một tin nhiều lần, gặp lại thì bỏ qua
CREATE TABLE IF NOT EXISTS rt_processed_events (
    event_id     CHAR(36)    NOT NULL,                 -- EventEnvelope.eventId (UUID)
    event_type   VARCHAR(50) NOT NULL,                 -- VisitRecorded | FeedbackCreated
    processed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (event_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Số đếm theo ngày và hiện vật: lượt xem hôm nay = SUM(views), top hiện vật hôm nay = ORDER BY views
CREATE TABLE IF NOT EXISTS rt_daily_counters (
    day         DATE   NOT NULL,                       -- ngày của detectedAt / createdAt
    artifact_id BIGINT NOT NULL,
    views       INT    NOT NULL DEFAULT 0,             -- số sự kiện visit.recorded
    feedbacks   INT    NOT NULL DEFAULT 0,             -- số sự kiện feedback.created
    rating_sum  INT    NOT NULL DEFAULT 0,             -- tổng điểm, điểm TB = rating_sum / feedbacks
    PRIMARY KEY (day, artifact_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
