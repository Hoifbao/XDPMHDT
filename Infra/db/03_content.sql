-- =====================================================================
--  content_db - content-service (Xuân Anh, cổng 8083) | Script: Tường - Sprint 4
--  Bảng: artifacts, artifact_contents, media_resources
--  - artifacts.site_id KHÔNG còn khóa ngoại sang heritage_sites (đã sang site_db).
--    content-service kiểm tra siteId bằng cách gọi site-service GET /api/heritage-sites/{id}.
--  - Khóa ngoại trong cùng schema (artifact_contents, media_resources -> artifacts) vẫn giữ.
--  - Id hiện vật GIỮ NGUYÊN (1..3) như beacon_db, personalization_db, feedback_db.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS content_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE content_db;
SET NAMES utf8mb4;

-- Entity: Artifact (Xuân Anh)
CREATE TABLE IF NOT EXISTS artifacts (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    site_id       BIGINT        NOT NULL,              -- id trong site_db.heritage_sites (không FK)
    name          VARCHAR(200)  NOT NULL,
    artifact_code VARCHAR(50)   NULL,                  -- mã định danh vật lý (ERD: Code)
    category      VARCHAR(50)   NULL,
    description   TEXT          NULL,
    thumbnail_url VARCHAR(1000) NULL,
    period        VARCHAR(100)  NULL,                  -- niên đại
    material      VARCHAR(200)  NULL,                  -- chất liệu
    dimensions    VARCHAR(200)  NULL,                  -- kích thước
    beacon_uuid   VARCHAR(100)  NULL,                  -- tạm thời; liên kết chính thức ở beacon_db.ibeacons
    status        VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',   -- ACTIVE | INACTIVE
    created_at    DATETIME(6)   NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6)   NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_artifacts_code (artifact_code),
    UNIQUE KEY uq_artifacts_beacon (beacon_uuid),
    KEY idx_artifacts_site (site_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Entity: ArtifactContent (Xuân Anh). Mỗi hiện vật tối đa 1 bản nội dung cho mỗi ngôn ngữ.
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

-- Entity: MediaResource (Xuân Anh)
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
