-- =====================================================================
--  Tuan 8 (Bao): index bao trum cho fact_visits tren DB dang co du lieu.
--  Loc Dashboard theo di tich voi 1 trieu luot mat ~25 s vi MySQL phai quay lai bang
--  lay user_id, duration_seconds cho tung dong. Index moi chua san cac cot do.
--
--  Chay (PowerShell, dung o thu muc repo), chay lai nhieu lan khong sao:
--    Get-Content Infra/analytics_w8_cover_index.sql | docker exec -i smart_heritage_mysql mysql -uroot -prootpassword123
--
--  DB moi (volume trong): KHONG can chay, Infra/db/07_analytics.sql da co index moi.
--  File de ngoai Infra/db vi MySQL tu chay moi file trong thu muc do khi khoi tao.
-- =====================================================================
USE analytics_db;

SET @has_new := (SELECT COUNT(*) > 0 FROM information_schema.statistics
                 WHERE table_schema = 'analytics_db' AND table_name = 'fact_visits'
                   AND index_name = 'idx_fact_visits_artifact_cover');
SET @has_old := (SELECT COUNT(*) > 0 FROM information_schema.statistics
                 WHERE table_schema = 'analytics_db' AND table_name = 'fact_visits'
                   AND index_name = 'idx_fact_visits_artifact_date');

-- 1. Them index moi (artifact_id, detected_at, user_id, duration_seconds)
SET @sql := IF(@has_new,
    'SELECT ''idx_fact_visits_artifact_cover da co, bo qua'' AS info',
    'ALTER TABLE fact_visits ADD KEY idx_fact_visits_artifact_cover (artifact_id, detected_at, user_id, duration_seconds)');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. Bo index cu (index moi bat dau bang dung 2 cot cua index cu nen thay the duoc)
SET @sql := IF(@has_old,
    'ALTER TABLE fact_visits DROP KEY idx_fact_visits_artifact_date',
    'SELECT ''khong co idx_fact_visits_artifact_date, bo qua'' AS info');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. Cap nhat thong ke de MySQL chon dung index
ANALYZE TABLE fact_visits;

SELECT index_name, GROUP_CONCAT(column_name ORDER BY seq_in_index) AS columns
FROM information_schema.statistics
WHERE table_schema = 'analytics_db' AND table_name = 'fact_visits'
GROUP BY index_name;
