CREATE DATABASE IF NOT EXISTS smart_heritage_db
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE smart_heritage_db;
SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1. CAC BANG CHA (khong co khoa ngoai)
-- ---------------------------------------------------------------------

-- ERD: Users.
CREATE TABLE IF NOT EXISTS users (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    username           VARCHAR(50)  NOT NULL,
    email              VARCHAR(100) NOT NULL,
    password_hash      VARCHAR(255) NOT NULL,
    full_name          VARCHAR(100) NULL,
    preferred_language VARCHAR(10)  NULL DEFAULT 'vi',
    role               VARCHAR(20)  NOT NULL DEFAULT 'VISITOR',
    created_at         DATETIME(6)  NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_username (username),
    UNIQUE KEY uq_users_email (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ERD: Heritage_Sites (Site_Id, Name, Location, Map_Image_Url).
CREATE TABLE IF NOT EXISTS heritage_sites (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    name          VARCHAR(150) NOT NULL,
    location      VARCHAR(255) NULL,
    map_image_url VARCHAR(500) NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ---------------------------------------------------------------------
-- 2. CAC BANG CON CAP 1
-- ---------------------------------------------------------------------

-- ERD: Artifacts (Artifact_Id, Status, Category, Code).
-- Quan he Contains: Heritage_Sites (1,n) - Artifacts (1,1)
--   => moi hien vat thuoc dung 1 di tich: site_id NOT NULL.
CREATE TABLE IF NOT EXISTS artifacts (
    id       BIGINT      NOT NULL AUTO_INCREMENT,
    site_id  BIGINT      NOT NULL,
    status   VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    category VARCHAR(50) NULL,
    code     VARCHAR(50) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_artifacts_code (code),
    CONSTRAINT fk_artifacts_site
        FOREIGN KEY (site_id) REFERENCES heritage_sites (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ---------------------------------------------------------------------
-- 3. CAC BANG CON CAP 2
-- ---------------------------------------------------------------------

-- ERD: iBeacons (Beacon_Id, Uuid, Major, Minor, X_Coord, Y_Coord, Battery_Level).
-- Located_At: Heritage_Sites (0,n) - iBeacons (1,1)  => site_id NOT NULL.
-- Assigned_To: Artifacts (0,n) - iBeacons (0,1)      => artifact_id NULL (1 beacon gan toi da 1 hien vat).
CREATE TABLE IF NOT EXISTS ibeacons (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    site_id       BIGINT      NOT NULL,
    artifact_id   BIGINT      NULL,
    uuid          CHAR(36)    NOT NULL,
    major         INT         NOT NULL,
    minor         INT         NOT NULL,
    x_coord       DOUBLE      NULL,
    y_coord       DOUBLE      NULL,
    battery_level INT         NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_ibeacons_identity (uuid, major, minor),
    CONSTRAINT chk_ibeacons_major   CHECK (major BETWEEN 0 AND 65535),
    CONSTRAINT chk_ibeacons_minor   CHECK (minor BETWEEN 0 AND 65535),
    CONSTRAINT chk_ibeacons_battery CHECK (battery_level IS NULL OR battery_level BETWEEN 0 AND 100),
    CONSTRAINT fk_ibeacons_site
        FOREIGN KEY (site_id) REFERENCES heritage_sites (id),
    CONSTRAINT fk_ibeacons_artifact
        FOREIGN KEY (artifact_id) REFERENCES artifacts (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ERD: Artifact_Translations (thuc the yeu; Translation_Id, Language_Code, Name, Audio_Url, Description).
-- Translates: Artifacts (1,n) - Artifact_Translations (1,1) => artifact_id NOT NULL.
-- Moi hien vat co toi da 1 ban dich cho moi ngon ngu (rang buoc trien khai).
CREATE TABLE IF NOT EXISTS artifact_translations (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    artifact_id   BIGINT       NOT NULL,
    language_code VARCHAR(10)  NOT NULL,
    name          VARCHAR(200) NOT NULL,
    audio_url     VARCHAR(500) NULL,
    description   TEXT         NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_artifact_translations_lang (artifact_id, language_code),
    CONSTRAINT fk_artifact_translations_artifact
        FOREIGN KEY (artifact_id) REFERENCES artifacts (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ERD: Media_Resources (Media_Id, Media_Type, File_Url, Caption).
-- Has: Artifacts (0,n) - Media_Resources (1,1) => artifact_id NOT NULL.
CREATE TABLE IF NOT EXISTS media_resources (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    artifact_id BIGINT       NOT NULL,
    media_type  VARCHAR(20)  NOT NULL,
    file_url    VARCHAR(500) NOT NULL,
    caption     VARCHAR(255) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_media_resources_artifact
        FOREIGN KEY (artifact_id) REFERENCES artifacts (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ERD: quan he Visits (Users N-N Artifacts), thuoc tinh Detected_At, Duration_Seconds.
-- Mot nguoi co the ghe tham cung mot hien vat nhieu lan => khoa thay the id.
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
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ERD: quan he Likes (Users N-N Artifacts), thuoc tinh Saved_At.
-- Mot nguoi luu mot hien vat toi da 1 lan => khoa chinh ghep (user_id, artifact_id).
CREATE TABLE IF NOT EXISTS likes (
    user_id     BIGINT   NOT NULL,
    artifact_id BIGINT   NOT NULL,
    saved_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, artifact_id),
    CONSTRAINT fk_likes_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_likes_artifact
        FOREIGN KEY (artifact_id) REFERENCES artifacts (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ERD: quan he Feedbacks (Users N-N Artifacts), thuoc tinh Rating, Comment, Status, Created_At.
-- Khong dat UNIQUE (user_id, artifact_id): ERD the hien (0,n) hai phia.
-- Status: 'PENDING' | 'APPROVED' | 'REJECTED' (gia tri do trien khai chon, ERD khong quy dinh).
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
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- =====================================================================
-- DU LIEU MAU (gia, chi de kiem thu). Thu tu: bang cha truoc, bang con sau.
-- Mat khau cua moi tai khoan mau: Test@1234 (hash BCrypt cost 10).
-- Chi dung de thu nghiem, khong dung o moi truong that.
-- =====================================================================

INSERT INTO users (id, username, email, password_hash, full_name, preferred_language, role, created_at) VALUES
    (1, 'admin',        'admin@example.com',       '$2b$10$abcdefghijklmnopqrstuupYWJKQ4BNERAHkseHR6HFLBtBzDzsjO', 'Quản trị viên mẫu', 'vi', 'ADMIN',   '2026-10-01 08:00:00'),
    (2, 'visitor_an',   'an.nguyen@example.com',   '$2b$10$abcdefghijklmnopqrstuupYWJKQ4BNERAHkseHR6HFLBtBzDzsjO', 'Nguyễn Văn An',     'vi', 'VISITOR', '2026-10-01 08:05:00'),
    (3, 'visitor_mai',  'mai.tran@example.com',    '$2b$10$abcdefghijklmnopqrstuupYWJKQ4BNERAHkseHR6HFLBtBzDzsjO', 'Trần Thị Mai',      'vi', 'VISITOR', '2026-10-01 08:10:00'),
    (4, 'visitor_john', 'john.smith@example.com',  '$2b$10$abcdefghijklmnopqrstuupYWJKQ4BNERAHkseHR6HFLBtBzDzsjO', 'John Smith',        'en', 'VISITOR', '2026-10-01 08:15:00');

INSERT INTO heritage_sites (id, name, location, map_image_url) VALUES
    (1, 'Văn Miếu - Quốc Tử Giám', '58 Quốc Tử Giám, Đống Đa, Hà Nội', 'https://example.com/maps/van-mieu.png'),
    (2, 'Hoàng thành Thăng Long',  '19C Hoàng Diệu, Ba Đình, Hà Nội',  'https://example.com/maps/hoang-thanh.png');

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