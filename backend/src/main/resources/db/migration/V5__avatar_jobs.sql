CREATE TABLE avatar_jobs (
    id TEXT PRIMARY KEY NOT NULL,
    owner_id TEXT NOT NULL REFERENCES installations(id),
    type TEXT NOT NULL,
    personality TEXT NOT NULL,
    appearance TEXT NOT NULL,
    asset_id TEXT NOT NULL UNIQUE,
    url TEXT NOT NULL,
    state TEXT NOT NULL CHECK (state IN ('QUEUED', 'RUNNING', 'READY', 'FAILED'))
);
CREATE INDEX avatar_jobs_state_idx ON avatar_jobs(state);
