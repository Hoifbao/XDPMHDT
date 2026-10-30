-- =====================================================================
--  site_db - site-service (Ân, cổng 8082) | Script: Tường - Sprint 4
--  Bảng: heritage_sites
--  Id di tích GIỮ NGUYÊN như smart_heritage_db (1..2): content_db.artifacts.site_id
--  và beacon_db.ibeacons.site_id tham chiếu theo id này (không có khóa ngoại chéo schema).
-- =====================================================================

CREATE DATABASE IF NOT EXISTS site_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE site_db;
SET NAMES utf8mb4;

-- Entity: HeritageSite (Ân)
CREATE TABLE IF NOT EXISTS heritage_sites (
    id            BIGINT         NOT NULL AUTO_INCREMENT,
    name          VARCHAR(150)   NOT NULL,
    location      VARCHAR(255)   NULL,                 -- địa chỉ
    map_image_url VARCHAR(500)   NULL,
    latitude      DECIMAL(10, 7) NULL COMMENT 'Vĩ độ (WGS-84)',
    longitude     DECIMAL(10, 7) NULL COMMENT 'Kinh độ (WGS-84)',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

INSERT INTO heritage_sites (id, name, location, map_image_url, latitude, longitude) VALUES
    (1, 'Văn Miếu - Quốc Tử Giám', '58 Quốc Tử Giám, Đống Đa, Hà Nội', 'https://example.com/maps/van-mieu.png',    21.0277644, 105.8354028),
    (2, 'Hoàng thành Thăng Long',  '19C Hoàng Diệu, Ba Đình, Hà Nội',  'https://example.com/maps/hoang-thanh.png', 21.0354430, 105.8349380);
