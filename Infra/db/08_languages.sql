-- Existing databases: run this file once. New databases: Docker init runs it automatically.
USE content_db;
CREATE TABLE IF NOT EXISTS languages (
    code VARCHAR(10) NOT NULL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
INSERT IGNORE INTO languages (code, name, active) VALUES
('vi', 'Tiếng Việt', TRUE), ('en', 'English', TRUE);
