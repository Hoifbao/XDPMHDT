-- Additive migration for content_db only. Existing dev configuration (ddl-auto: update)
-- also creates this table. No reset of MySQL volumes is required for this migration.
CREATE TABLE IF NOT EXISTS content_db.artifact_event_outbox (
    event_id VARCHAR(36) NOT NULL PRIMARY KEY,
    occurred_at DATETIME(6) NOT NULL,
    payload TEXT NOT NULL,
    published_at DATETIME(6) NULL,
    INDEX idx_artifact_outbox_pending (published_at, occurred_at)
);
