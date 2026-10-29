-- =====================================================================
--  SmartHeritage - Database Initialization Script
--  Database : smart_heritage_db   |   Engine : MySQL 8.x
--
--  Bản hợp nhất tuần 2 (gộp từ bản 9 bảng của Tường, entity Artifact /
--  ArtifactContent của Xuân Anh và HeritageSite của Ân).
--
--  Quy ước:
--   - Tên bảng/cột viết thường, snake_case: khớp với cách Spring Boot
--     (CamelCaseToUnderscoresNamingStrategy) đặt tên từ các @Entity.
--     VD: @Table(name = "ArtifactContents") -> bảng artifact_contents.
--   - Khác ERD tuần 1: bảng artifact_translations được thay bằng
--     artifact_contents (đa ngôn ngữ + audio/video/ảnh) theo code của Xuân Anh.
--     Cần cập nhật lại ERD cho khớp.
--   - Script chỉ chạy khi volume MySQL còn trống. Muốn chạy lại:
--       docker compose -f Infra/docker-compose.yml down -v
--       docker compose -f Infra/docker-compose.yml up -d
-- =====================================================================

CREATE DATABASE IF NOT EXISTS smart_heritage_db
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE smart_heritage_db;
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1. CÁC BẢNG CHA (không có khóa ngoại)
-- ---------------------------------------------------------------------

