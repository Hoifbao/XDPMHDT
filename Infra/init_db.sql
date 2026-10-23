-- ============================================================
--  SmartHeritage – Database Initialization Script
--  Database : smart_heritage_db
--  Engine   : MySQL 8.x
-- ============================================================

CREATE DATABASE IF NOT EXISTS smart_heritage_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE smart_heritage_db;

-- ERD: Heritage_Sites (Site_Id, Name, Location, Map_Image_Url, Latitude, Longitude).
CREATE TABLE IF NOT EXISTS heritage_sites (
    id            BIGINT         NOT NULL AUTO_INCREMENT,
    name          VARCHAR(150)   NOT NULL,
    location      VARCHAR(255)   NULL,
    map_image_url VARCHAR(500)   NULL,
    latitude      DECIMAL(10, 7) NULL COMMENT 'Vĩ độ (WGS-84)',
    longitude     DECIMAL(10, 7) NULL COMMENT 'Kinh độ (WGS-84)',
-- ------------------------------------------------------------
-- 1. Users
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS Users (
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    username         VARCHAR(50)  NOT NULL UNIQUE,
    email            VARCHAR(100) NOT NULL UNIQUE,
    password_hash    VARCHAR(255) NOT NULL,
    full_name        VARCHAR(100),
    preferred_language VARCHAR(10) NOT NULL DEFAULT 'vi',
    role             VARCHAR(20)  NOT NULL DEFAULT 'VISITOR',
                                  -- VISITOR | ADMIN
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- 2. HeritageLocations  (địa điểm di tích)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS HeritageLocations (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    name            VARCHAR(200)  NOT NULL,
    description     TEXT,
    address         VARCHAR(500),
    latitude        DOUBLE,
    longitude       DOUBLE,
    cover_image_url VARCHAR(1000),
    city            VARCHAR(100),
    status          VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
                                  -- ACTIVE | INACTIVE
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- 3. Artifacts  (hiện vật)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS Artifacts (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    location_id     BIGINT        NOT NULL,
    name            VARCHAR(200)  NOT NULL,
    artifact_code   VARCHAR(50)   UNIQUE,           -- mã định danh vật lý
    description     TEXT,
    thumbnail_url   VARCHAR(1000),
    period          VARCHAR(100),                   -- niên đại / thời kỳ
    material        VARCHAR(200),                   -- chất liệu
    dimensions      VARCHAR(200),                   -- kích thước
    beacon_uuid     VARCHAR(100)  UNIQUE,           -- UUID iBeacon gắn với hiện vật
    status          VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_artifact_location
        FOREIGN KEY (location_id) REFERENCES HeritageLocations(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX idx_artifact_location (location_id),
    INDEX idx_artifact_beacon   (beacon_uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ------------------------------------------------------------
-- 4. ArtifactContents  (nội dung đa phương tiện đa ngôn ngữ)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ArtifactContents (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    artifact_id    BIGINT        NOT NULL,
    language_code  VARCHAR(10)   NOT NULL,          -- vi | en | fr | zh | ja | ko …
    title          VARCHAR(200),
    description    TEXT,
    audio_url      VARCHAR(1000),                   -- URL file audio thuyết minh
    video_url      VARCHAR(1000),                   -- URL video giới thiệu
    image_urls     TEXT,                            -- JSON array: ["url1","url2"]
    transcript     TEXT,                            -- phụ đề / transcript audio
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT chk_feedbacks_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT fk_feedbacks_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_feedbacks_artifact
        FOREIGN KEY (artifact_id) REFERENCES artifacts (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- =====================================================================
-- DU LIEU MAU (gia, chi de kiem thu). Thu tu: bang cha truoc, bang con sau.
-- Mat khau cua moi tai khoan mau: Test@1234 (hash BCrypt cost 10).
-- Chi dung de thu nghiem, khong dung o moi truong that.
-- =====================================================================

INSERT INTO users (id, username, email, password_hash, full_name, preferred_language, role, created_at) VALUES
    (1, 'admin',        'admin@example.com',       '$2a$10$49XXXfZrrbAlJRbVGs6Gf.f3du6Nsa8kMEgkHd2gOGk8texi.CtCO', 'Quản trị viên mẫu', 'vi', 'ADMIN',   '2026-10-01 08:00:00'),
    (2, 'visitor_an',   'an.nguyen@example.com',   '$2a$10$49XXXfZrrbAlJRbVGs6Gf.f3du6Nsa8kMEgkHd2gOGk8texi.CtCO', 'Nguyễn Văn An',     'vi', 'VISITOR', '2026-10-01 08:05:00'),
    (3, 'visitor_mai',  'mai.tran@example.com',    '$2a$10$49XXXfZrrbAlJRbVGs6Gf.f3du6Nsa8kMEgkHd2gOGk8texi.CtCO', 'Trần Thị Mai',      'vi', 'VISITOR', '2026-10-01 08:10:00'),
    (4, 'visitor_john', 'john.smith@example.com',  '$2a$10$49XXXfZrrbAlJRbVGs6Gf.f3du6Nsa8kMEgkHd2gOGk8texi.CtCO', 'John Smith',        'en', 'VISITOR', '2026-10-01 08:15:00');

INSERT INTO heritage_sites (id, name, location, map_image_url, latitude, longitude) VALUES
    (1, 'Văn Miếu - Quốc Tử Giám', '58 Quốc Tử Giám, Đống Đa, Hà Nội', 'https://example.com/maps/van-mieu.png',    21.0277644, 105.8354028),
    (2, 'Hoàng thành Thăng Long',  '19C Hoàng Diệu, Ba Đình, Hà Nội',  'https://example.com/maps/hoang-thanh.png', 21.0354430, 105.8349380);

INSERT INTO artifacts (id, site_id, status, category, code) VALUES
    (1, 1, 'ACTIVE', 'ARCHITECTURE', 'KVC-01'),
    (2, 1, 'ACTIVE', 'STELE',        'BTS-01'),
    (3, 2, 'ACTIVE', 'ARCHITECTURE', 'DM-01');

INSERT INTO ibeacons (id, site_id, artifact_id, uuid, major, minor, x_coord, y_coord, battery_level) VALUES
    (1, 1, 1,    'E2C56DB5-DFFB-48D2-B060-D0F5A71096E0', 1, 1, 12.5, 30.0, 87),
    (2, 1, 2,    'E2C56DB5-DFFB-48D2-B060-D0F5A71096E0', 1, 2, 25.0, 42.5, 64),
    (3, 2, 3,    'E2C56DB5-DFFB-48D2-B060-D0F5A71096E0', 2, 1,  8.0, 15.0, 92),
    (4, 1, NULL, 'E2C56DB5-DFFB-48D2-B060-D0F5A71096E0', 1, 3, 40.0, 10.0, 15);

INSERT INTO artifact_translations (id, artifact_id, language_code, name, audio_url, description) VALUES
    (1, 1, 'vi', 'Khuê Văn Các',   'https://example.com/audio/kvc-vi.mp3',  'Khuê Văn Các là gác vuông nằm ở khu vực trung tâm Văn Miếu, được xem là biểu tượng của Hà Nội.'),
    (2, 1, 'en', 'Khue Van Cac',   'https://example.com/audio/kvc-en.mp3',  'Khue Van Cac, the Constellation of Literature Pavilion, is a symbol of the Temple of Literature.'),
    (3, 2, 'vi', 'Bia Tiến sĩ',    'https://example.com/audio/bts-vi.mp3',  'Các bia đá ghi tên những người đỗ tiến sĩ trong các kỳ thi dưới triều đại phong kiến.'),
    (4, 2, 'en', 'Doctors Stelae', 'https://example.com/audio/bts-en.mp3',  'Stone stelae recording the names of laureates of royal examinations.'),
    (5, 3, 'vi', 'Đoan Môn',       'https://example.com/audio/dm-vi.mp3',   'Đoan Môn là cổng chính phía nam của khu Hoàng thành Thăng Long.');

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

-- Du lieu Feedbacks: du 3 trang thai, co ca binh luan NULL, tham chieu user/artifact da ton tai.
INSERT INTO feedbacks (id, user_id, artifact_id, rating, comment, status, created_at) VALUES
    (1, 2, 1, 5, 'Khuê Văn Các rất đẹp, phần thuyết minh dễ hiểu.',          'APPROVED', '2026-10-05 09:30:00'),
    (2, 2, 2, 4, 'Thông tin về bia Tiến sĩ hay, mong có thêm video.',         'APPROVED', '2026-10-05 09:45:00'),
    (3, 3, 1, 3, 'Tín hiệu beacon hơi chậm khi đông người.',                  'PENDING',  '2026-10-06 14:20:00'),
    (4, 4, 3, 5, 'The English audio guide is clear and helpful.',             'APPROVED', '2026-10-07 10:50:00'),
    (5, 3, 2, 2, NULL,                                                         'PENDING',  '2026-10-06 14:40:00'),
    (6, 4, 1, 1, 'Nội dung quảng cáo không liên quan đến hiện vật.',          'REJECTED', '2026-10-07 11:00:00');
    CONSTRAINT uq_artifact_language UNIQUE (artifact_id, language_code),
    CONSTRAINT fk_content_artifact
        FOREIGN KEY (artifact_id) REFERENCES Artifacts(id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    INDEX idx_content_artifact  (artifact_id),
    INDEX idx_content_language  (language_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- Sample Data – dữ liệu mẫu để test
-- ============================================================

-- Admin user (password: Admin@123 – BCrypt hash)
INSERT IGNORE INTO Users (username, email, password_hash, full_name, preferred_language, role)
VALUES (
    'admin',
    'admin@smartheritage.vn',
    '$2a$10$7EqJtq98hPqEX7fNZaFWoOwOJknDhUf9b.lz6vqHV5Bn4Oq1KYTIK',
    'Quản trị viên',
    'vi',
    'ADMIN'
);

-- Địa điểm di tích mẫu
INSERT IGNORE INTO HeritageLocations (name, description, address, latitude, longitude, city, status)
VALUES (
    'Bảo tàng Lịch sử Quốc gia',
    'Bảo tàng lưu giữ hàng nghìn hiện vật lịch sử Việt Nam từ thời tiền sử đến hiện đại.',
    '1 Tràng Tiền, Hoàn Kiếm, Hà Nội',
    21.0245,
    105.8412,
    'Hà Nội',
    'ACTIVE'
);

-- Hiện vật mẫu
INSERT IGNORE INTO Artifacts
    (location_id, name, artifact_code, description, period, material, beacon_uuid, status)
VALUES (
    1,
    'Trống đồng Đông Sơn',
    'ART-001',
    'Trống đồng Đông Sơn là biểu tượng văn hóa đặc sắc của người Việt cổ, được chế tác tinh xảo vào khoảng thế kỷ VII – I TCN.',
    'Thế kỷ VII – I TCN',
    'Đồng',
    '550e8400-e29b-41d4-a716-446655440000',
    'ACTIVE'
);

-- Nội dung tiếng Việt
INSERT IGNORE INTO ArtifactContents
    (artifact_id, language_code, title, description, transcript)
VALUES (
    1,
    'vi',
    'Trống đồng Đông Sơn',
    'Trống đồng Đông Sơn là hiện vật tiêu biểu nhất của nền văn hóa Đông Sơn, xuất hiện vào khoảng thế kỷ VII đến thế kỷ I trước Công nguyên. Trống được dùng trong nghi lễ, tín ngưỡng và chiến tranh.',
    'Trống đồng Đông Sơn là biểu tượng văn hóa tiêu biểu của người Việt cổ...'
);

-- Nội dung tiếng Anh
INSERT IGNORE INTO ArtifactContents
    (artifact_id, language_code, title, description, transcript)
VALUES (
    1,
    'en',
    'Dong Son Bronze Drum',
    'The Dong Son Bronze Drum is the most iconic artifact of the Dong Son culture, dating from approximately the 7th to the 1st century BC. It was used in rituals, religious ceremonies, and warfare.',
    'The Dong Son Bronze Drum is a cultural symbol of the ancient Vietnamese people...'
);
