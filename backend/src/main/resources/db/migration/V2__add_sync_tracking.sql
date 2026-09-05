ALTER TABLE profile ADD COLUMN content_hash VARCHAR(64);

CREATE TABLE sync_job
(
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    started_at              TIMESTAMP NOT NULL,
    finished_at             TIMESTAMP,
    status                  VARCHAR(20) NOT NULL,
    triggered_by            VARCHAR(20) NOT NULL,
    source_url              VARCHAR(1024) NOT NULL,
    profiles_created        INT NOT NULL DEFAULT 0,
    profiles_updated        INT NOT NULL DEFAULT 0,
    profiles_unchanged      INT NOT NULL DEFAULT 0,
    profiles_failed         INT NOT NULL DEFAULT 0,
    error_message           TEXT
);

CREATE INDEX idx_sync_job_started_at ON sync_job (started_at DESC);
