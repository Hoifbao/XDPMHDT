-- =====================================================================
--  SmartHeritage - Tạo schema + user MySQL cho từng service (Tường - Sprint 4)
--  Engine : MySQL 8.x
--
--  Mỗi microservice có 1 schema riêng và 1 user MySQL riêng. User chỉ được
--  GRANT trên schema của mình => service A không đọc được dữ liệu service B.
--
--  | Service                 | Cổng | Schema             | User                 |
--  |-------------------------|------|--------------------|----------------------|
--  | auth-service            | 8081 | auth_db            | auth_user            |
--  | site-service            | 8082 | site_db            | site_user            |
--  | content-service         | 8083 | content_db         | content_user         |
--  | beacon-service          | 8084 | beacon_db          | beacon_user          |
--  | personalization-service | 8085 | personalization_db | personalization_user |
--  | feedback-service        | 8086 | feedback_db        | feedback_user        |
--  | analytics-service       | 8087 | analytics_db       | analytics_user       |
--  | Backend cũ (monolith)   | 8090 | smart_heritage_db  | root (99_init_db_legacy.sql) |
--
--  Mật khẩu dưới đây CHỈ dùng cho máy dev (giống rootpassword123 trong
--  docker-compose). Môi trường thật phải đổi mật khẩu.
--
--  Docker chạy các file trong Infra/db theo thứ tự tên (00 -> 07 -> 99),
--  chỉ khi volume MySQL còn trống. Chạy lại từ đầu:
--      docker compose -f Infra/docker-compose.yml down -v
--      docker compose -f Infra/docker-compose.yml up -d
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1. Schema
-- ---------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS auth_db            CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS site_db            CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS content_db         CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS beacon_db          CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS personalization_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS feedback_db        CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS analytics_db       CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 2. User riêng cho từng service ('%' = kết nối từ container khác hoặc từ máy host qua cổng 3307)
-- ---------------------------------------------------------------------
CREATE USER IF NOT EXISTS 'auth_user'@'%'            IDENTIFIED BY 'auth_pass_dev';
CREATE USER IF NOT EXISTS 'site_user'@'%'            IDENTIFIED BY 'site_pass_dev';
CREATE USER IF NOT EXISTS 'content_user'@'%'         IDENTIFIED BY 'content_pass_dev';
CREATE USER IF NOT EXISTS 'beacon_user'@'%'          IDENTIFIED BY 'beacon_pass_dev';
CREATE USER IF NOT EXISTS 'personalization_user'@'%' IDENTIFIED BY 'personalization_pass_dev';
CREATE USER IF NOT EXISTS 'feedback_user'@'%'        IDENTIFIED BY 'feedback_pass_dev';
CREATE USER IF NOT EXISTS 'analytics_user'@'%'       IDENTIFIED BY 'analytics_pass_dev';

-- ---------------------------------------------------------------------
-- 3. Quyền: mỗi user CHỈ có quyền trên schema của mình
--    (ALL để Hibernate ddl-auto=update tạo/sửa bảng được khi dev)
-- ---------------------------------------------------------------------
GRANT ALL PRIVILEGES ON auth_db.*            TO 'auth_user'@'%';
GRANT ALL PRIVILEGES ON site_db.*            TO 'site_user'@'%';
GRANT ALL PRIVILEGES ON content_db.*         TO 'content_user'@'%';
GRANT ALL PRIVILEGES ON beacon_db.*          TO 'beacon_user'@'%';
GRANT ALL PRIVILEGES ON personalization_db.* TO 'personalization_user'@'%';
GRANT ALL PRIVILEGES ON feedback_db.*        TO 'feedback_user'@'%';
GRANT ALL PRIVILEGES ON analytics_db.*       TO 'analytics_user'@'%';

FLUSH PRIVILEGES;
