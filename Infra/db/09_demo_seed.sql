-- =====================================================================
--  Dữ liệu demo bản cuối - content_db (Tường - Sprint 7, tuần 7)
--  Nguồn: services/content-service/demo-data/manifest.json của Xuân Anh (PR #84), nạp bằng
--         services/content-service/scripts/import-demo-data.ps1 qua Gateway rồi dump lại (mysqldump).
--  Ngày tạo: 2026-11-19, code ở commit 66e8361.
--
--  Chạy SAU 03_content.sql (Docker chạy theo thứ tự tên file khi volume MySQL còn trống):
--  thay toàn bộ hiện vật mẫu bằng 2 di tích, 6 hiện vật có bản Việt/Anh, ảnh, ảnh nhỏ, audio tiếng Anh.
--  Id 1..3 (Khuê Văn Các, Bia Tiến sĩ, Đoan Môn) giữ nguyên để beacon_db, feedback_db,
--  personalization_db vẫn khớp; hiện vật mới là id 4..6 (chưa gắn beacon).
--
--  File media nằm ở Infra/seed/media/ (tên UUID đúng như trong các URL bên dưới). Nạp media sau khi
--  content-service đã chạy (xem Docs/deployment/deployment-guide.md mục 8):
--      docker cp Infra/seed/media/. smart_heritage_content:/app/uploads/
--  Tạo lại file này: xem Infra/seed/README.md.
-- =====================================================================

USE content_db;
SET NAMES utf8mb4;

-- Xoá dữ liệu mẫu cũ (bảng con trước, bảng cha sau). Bảng outbox không đụng tới.
DELETE FROM media_resources;
DELETE FROM artifact_contents;
DELETE FROM artifacts;


-- Hiện vật
INSERT INTO artifacts (id, site_id, name, artifact_code, category, description, thumbnail_url, period, material, dimensions, beacon_uuid, status, created_at, updated_at) VALUES
    (1,1,'Khuê Văn Các','KVC-01','ARCHITECTURE','Khuê Văn Các là một công trình tiêu biểu trong khu di tích Văn Miếu - Quốc Tử Giám. Các ô cửa tròn và không gian bên giếng Thiên Quang tạo nên hình ảnh quen thuộc của di sản Hà Nội.','http://localhost:8080/api/media/files/d464fdca-51b3-4814-a223-734d2931f97e.jpg','Năm 1805 (triều Nguyễn)','Gỗ, gạch',NULL,NULL,'ACTIVE','2026-11-19 13:18:06.304920','2026-11-19 13:19:30.640585');
INSERT INTO artifacts (id, site_id, name, artifact_code, category, description, thumbnail_url, period, material, dimensions, beacon_uuid, status, created_at, updated_at) VALUES
    (2,1,'Bia Tiến sĩ','BTS-01','STELE','Những tấm bia đá tại Văn Miếu lưu danh người đỗ các kỳ thi thời phong kiến. Bia đặt trên lưng rùa đá, thể hiện truyền thống trọng học và lưu giữ dấu ấn nghệ thuật của nhiều thời kỳ.','http://localhost:8080/api/media/files/7e657e40-50a1-4b32-b936-c8c14d47dc78.jpg','Thế kỷ XV - XVIII','Đá xanh',NULL,NULL,'ACTIVE','2026-11-19 13:18:06.304920','2026-11-19 13:19:31.613303');
INSERT INTO artifacts (id, site_id, name, artifact_code, category, description, thumbnail_url, period, material, dimensions, beacon_uuid, status, created_at, updated_at) VALUES
    (3,2,'Đoan Môn','DM-01','ARCHITECTURE','Đoan Môn là cửa phía nam dẫn vào khu vực trung tâm Cấm thành Thăng Long. Công trình nằm trên trục không gian nối với nền điện Kính Thiên, giúp hình dung tổ chức của khu cung điện xưa.','http://localhost:8080/api/media/files/1891c050-86f9-40e4-92b7-61137ea0bfc0.jpg','Thời Lê','Gạch, đá',NULL,NULL,'ACTIVE','2026-11-19 13:18:06.304920','2026-11-19 13:19:32.575491');
INSERT INTO artifacts (id, site_id, name, artifact_code, category, description, thumbnail_url, period, material, dimensions, beacon_uuid, status, created_at, updated_at) VALUES
    (4,1,'Đại Trung Môn','DTM-W7',NULL,'Đại Trung Môn nằm trên tuyến tham quan Văn Miếu - Quốc Tử Giám. Cổng chính và các lối đi bên dẫn du khách vào không gian sân vườn của di tích trước khi đến Khuê Văn Các.','http://localhost:8080/api/media/files/33ff94e7-1d5e-4641-8485-c256bc69be2e.jpg',NULL,NULL,NULL,NULL,'ACTIVE','2026-11-19 13:19:32.075425','2026-11-19 13:19:32.075441');
INSERT INTO artifacts (id, site_id, name, artifact_code, category, description, thumbnail_url, period, material, dimensions, beacon_uuid, status, created_at, updated_at) VALUES
    (5,2,'Nền điện Kính Thiên','DKT-W7',NULL,'Khu vực điện Kính Thiên giữ vị trí trung tâm của Hoàng thành Thăng Long. Nền điện và những dấu tích còn lại giúp du khách hình dung không gian cung đình; ảnh kèm theo là tư liệu lịch sử, không phải hiện trạng công trình.','http://localhost:8080/api/media/files/cbba2ea5-003b-406d-b8ee-e010893b92de.jpg',NULL,NULL,NULL,NULL,'ACTIVE','2026-11-19 13:19:32.930664','2026-11-19 13:19:32.930679');
INSERT INTO artifacts (id, site_id, name, artifact_code, category, description, thumbnail_url, period, material, dimensions, beacon_uuid, status, created_at, updated_at) VALUES
    (6,2,'Cột cờ Hà Nội','CCHN-W7',NULL,'Cột cờ Hà Nội là một công trình lịch sử còn lại của khu thành Hà Nội. Nằm về phía nam trục trung tâm, công trình giúp du khách liên hệ vị trí Đoan Môn và khu vực điện Kính Thiên.','http://localhost:8080/api/media/files/0b3362be-fd1b-4c73-bb20-ad45f789c961.jpg',NULL,NULL,NULL,NULL,'ACTIVE','2026-11-19 13:19:33.261660','2026-11-19 13:19:33.261675');
ALTER TABLE artifacts AUTO_INCREMENT = 7;

-- Bản dịch và thuyết minh (vi, en)
INSERT INTO artifact_contents (id, artifact_id, language_code, title, description, audio_url, video_url, image_urls, transcript, created_at, updated_at) VALUES
    (1,1,'vi','Khuê Văn Các','Khuê Văn Các là một công trình tiêu biểu trong khu di tích Văn Miếu - Quốc Tử Giám. Các ô cửa tròn và không gian bên giếng Thiên Quang tạo nên hình ảnh quen thuộc của di sản Hà Nội.',NULL,NULL,'[\"http://localhost:8080/api/media/files/64cb00c4-83c1-47d2-89d6-aa8e9f6bcdf6.jpg\"]',NULL,'2026-11-19 13:18:06.318419','2026-11-19 13:19:30.681361');
INSERT INTO artifact_contents (id, artifact_id, language_code, title, description, audio_url, video_url, image_urls, transcript, created_at, updated_at) VALUES
    (2,1,'en','Constellation of Literature Pavilion','The Constellation of Literature Pavilion is a landmark within Hanoi\'s Temple of Literature. Its circular windows and position beside the Thien Quang well make it a distinctive part of the heritage complex.','http://localhost:8080/api/media/files/0dd6f7ac-f296-4176-b0a9-749787d863e3.wav',NULL,'[\"http://localhost:8080/api/media/files/64cb00c4-83c1-47d2-89d6-aa8e9f6bcdf6.jpg\"]','The Constellation of Literature Pavilion is a landmark within Hanoi\'s Temple of Literature. Its circular windows and position beside the Thien Quang well make it a distinctive part of the heritage complex.','2026-11-19 13:18:06.318419','2026-11-19 13:19:30.712051');
INSERT INTO artifact_contents (id, artifact_id, language_code, title, description, audio_url, video_url, image_urls, transcript, created_at, updated_at) VALUES
    (3,2,'vi','Bia Tiến sĩ','Những tấm bia đá tại Văn Miếu lưu danh người đỗ các kỳ thi thời phong kiến. Bia đặt trên lưng rùa đá, thể hiện truyền thống trọng học và lưu giữ dấu ấn nghệ thuật của nhiều thời kỳ.',NULL,NULL,'[\"http://localhost:8080/api/media/files/2ca46f7f-9cf1-4d7f-8081-15f336b0edaa.jpg\"]',NULL,'2026-11-19 13:18:06.318419','2026-11-19 13:19:31.628071');
INSERT INTO artifact_contents (id, artifact_id, language_code, title, description, audio_url, video_url, image_urls, transcript, created_at, updated_at) VALUES
    (4,2,'en','Doctors\' Stelae','The stone stelae at the Temple of Literature record successful candidates in historical royal examinations. Resting on stone tortoises, they preserve memories of learning and the artistic styles of different periods.','http://localhost:8080/api/media/files/31938974-5e52-4b5c-b3dd-2043c3fa9c76.wav',NULL,'[\"http://localhost:8080/api/media/files/2ca46f7f-9cf1-4d7f-8081-15f336b0edaa.jpg\"]','The stone stelae at the Temple of Literature record successful candidates in historical royal examinations. Resting on stone tortoises, they preserve memories of learning and the artistic styles of different periods.','2026-11-19 13:18:06.318419','2026-11-19 13:19:31.645470');
INSERT INTO artifact_contents (id, artifact_id, language_code, title, description, audio_url, video_url, image_urls, transcript, created_at, updated_at) VALUES
    (5,3,'vi','Đoan Môn','Đoan Môn là cửa phía nam dẫn vào khu vực trung tâm Cấm thành Thăng Long. Công trình nằm trên trục không gian nối với nền điện Kính Thiên, giúp hình dung tổ chức của khu cung điện xưa.',NULL,NULL,'[\"http://localhost:8080/api/media/files/c7432a39-79c9-426e-99c2-e925b1bef6fc.jpg\"]',NULL,'2026-11-19 13:18:06.318419','2026-11-19 13:19:32.586579');
INSERT INTO artifact_contents (id, artifact_id, language_code, title, description, audio_url, video_url, image_urls, transcript, created_at, updated_at) VALUES
    (6,4,'vi','Đại Trung Môn','Đại Trung Môn nằm trên tuyến tham quan Văn Miếu - Quốc Tử Giám. Cổng chính và các lối đi bên dẫn du khách vào không gian sân vườn của di tích trước khi đến Khuê Văn Các.',NULL,NULL,'[\"http://localhost:8080/api/media/files/6c291582-1787-42c9-8e6a-28a34abccaa3.jpg\"]',NULL,'2026-11-19 13:19:32.097022','2026-11-19 13:19:32.097046');
INSERT INTO artifact_contents (id, artifact_id, language_code, title, description, audio_url, video_url, image_urls, transcript, created_at, updated_at) VALUES
    (7,4,'en','Dai Trung Gate','Dai Trung Gate stands along the visitor route through the Temple of Literature. The central gate and side passages lead into the complex\'s garden courtyards before visitors reach the Constellation of Literature Pavilion.','http://localhost:8080/api/media/files/dd657bdb-abf8-4aab-afb0-cf681a17bd17.wav',NULL,'[\"http://localhost:8080/api/media/files/6c291582-1787-42c9-8e6a-28a34abccaa3.jpg\"]','Dai Trung Gate stands along the visitor route through the Temple of Literature. The central gate and side passages lead into the complex\'s garden courtyards before visitors reach the Constellation of Literature Pavilion.','2026-11-19 13:19:32.097054','2026-11-19 13:19:32.097057');
INSERT INTO artifact_contents (id, artifact_id, language_code, title, description, audio_url, video_url, image_urls, transcript, created_at, updated_at) VALUES
    (8,3,'en','Doan Mon Gate','Doan Mon is the southern gateway into the central area of the former imperial enclosure. Its relationship to the Kinh Thien Palace site helps visitors understand the arrangement of the historic royal complex.','http://localhost:8080/api/media/files/29cacba8-046d-4e1a-9355-9851766727f5.wav',NULL,'[\"http://localhost:8080/api/media/files/c7432a39-79c9-426e-99c2-e925b1bef6fc.jpg\"]','Doan Mon is the southern gateway into the central area of the former imperial enclosure. Its relationship to the Kinh Thien Palace site helps visitors understand the arrangement of the historic royal complex.','2026-11-19 13:19:32.591161','2026-11-19 13:19:32.591179');
INSERT INTO artifact_contents (id, artifact_id, language_code, title, description, audio_url, video_url, image_urls, transcript, created_at, updated_at) VALUES
    (9,5,'vi','Nền điện Kính Thiên','Khu vực điện Kính Thiên giữ vị trí trung tâm của Hoàng thành Thăng Long. Nền điện và những dấu tích còn lại giúp du khách hình dung không gian cung đình; ảnh kèm theo là tư liệu lịch sử, không phải hiện trạng công trình.',NULL,NULL,'[\"http://localhost:8080/api/media/files/88ffa886-45e4-4083-b7bb-495a0bd47d5e.jpg\"]',NULL,'2026-11-19 13:19:32.938040','2026-11-19 13:19:32.938054');
INSERT INTO artifact_contents (id, artifact_id, language_code, title, description, audio_url, video_url, image_urls, transcript, created_at, updated_at) VALUES
    (10,5,'en','Kinh Thien Palace Site','The Kinh Thien Palace site occupies a central position in the Imperial Citadel. Surviving remains help visitors imagine the royal setting. The accompanying photograph is historical documentation and does not show the site\'s present condition.','http://localhost:8080/api/media/files/39842220-863e-4180-9f86-790afd324d21.wav',NULL,'[\"http://localhost:8080/api/media/files/88ffa886-45e4-4083-b7bb-495a0bd47d5e.jpg\"]','The Kinh Thien Palace site occupies a central position in the Imperial Citadel. Surviving remains help visitors imagine the royal setting. The accompanying photograph is historical documentation and does not show the site\'s present condition.','2026-11-19 13:19:32.938061','2026-11-19 13:19:32.938063');
INSERT INTO artifact_contents (id, artifact_id, language_code, title, description, audio_url, video_url, image_urls, transcript, created_at, updated_at) VALUES
    (11,6,'vi','Cột cờ Hà Nội','Cột cờ Hà Nội là một công trình lịch sử còn lại của khu thành Hà Nội. Nằm về phía nam trục trung tâm, công trình giúp du khách liên hệ vị trí Đoan Môn và khu vực điện Kính Thiên.',NULL,NULL,'[\"http://localhost:8080/api/media/files/f6be815f-07a1-459a-baed-94ca3bc08ab8.jpg\"]',NULL,'2026-11-19 13:19:33.270633','2026-11-19 13:19:33.270648');
INSERT INTO artifact_contents (id, artifact_id, language_code, title, description, audio_url, video_url, image_urls, transcript, created_at, updated_at) VALUES
    (12,6,'en','Hanoi Flag Tower','Hanoi Flag Tower is a surviving historic structure of the former Hanoi citadel. Its location to the south helps visitors relate the city\'s historic landscape to Doan Mon Gate and the Kinh Thien Palace site.','http://localhost:8080/api/media/files/6c39f2c4-f3a1-47ee-bdc2-84efb8aee564.wav',NULL,'[\"http://localhost:8080/api/media/files/f6be815f-07a1-459a-baed-94ca3bc08ab8.jpg\"]','Hanoi Flag Tower is a surviving historic structure of the former Hanoi citadel. Its location to the south helps visitors relate the city\'s historic landscape to Doan Mon Gate and the Kinh Thien Palace site.','2026-11-19 13:19:33.270655','2026-11-19 13:19:33.270657');
ALTER TABLE artifact_contents AUTO_INCREMENT = 13;

-- Tài nguyên đa phương tiện
INSERT INTO media_resources (id, artifact_id, media_type, file_url, caption) VALUES
    (4,1,'IMAGE','http://localhost:8080/api/media/files/64cb00c4-83c1-47d2-89d6-aa8e9f6bcdf6.jpg','Khuê Văn Các và giếng Thiên Quang');
INSERT INTO media_resources (id, artifact_id, media_type, file_url, caption) VALUES
    (5,2,'IMAGE','http://localhost:8080/api/media/files/2ca46f7f-9cf1-4d7f-8081-15f336b0edaa.jpg','Bia đá và rùa đá tại Văn Miếu');
INSERT INTO media_resources (id, artifact_id, media_type, file_url, caption) VALUES
    (6,4,'IMAGE','http://localhost:8080/api/media/files/6c291582-1787-42c9-8e6a-28a34abccaa3.jpg','Đại Trung Môn tại Văn Miếu');
INSERT INTO media_resources (id, artifact_id, media_type, file_url, caption) VALUES
    (7,3,'IMAGE','http://localhost:8080/api/media/files/c7432a39-79c9-426e-99c2-e925b1bef6fc.jpg','Cổng Đoan Môn, ảnh chụp tháng 5 năm 2007');
INSERT INTO media_resources (id, artifact_id, media_type, file_url, caption) VALUES
    (8,5,'IMAGE','http://localhost:8080/api/media/files/88ffa886-45e4-4083-b7bb-495a0bd47d5e.jpg','Ảnh tư liệu điện Kính Thiên khoảng 1884–1885, Charles-Edouard Hocquard');
INSERT INTO media_resources (id, artifact_id, media_type, file_url, caption) VALUES
    (9,6,'IMAGE','http://localhost:8080/api/media/files/f6be815f-07a1-459a-baed-94ca3bc08ab8.jpg','Cột cờ Hà Nội');
ALTER TABLE media_resources AUTO_INCREMENT = 10;
