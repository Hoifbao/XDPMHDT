-- Optional migration from the original schema, NOT run automatically.
-- Back up first. Stop writes and start services once to create target tables.
-- Run only on EMPTY site_db/content_db. If targets have data, reconcile IDs first.
-- Duplicate IDs/codes/UUIDs cause an error rather than silently overwriting data.
START TRANSACTION;
INSERT INTO site_db.heritage_sites (id, name, location, map_image_url, latitude, longitude)
SELECT id, name, location, map_image_url, latitude, longitude
FROM smart_heritage_db.heritage_sites;

INSERT INTO content_db.artifacts
 (id, site_id, site_name, name, artifact_code, description, thumbnail_url,
  period, material, dimensions, beacon_uuid, status, created_at, updated_at)
SELECT a.id, a.site_id, s.name, a.name, NULLIF(a.artifact_code,''), a.description,
 a.thumbnail_url, a.period, a.material, a.dimensions, NULLIF(a.beacon_uuid,''),
 a.status, a.created_at, a.updated_at
FROM smart_heritage_db.artifacts a
JOIN smart_heritage_db.heritage_sites s ON s.id = a.site_id;

INSERT INTO content_db.artifact_contents
 (id, artifact_id, language_code, title, description, audio_url, video_url,
  image_urls, transcript, created_at, updated_at)
SELECT id, artifact_id, language_code, title, description, audio_url, video_url,
 image_urls, transcript, created_at, updated_at
FROM smart_heritage_db.artifact_contents;

-- Supply a Vietnamese fallback for legacy artifacts lacking a vi row.
INSERT INTO content_db.artifact_contents
 (artifact_id, language_code, title, description, created_at, updated_at)
SELECT a.id, 'vi', a.name, COALESCE(a.description,''), NOW(), NOW()
FROM content_db.artifacts a
WHERE NOT EXISTS (SELECT 1 FROM content_db.artifact_contents c
                 WHERE c.artifact_id = a.id AND c.language_code = 'vi');

INSERT INTO content_db.media_resources (id, artifact_id, media_type, file_url, caption)
SELECT id, artifact_id, media_type, file_url, caption
FROM smart_heritage_db.media_resources;
COMMIT;