-- ERD: Users  |  Entity: User (Bảo)
CREATE TABLE IF NOT EXISTS users (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    username           VARCHAR(50)  NOT NULL,
    email              VARCHAR(100) NOT NULL,
    password_hash      VARCHAR(255) NOT NULL,
    full_name          VARCHAR(100) NULL,
    preferred_language VARCHAR(10)  NULL DEFAULT 'vi',
    role               VARCHAR(20)  NOT NULL DEFAULT 'VISITOR',   -- VISITOR | ADMIN
    created_at         DATETIME(6)  NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_username (username),
    UNIQUE KEY uq_users_email (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ERD: Heritage_Sites  |  Entity: HeritageSite (Ân)
-- Bảng di tích DUY NHẤT của hệ thống (đã bỏ HeritageLocations).
CREATE TABLE IF NOT EXISTS heritage_sites (
    id            BIGINT         NOT NULL AUTO_INCREMENT,
    name          VARCHAR(150)   NOT NULL,
    location      VARCHAR(255)   NULL,                 -- địa chỉ
    map_image_url VARCHAR(500)   NULL,
    latitude      DECIMAL(10, 7) NULL COMMENT 'Vĩ độ (WGS-84)',
    longitude     DECIMAL(10, 7) NULL COMMENT 'Kinh độ (WGS-84)',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 2. CÁC BẢNG CON CẤP 1
-- ---------------------------------------------------------------------

-- ERD: Artifacts  |  Entity: Artifact (Xuân Anh)
-- Contains: Heritage_Sites (1,n) - Artifacts (1,1) => site_id NOT NULL.
CREATE TABLE IF NOT EXISTS artifacts (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    site_id       BIGINT        NOT NULL,
    name          VARCHAR(200)  NOT NULL,
    artifact_code VARCHAR(50)   NULL,                  -- mã định danh vật lý (ERD: Code)
    category      VARCHAR(50)   NULL,                  -- ERD: Category (chưa có trong entity)
    description   TEXT          NULL,
    thumbnail_url VARCHAR(1000) NULL,
    period        VARCHAR(100)  NULL,                  -- niên đại
    material      VARCHAR(200)  NULL,                  -- chất liệu
    dimensions    VARCHAR(200)  NULL,                  -- kích thước
    beacon_uuid   VARCHAR(100)  NULL,                  -- tạm thời; liên kết chính thức qua bảng ibeacons
    status        VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',   -- ACTIVE | INACTIVE
    created_at    DATETIME(6)   NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6)   NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_artifacts_code (artifact_code),
    UNIQUE KEY uq_artifacts_beacon (beacon_uuid),
    KEY idx_artifacts_site (site_id),
    CONSTRAINT fk_artifacts_site
        FOREIGN KEY (site_id) REFERENCES heritage_sites (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 3. CÁC BẢNG CON CẤP 2
-- ---------------------------------------------------------------------

-- ERD: iBeacons (Khoa)  |  Entity: IBeacon
-- Located_At: Heritage_Sites (0,n) - iBeacons (1,1)  => site_id NOT NULL.
-- Assigned_To: Artifacts (0,n) - iBeacons (0,1)      => artifact_id NULL.
CREATE TABLE IF NOT EXISTS ibeacons (
    id            BIGINT   NOT NULL AUTO_INCREMENT,
    site_id       BIGINT   NOT NULL,
    artifact_id   BIGINT   NULL,
    uuid          CHAR(36) NOT NULL,
    major         INT      NOT NULL,
    minor         INT      NOT NULL,
    x_coord       DOUBLE   NULL,
    y_coord       DOUBLE   NULL,
    battery_level INT      NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',   -- ACTIVE | INACTIVE
    created_at    DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_ibeacons_identity (uuid, major, minor),
    CONSTRAINT chk_ibeacons_major   CHECK (major BETWEEN 0 AND 65535),
    CONSTRAINT chk_ibeacons_minor   CHECK (minor BETWEEN 0 AND 65535),
    CONSTRAINT chk_ibeacons_battery CHECK (battery_level IS NULL OR battery_level BETWEEN 0 AND 100),
    CONSTRAINT fk_ibeacons_site
        FOREIGN KEY (site_id) REFERENCES heritage_sites (id),
    CONSTRAINT fk_ibeacons_artifact
        FOREIGN KEY (artifact_id) REFERENCES artifacts (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Thay cho ERD Artifact_Translations  |  Entity: ArtifactContent (Xuân Anh)
-- Mỗi hiện vật có tối đa 1 bản nội dung cho mỗi ngôn ngữ.
CREATE TABLE IF NOT EXISTS artifact_contents (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    artifact_id   BIGINT        NOT NULL,
    language_code VARCHAR(10)   NOT NULL,              -- vi | en | fr | zh | ja | ko ...
    title         VARCHAR(200)  NULL,
    description   TEXT          NULL,
    audio_url     VARCHAR(1000) NULL,
    video_url     VARCHAR(1000) NULL,
    image_urls    TEXT          NULL,                  -- JSON array: ["url1","url2"]
    transcript    TEXT          NULL,
    created_at    DATETIME(6)   NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6)   NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_artifact_language (artifact_id, language_code),
    CONSTRAINT fk_artifact_contents_artifact
        FOREIGN KEY (artifact_id) REFERENCES artifacts (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ERD: Media_Resources (Xuân Anh)  |  Chưa có entity
-- Has: Artifacts (0,n) - Media_Resources (1,1) => artifact_id NOT NULL.
CREATE TABLE IF NOT EXISTS media_resources (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    artifact_id BIGINT       NOT NULL,
    media_type  VARCHAR(20)  NOT NULL,                 -- IMAGE | AUDIO | VIDEO
    file_url    VARCHAR(500) NOT NULL,
    caption     VARCHAR(255) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_media_resources_artifact
        FOREIGN KEY (artifact_id) REFERENCES artifacts (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ERD: quan hệ Visits (Users N-N Artifacts) (Phú)  |  Chưa có entity trong Backend
CREATE TABLE IF NOT EXISTS visits (
    id               BIGINT   NOT NULL AUTO_INCREMENT,
    user_id          BIGINT   NOT NULL,
    artifact_id      BIGINT   NOT NULL,
    detected_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    duration_seconds INT      NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT chk_visits_duration CHECK (duration_seconds IS NULL OR duration_seconds >= 0),
    CONSTRAINT fk_visits_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_visits_artifact
        FOREIGN KEY (artifact_id) REFERENCES artifacts (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ERD: quan hệ Likes (Users N-N Artifacts) (Phú)  |  Chưa có entity trong Backend
CREATE TABLE IF NOT EXISTS likes (
    user_id     BIGINT   NOT NULL,
    artifact_id BIGINT   NOT NULL,
    saved_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, artifact_id),
    CONSTRAINT fk_likes_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_likes_artifact
        FOREIGN KEY (artifact_id) REFERENCES artifacts (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ERD: quan hệ Feedbacks (Users N-N Artifacts)  |  Entity: Feedback (Tường)
-- Status: PENDING | APPROVED | REJECTED
CREATE TABLE IF NOT EXISTS feedbacks (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    user_id     BIGINT      NOT NULL,
    artifact_id BIGINT      NOT NULL,
    rating      INT         NOT NULL,
    comment     TEXT        NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at  DATETIME(6) NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT chk_feedbacks_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT fk_feedbacks_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_feedbacks_artifact
        FOREIGN KEY (artifact_id) REFERENCES artifacts (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Entity: RefreshToken (Bảo, Sprint 3). Chỉ lưu SHA-256 của refresh token, không lưu token gốc.
-- Nguồn: Docs/api/auth-jwt.md mục 5. Hibernate (ddl-auto=update) từng tự tạo bảng này.
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    token_hash  CHAR(64)    NOT NULL,
    user_id     BIGINT      NOT NULL,
    expires_at  DATETIME(6) NOT NULL,
    revoked     BIT(1)      NOT NULL DEFAULT 0,
    created_at  DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_refresh_tokens_hash (token_hash),
    KEY idx_refresh_tokens_user (user_id),
    CONSTRAINT fk_refresh_tokens_user
        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- =====================================================================
-- DỮ LIỆU MẪU (giả, chỉ để kiểm thử). Thứ tự: bảng cha trước, bảng con sau.
-- Mật khẩu của mọi tài khoản mẫu: Test@1234 (hash BCrypt cost 10).
-- Không dùng ở môi trường thật.
-- =====================================================================

INSERT INTO users (id, username, email, password_hash, full_name, preferred_language, role, created_at) VALUES
    (1, 'admin',        'admin@example.com',      '$2a$10$49XXXfZrrbAlJRbVGs6Gf.f3du6Nsa8kMEgkHd2gOGk8texi.CtCO', 'Quản trị viên mẫu', 'vi', 'ADMIN',   '2026-10-01 08:00:00'),
    (2, 'visitor_an',   'an.nguyen@example.com',  '$2a$10$49XXXfZrrbAlJRbVGs6Gf.f3du6Nsa8kMEgkHd2gOGk8texi.CtCO', 'Nguyễn Văn An',     'vi', 'VISITOR', '2026-10-01 08:05:00'),
    (3, 'visitor_mai',  'mai.tran@example.com',   '$2a$10$49XXXfZrrbAlJRbVGs6Gf.f3du6Nsa8kMEgkHd2gOGk8texi.CtCO', 'Trần Thị Mai',      'vi', 'VISITOR', '2026-10-01 08:10:00'),
    (4, 'visitor_john', 'john.smith@example.com', '$2a$10$49XXXfZrrbAlJRbVGs6Gf.f3du6Nsa8kMEgkHd2gOGk8texi.CtCO', 'John Smith',        'en', 'VISITOR', '2026-10-01 08:15:00');

INSERT INTO heritage_sites (id, name, location, map_image_url, latitude, longitude) VALUES
    (1, 'Văn Miếu - Quốc Tử Giám', '58 Quốc Tử Giám, Đống Đa, Hà Nội', 'https://example.com/maps/van-mieu.png',    21.0277644, 105.8354028),
    (2, 'Hoàng thành Thăng Long',  '19C Hoàng Diệu, Ba Đình, Hà Nội',  'https://example.com/maps/hoang-thanh.png', 21.0354430, 105.8349380);

INSERT INTO artifacts (id, site_id, name, artifact_code, category, description, thumbnail_url, period, material, dimensions, beacon_uuid, status) VALUES
    (1, 1, 'Khuê Văn Các', 'KVC-01', 'ARCHITECTURE',
        'Gác vuông ở trung tâm Văn Miếu, biểu tượng của Hà Nội.',
        'https://example.com/media/kvc-thumb.jpg', 'Năm 1805 (triều Nguyễn)', 'Gỗ, gạch', NULL, NULL, 'ACTIVE'),
    (2, 1, 'Bia Tiến sĩ', 'BTS-01', 'STELE',
        'Các bia đá ghi tên người đỗ tiến sĩ trong các kỳ thi thời phong kiến.',
        'https://example.com/media/bts-thumb.jpg', 'Thế kỷ XV - XVIII', 'Đá xanh', NULL, NULL, 'ACTIVE'),
    (3, 2, 'Đoan Môn', 'DM-01', 'ARCHITECTURE',
        'Cổng chính phía nam của khu Hoàng thành Thăng Long.',
        'https://example.com/media/dm-thumb.jpg', 'Thời Lê', 'Gạch, đá', NULL, NULL, 'ACTIVE');

INSERT INTO ibeacons (id, site_id, artifact_id, uuid, major, minor, x_coord, y_coord, battery_level, status) VALUES
    (1, 1, 1,    'E2C56DB5-DFFB-48D2-B060-D0F5A71096E0', 1, 1, 12.5, 30.0, 87, 'ACTIVE'),
    (2, 1, 2,    'E2C56DB5-DFFB-48D2-B060-D0F5A71096E0', 1, 2, 25.0, 42.5, 64, 'ACTIVE'),
    (3, 2, 3,    'E2C56DB5-DFFB-48D2-B060-D0F5A71096E0', 2, 1,  8.0, 15.0, 92, 'ACTIVE'),
    (4, 1, NULL, 'E2C56DB5-DFFB-48D2-B060-D0F5A71096E0', 1, 3, 40.0, 10.0, 15, 'INACTIVE');

INSERT INTO artifact_contents (id, artifact_id, language_code, title, description, audio_url) VALUES
    (1, 1, 'vi', 'Khuê Văn Các',   'Khuê Văn Các là gác vuông nằm ở khu vực trung tâm Văn Miếu, được xem là biểu tượng của Hà Nội.', 'https://example.com/audio/kvc-vi.mp3'),
    (2, 1, 'en', 'Khue Van Cac',   'Khue Van Cac, the Constellation of Literature Pavilion, is a symbol of the Temple of Literature.', 'https://example.com/audio/kvc-en.mp3'),
    (3, 2, 'vi', 'Bia Tiến sĩ',    'Các bia đá ghi tên những người đỗ tiến sĩ trong các kỳ thi dưới triều đại phong kiến.',          'https://example.com/audio/bts-vi.mp3'),
    (4, 2, 'en', 'Doctors Stelae', 'Stone stelae recording the names of laureates of royal examinations.',                           'https://example.com/audio/bts-en.mp3'),
    (5, 3, 'vi', 'Đoan Môn',       'Đoan Môn là cổng chính phía nam của khu Hoàng thành Thăng Long.',                                 'https://example.com/audio/dm-vi.mp3');

INSERT INTO media_resources (id, artifact_id, media_type, file_url, caption) VALUES
    (1, 1, 'IMAGE', 'https://example.com/media/kvc-1.jpg', 'Toàn cảnh Khuê Văn Các'),
    (2, 2, 'IMAGE', 'https://example.com/media/bts-1.jpg', 'Dãy bia Tiến sĩ'),
    (3, 1, 'VIDEO', 'https://example.com/media/kvc.mp4',   'Giới thiệu Khuê Văn Các');

INSERT INTO visits (id, user_id, artifact_id, detected_at, duration_seconds) VALUES
    (1, 2, 1, '2026-10-05 09:10:00', 120),
    (2, 2, 2, '2026-10-05 09:20:00', 300),
    (3, 3, 1, '2026-10-06 14:05:00',  90),
    (4, 4, 3, '2026-10-07 10:30:00', 210);

INSERT INTO likes (user_id, artifact_id, saved_at) VALUES
    (2, 1, '2026-10-05 09:15:00'),
    (3, 1, '2026-10-06 14:10:00'),
    (4, 3, '2026-10-07 10:35:00');

-- Feedbacks: đủ 3 trạng thái, có cả bình luận NULL.
INSERT INTO feedbacks (id, user_id, artifact_id, rating, comment, status, created_at) VALUES
    (1, 2, 1, 5, 'Khuê Văn Các rất đẹp, phần thuyết minh dễ hiểu.',   'APPROVED', '2026-10-05 09:30:00'),
    (2, 2, 2, 4, 'Thông tin về bia Tiến sĩ hay, mong có thêm video.',  'APPROVED', '2026-10-05 09:45:00'),
    (3, 3, 1, 3, 'Tín hiệu beacon hơi chậm khi đông người.',           'PENDING',  '2026-10-06 14:20:00'),
    (4, 4, 3, 5, 'The English audio guide is clear and helpful.',      'APPROVED', '2026-10-07 10:50:00'),
    (5, 3, 2, 2, NULL,                                                  'PENDING',  '2026-10-06 14:40:00'),
    (6, 4, 1, 1, 'Nội dung quảng cáo không liên quan đến hiện vật.',   'REJECTED', '2026-10-07 11:00:00');
