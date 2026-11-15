CREATE TABLE assets (
    id TEXT PRIMARY KEY NOT NULL,
    installation_id TEXT NOT NULL REFERENCES installations(id),
    url TEXT NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('PENDING', 'READY', 'FAILED'))
);
CREATE INDEX assets_installation_idx ON assets(installation_id);
CREATE TABLE characters (
    id TEXT PRIMARY KEY NOT NULL,
    installation_id TEXT NOT NULL REFERENCES installations(id),
    name TEXT NOT NULL,
    personality TEXT NOT NULL,
    appearance TEXT NOT NULL,
    type TEXT NOT NULL CHECK (type IN ('CHILD', 'ANIMAL', 'FANTASY', 'OTHER')),
    avatar_id TEXT REFERENCES assets(id),
    avatar_url TEXT,
    deleted INTEGER NOT NULL DEFAULT 0 CHECK (deleted IN (0, 1)),
    CHECK ((avatar_id IS NULL) = (avatar_url IS NULL))
);
CREATE INDEX characters_installation_idx ON characters(installation_id);
