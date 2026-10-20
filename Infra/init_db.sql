-- ============================================================
--  SmartHeritage – Database Initialization Script
--  Database : smart_heritage_db
--  Engine   : MySQL 8.x
-- ============================================================

CREATE DATABASE IF NOT EXISTS smart_heritage_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE smart_heritage_db;

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
