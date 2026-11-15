ALTER TABLE story_jobs ADD COLUMN execution_id TEXT NOT NULL DEFAULT '';
ALTER TABLE story_jobs ADD COLUMN ordinal INTEGER NOT NULL DEFAULT 1;
CREATE TABLE story_executions (
 id TEXT PRIMARY KEY NOT NULL,
 installation_id TEXT NOT NULL REFERENCES installations(id),
 job_id TEXT NOT NULL REFERENCES story_jobs(id),
 idempotency_key TEXT NOT NULL,
 operation TEXT NOT NULL,
 ordinal INTEGER NOT NULL,
 stage TEXT NOT NULL,
 UNIQUE(installation_id, idempotency_key),
 UNIQUE(job_id, ordinal)
);
UPDATE story_jobs SET execution_id = id;
INSERT INTO story_executions SELECT id, installation_id, id, idempotency_key, operation, 1, stage FROM story_jobs;
