-- =====================================================================
--  SmartHeritage - analytics_db (Tuấn - Sprint 4)
--  Database : analytics_db   |   Engine : MySQL 8.x
--
--  Kho dữ liệu thống kê do Apache NiFi đổ vào từ các schema nghiệp vụ.
--  Thiết kế: Tuấn (nhánh feature/tuan-w4-nifi, Infra/nifi/sql/07_analytics.sql).
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

-- Tổng hợp theo ngày cho biểu đồ lượt xem (tính từ fact_visits)
CREATE TABLE IF NOT EXISTS agg_daily_views (
    day      DATE NOT NULL,
    views    INT  NOT NULL DEFAULT 0,                  -- số lượt (COUNT visit_id)
    visitors INT  NOT NULL DEFAULT 0,                  -- số du khách khác nhau (COUNT DISTINCT user_id)
    PRIMARY KEY (day)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
