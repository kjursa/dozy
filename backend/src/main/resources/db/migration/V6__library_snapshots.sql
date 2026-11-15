CREATE TABLE library_snapshots (
 id TEXT PRIMARY KEY NOT NULL,
 installation_id TEXT NOT NULL REFERENCES installations(id),
 page_limit INTEGER NOT NULL,
 summaries TEXT NOT NULL
);
CREATE TABLE library_cursors (
 token TEXT PRIMARY KEY NOT NULL,
 snapshot_id TEXT NOT NULL REFERENCES library_snapshots(id),
 position INTEGER NOT NULL,
 UNIQUE(snapshot_id, position)
);
