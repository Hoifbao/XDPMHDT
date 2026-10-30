-- =====================================================================
--  beacon_db - beacon-service (Khoa, cổng 8084) | Script: Tường - Sprint 4
--  Bảng: ibeacons
--  - site_id, artifact_id KHÔNG còn khóa ngoại (bảng gốc nằm ở site_db, content_db).
--    beacon-service kiểm tra bằng cách gọi site-service và content-service.
--  - Id giữ nguyên như smart_heritage_db. UUID dự án: E2C56DB5-DFFB-48D2-B060-D0F5A71096E0
--    (Major/Minor 1/1 = Khuê Văn Các, 1/2 = Bia Tiến sĩ, 2/1 = Đoan Môn).
-- =====================================================================

CREATE DATABASE IF NOT EXISTS beacon_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE beacon_db;
SET NAMES utf8mb4;

-- Entity: IBeacon (Khoa). Located_At: site_id NOT NULL; Assigned_To: artifact_id NULL (chưa gán).
CREATE TABLE IF NOT EXISTS ibeacons (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    site_id       BIGINT      NOT NULL,                -- id trong site_db.heritage_sites (không FK)
    artifact_id   BIGINT      NULL,                    -- id trong content_db.artifacts (không FK)
    uuid          CHAR(36)    NOT NULL,
    major         INT         NOT NULL,
    minor         INT         NOT NULL,
    x_coord       DOUBLE      NULL,
    y_coord       DOUBLE      NULL,
    battery_level INT         NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',   -- ACTIVE | INACTIVE
    created_at    DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_ibeacons_identity (uuid, major, minor),
    KEY idx_ibeacons_site (site_id),
    KEY idx_ibeacons_artifact (artifact_id),
    CONSTRAINT chk_ibeacons_major   CHECK (major BETWEEN 0 AND 65535),
    CONSTRAINT chk_ibeacons_minor   CHECK (minor BETWEEN 0 AND 65535),
    CONSTRAINT chk_ibeacons_battery CHECK (battery_level IS NULL OR battery_level BETWEEN 0 AND 100)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO ibeacons (id, site_id, artifact_id, uuid, major, minor, x_coord, y_coord, battery_level, status) VALUES
    (1, 1, 1,    'E2C56DB5-DFFB-48D2-B060-D0F5A71096E0', 1, 1, 12.5, 30.0, 87, 'ACTIVE'),
    (2, 1, 2,    'E2C56DB5-DFFB-48D2-B060-D0F5A71096E0', 1, 2, 25.0, 42.5, 64, 'ACTIVE'),
    (3, 2, 3,    'E2C56DB5-DFFB-48D2-B060-D0F5A71096E0', 2, 1,  8.0, 15.0, 92, 'ACTIVE'),
    (4, 1, NULL, 'E2C56DB5-DFFB-48D2-B060-D0F5A71096E0', 1, 3, 40.0, 10.0, 15, 'INACTIVE');
