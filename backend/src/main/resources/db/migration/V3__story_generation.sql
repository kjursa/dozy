CREATE TABLE story_jobs (
    id TEXT PRIMARY KEY NOT NULL,
    owner_id TEXT NOT NULL REFERENCES installations(id),
    operation TEXT NOT NULL,
    idempotency_key TEXT NOT NULL,
    canonical_request TEXT NOT NULL,
    configuration TEXT NOT NULL,
    stage TEXT NOT NULL CHECK(stage IN ('QUEUED','CREATING_TEXT','CREATING_COVER','READY','FAILED')),
    story_id TEXT NOT NULL UNIQUE,
    asset_id TEXT NOT NULL UNIQUE,
    created_at INTEGER NOT NULL,
    title TEXT,
    text TEXT,
    error_code TEXT CHECK(error_code IN ('CONTENT_REJECTED','GENERATION_FAILED')),
    lease_token TEXT,
    lease_until INTEGER NOT NULL DEFAULT 0,
    execution_count INTEGER NOT NULL DEFAULT 0,
    UNIQUE(owner_id, idempotency_key),
    CHECK ((stage = 'FAILED') = (error_code IS NOT NULL)),
    CHECK (stage NOT IN ('CREATING_COVER','READY') OR (text IS NOT NULL AND title IS NOT NULL))
);
CREATE UNIQUE INDEX one_active_story_job ON story_jobs(owner_id) WHERE stage IN ('QUEUED','CREATING_TEXT','CREATING_COVER');
CREATE TABLE stories (
    id TEXT PRIMARY KEY NOT NULL,
    job_id TEXT NOT NULL UNIQUE REFERENCES story_jobs(id),
    owner_id TEXT NOT NULL REFERENCES installations(id),
    title TEXT NOT NULL,
    text TEXT NOT NULL,
    cover_url TEXT NOT NULL,
    configuration TEXT NOT NULL,
    created_at INTEGER NOT NULL
);
CREATE TABLE asset_contents (
    asset_id TEXT PRIMARY KEY NOT NULL REFERENCES assets(id),
    bytes BLOB NOT NULL
);
