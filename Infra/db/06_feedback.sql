-- =====================================================================
--  feedback_db - feedback-service (Tường, cổng 8086, tách ở tuần 5)
--  Script: Tường - Sprint 4
--  Bảng: feedbacks
--  - user_id, artifact_id KHÔNG còn khóa ngoại (users ở auth_db, artifacts ở content_db).
--  - reviewer_name: tên hiển thị của người đánh giá, chụp lại (snapshot) lúc gửi
--    từ auth-service /api/auth/me, vì feedback-service không đọc được bảng users.
--    NULL thì API hiển thị "Khách". Không lưu email/username.
--  - Id user (1..4) và id hiện vật (1..3) giữ nguyên như auth_db, content_db.
--  - Tuần 5 (chặn spam): mỗi người chỉ có 1 đánh giá cho 1 hiện vật (uq_feedbacks_user_artifact).
--    Gửi lại thì ghi đè đánh giá cũ và đưa về PENDING để ADMIN duyệt lại.
--  - Tuần 5 (NiFi): updated_at đổi mỗi khi sửa đánh giá hoặc ADMIN đổi trạng thái.
--    Luồng NiFi feedbacks -> fact_feedbacks dùng updated_at làm cột mốc (Maximum-value Column)
--    để bắt được cả lúc status đổi, không chỉ dòng mới.
--    Giá trị do feedback-service gán (cùng đồng hồ với created_at); DEFAULT/ON UPDATE chỉ để dự phòng.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS feedback_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE feedback_db;
SET NAMES utf8mb4;

-- Entity: Feedback (Tường). Status: PENDING | APPROVED | REJECTED
CREATE TABLE IF NOT EXISTS feedbacks (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    user_id       BIGINT       NOT NULL,               -- id trong auth_db.users (không FK)
    artifact_id   BIGINT       NOT NULL,               -- id trong content_db.artifacts (không FK)
    reviewer_name VARCHAR(100) NULL,
    rating        INT          NOT NULL,
    comment       TEXT         NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at    DATETIME(6)  NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6)  NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uq_feedbacks_user_artifact (user_id, artifact_id),
    KEY idx_feedbacks_artifact_status (artifact_id, status),
    KEY idx_feedbacks_status (status),
    KEY idx_feedbacks_updated_at (updated_at),
    CONSTRAINT chk_feedbacks_rating CHECK (rating BETWEEN 1 AND 5)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- Đủ 3 trạng thái, có cả bình luận NULL (giống smart_heritage_db). Mỗi cặp (user, hiện vật) chỉ 1 dòng.
INSERT INTO feedbacks (id, user_id, artifact_id, reviewer_name, rating, comment, status, created_at, updated_at) VALUES
    (1, 2, 1, 'Nguyễn Văn An', 5, 'Khuê Văn Các rất đẹp, phần thuyết minh dễ hiểu.',   'APPROVED', '2026-10-05 09:30:00', '2026-10-05 09:30:00'),
    (2, 2, 2, 'Nguyễn Văn An', 4, 'Thông tin về bia Tiến sĩ hay, mong có thêm video.',  'APPROVED', '2026-10-05 09:45:00', '2026-10-05 09:45:00'),
    (3, 3, 1, 'Trần Thị Mai',  3, 'Tín hiệu beacon hơi chậm khi đông người.',           'PENDING',  '2026-10-06 14:20:00', '2026-10-06 14:20:00'),
    (4, 4, 3, 'John Smith',    5, 'The English audio guide is clear and helpful.',      'APPROVED', '2026-10-07 10:50:00', '2026-10-07 10:50:00'),
    (5, 3, 2, 'Trần Thị Mai',  2, NULL,                                                  'PENDING',  '2026-10-06 14:40:00', '2026-10-06 14:40:00'),
    (6, 4, 1, 'John Smith',    1, 'Nội dung quảng cáo không liên quan đến hiện vật.',   'REJECTED', '2026-10-07 11:00:00', '2026-10-07 11:00:00');
