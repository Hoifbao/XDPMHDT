-- =====================================================================
--  Tuan 8 (Bao): nap 1.000.000 luot xem THU vao analytics_db de do toc do Dashboard.
--  Lam lai bai do cua Tuan 21/11 (bench_1m.log) tren may dang chay, khong can NiFi.
--
--  Du lieu thu:
--   - visit_id 900000000 .. 900999999, user_id 900000 .. 904999 (5.000 khach) -> khong trung du lieu that
--   - hien vat lay tu content_db.artifacts (chia deu), ngay trong 60 ngay gan nhat, 8h - 18h
--   - dim_site, dim_artifact chep tu site_db, content_db (INSERT IGNORE: da co thi giu nguyen)
--
--  Nap (PowerShell, dung o thu muc repo; mat khoang 1 - 3 phut):
--    Get-Content Infra/loadtest/analytics_1m_bench.sql | docker exec -i smart_heritage_mysql mysql -uroot -prootpassword123
--
--  XOA du lieu thu sau khi do (bat buoc, de Dashboard ve lai so that):
--    docker exec smart_heritage_mysql mysql -uroot -prootpassword123 -e "DELETE FROM analytics_db.fact_visits WHERE visit_id >= 900000000; DELETE FROM analytics_db.dim_user_lang WHERE user_id BETWEEN 900000 AND 904999;"
-- =====================================================================
USE analytics_db;

-- Dim: ten di tich, hien vat that (Dashboard can de loc theo di tich)
INSERT IGNORE INTO dim_site (site_id, name, location, latitude, longitude)
SELECT id, name, location, latitude, longitude FROM site_db.heritage_sites;

INSERT IGNORE INTO dim_artifact (artifact_id, code, name, site_id, site_name)
SELECT a.id, a.artifact_code, a.name, a.site_id, s.name
FROM content_db.artifacts a
LEFT JOIN site_db.heritage_sites s ON s.id = a.site_id;

-- Bang phu (bang thuong, khong dung TEMPORARY vi MySQL khong cho JOIN 1 bang tam nhieu lan)
DROP TABLE IF EXISTS bench_digits;
CREATE TABLE bench_digits (d INT NOT NULL PRIMARY KEY);
INSERT INTO bench_digits VALUES (0),(1),(2),(3),(4),(5),(6),(7),(8),(9);

DROP TABLE IF EXISTS bench_artifacts;
CREATE TABLE bench_artifacts AS
SELECT ROW_NUMBER() OVER (ORDER BY artifact_id) - 1 AS rn, artifact_id FROM dim_artifact;
SET @n_art := (SELECT COUNT(*) FROM bench_artifacts);

-- Ngon ngu cua 5.000 khach thu: vi / en / fr
INSERT IGNORE INTO dim_user_lang (user_id, language_code)
SELECT 900000 + n, ELT(1 + n MOD 3, 'vi', 'en', 'fr')
FROM (SELECT d1.d + d2.d * 10 + d3.d * 100 + d4.d * 1000 AS n
      FROM bench_digits d1, bench_digits d2, bench_digits d3, bench_digits d4) x
WHERE n < 5000;

-- 1.000.000 luot xem
INSERT IGNORE INTO fact_visits (visit_id, user_id, artifact_id, site_id, language_code, detected_at, duration_seconds)
SELECT 900000000 + x.n,
       900000 + x.n MOD 5000,
       ba.artifact_id,
       NULL,
       NULL,
       TIMESTAMP(CURDATE() - INTERVAL (x.n MOD 60) DAY) + INTERVAL (8 * 3600 + (x.n * 7) MOD 36000) SECOND,
       10 + (x.n * 13) MOD 290
FROM (SELECT d1.d + d2.d * 10 + d3.d * 100 + d4.d * 1000 + d5.d * 10000 + d6.d * 100000 AS n
      FROM bench_digits d1, bench_digits d2, bench_digits d3,
           bench_digits d4, bench_digits d5, bench_digits d6) x
JOIN bench_artifacts ba ON ba.rn = x.n MOD @n_art;

DROP TABLE bench_digits;
DROP TABLE bench_artifacts;
ANALYZE TABLE fact_visits;

SELECT COUNT(*) AS luot_thu FROM fact_visits WHERE visit_id >= 900000000;
SELECT a.site_id, a.site_name, COUNT(*) AS luot
FROM fact_visits v JOIN dim_artifact a ON a.artifact_id = v.artifact_id
WHERE v.visit_id >= 900000000
GROUP BY a.site_id, a.site_name;
