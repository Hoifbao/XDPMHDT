-- =====================================================================
--  auth_db - auth-service (Bảo, cổng 8081) | Script: Tường - Sprint 4
--  Bảng: users, refresh_tokens
--  Mật khẩu của mọi tài khoản mẫu: Test@1234 (hash BCrypt cost 10). Chỉ để kiểm thử.
--  Id người dùng GIỮ NGUYÊN như smart_heritage_db (1..4) để các schema khác
--  (personalization_db, feedback_db) tham chiếu đúng user_id.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS auth_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE auth_db;
SET NAMES utf8mb4;

-- Entity: User (Bảo)
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

-- Entity: RefreshToken (Bảo). Chỉ lưu SHA-256 của refresh token. Khóa ngoại nằm trong cùng schema.
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

INSERT INTO users (id, username, email, password_hash, full_name, preferred_language, role, created_at) VALUES
    (1, 'admin',        'admin@example.com',      '$2a$10$49XXXfZrrbAlJRbVGs6Gf.f3du6Nsa8kMEgkHd2gOGk8texi.CtCO', 'Quản trị viên mẫu', 'vi', 'ADMIN',   '2026-10-01 08:00:00'),
    (2, 'visitor_an',   'an.nguyen@example.com',  '$2a$10$49XXXfZrrbAlJRbVGs6Gf.f3du6Nsa8kMEgkHd2gOGk8texi.CtCO', 'Nguyễn Văn An',     'vi', 'VISITOR', '2026-10-01 08:05:00'),
    (3, 'visitor_mai',  'mai.tran@example.com',   '$2a$10$49XXXfZrrbAlJRbVGs6Gf.f3du6Nsa8kMEgkHd2gOGk8texi.CtCO', 'Trần Thị Mai',      'vi', 'VISITOR', '2026-10-01 08:10:00'),
    (4, 'visitor_john', 'john.smith@example.com', '$2a$10$49XXXfZrrbAlJRbVGs6Gf.f3du6Nsa8kMEgkHd2gOGk8texi.CtCO', 'John Smith',        'en', 'VISITOR', '2026-10-01 08:15:00');
