CREATE TABLE story_jobs_new (
    id TEXT PRIMARY KEY NOT NULL,
    installation_id TEXT NOT NULL REFERENCES installations(id),
    operation TEXT NOT NULL,
    idempotency_key TEXT NOT NULL,
    canonical_request TEXT NOT NULL,
    configuration TEXT NOT NULL,
    stage TEXT NOT NULL CHECK(stage IN ('QUEUED','CREATING_TEXT','CREATING_COVER','READY','FAILED')),
    asset_id TEXT NOT NULL UNIQUE,
    created_at INTEGER NOT NULL,
    title TEXT,
    text TEXT,
    error_code TEXT CHECK(error_code IN ('CONTENT_REJECTED','GENERATION_FAILED')),
    lease_token TEXT,
    lease_until INTEGER NOT NULL DEFAULT 0,
    execution_count INTEGER NOT NULL DEFAULT 0,
    execution_id TEXT NOT NULL DEFAULT '',
    ordinal INTEGER NOT NULL DEFAULT 1,
    UNIQUE(installation_id, idempotency_key),
    CHECK ((stage = 'FAILED') = (error_code IS NOT NULL)),
    CHECK (stage NOT IN ('CREATING_COVER','READY') OR (text IS NOT NULL AND title IS NOT NULL))
);
CREATE TABLE stories_new (
    id TEXT PRIMARY KEY NOT NULL,
    job_id TEXT NOT NULL UNIQUE REFERENCES story_jobs_new(id),
    installation_id TEXT NOT NULL REFERENCES installations(id),
    title TEXT NOT NULL,
    text TEXT NOT NULL,
    cover_url TEXT NOT NULL,
    configuration TEXT NOT NULL,
    created_at INTEGER NOT NULL
);
CREATE TABLE story_executions_new (
 id TEXT PRIMARY KEY NOT NULL,
 installation_id TEXT NOT NULL REFERENCES installations(id),
 job_id TEXT NOT NULL REFERENCES story_jobs_new(id),
 idempotency_key TEXT NOT NULL,
 operation TEXT NOT NULL,
 ordinal INTEGER NOT NULL,
 stage TEXT NOT NULL,
 UNIQUE(installation_id, idempotency_key),
 UNIQUE(job_id, ordinal)
);

INSERT INTO story_jobs_new
SELECT id, installation_id, operation, idempotency_key, canonical_request, configuration, stage,
       asset_id, created_at, title, text, error_code, lease_token, lease_until, execution_count, execution_id, ordinal
FROM story_jobs;
INSERT INTO stories_new SELECT * FROM stories;
INSERT INTO story_executions_new SELECT * FROM story_executions;

DROP TABLE story_executions;
DROP TABLE stories;
DROP TABLE story_jobs;
ALTER TABLE story_jobs_new RENAME TO story_jobs;
ALTER TABLE stories_new RENAME TO stories;
ALTER TABLE story_executions_new RENAME TO story_executions;

CREATE UNIQUE INDEX one_active_story_job ON story_jobs(installation_id) WHERE stage IN ('QUEUED','CREATING_TEXT','CREATING_COVER');
CREATE INDEX stories_by_owner_and_id ON stories(installation_id, id DESC);

DROP TABLE library_cursors;
DROP TABLE library_snapshots;
