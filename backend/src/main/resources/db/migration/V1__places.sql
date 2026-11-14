CREATE TABLE installations (
    id TEXT PRIMARY KEY NOT NULL,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE places (
    id TEXT PRIMARY KEY NOT NULL CHECK (length(trim(id)) > 0),
    installation_id TEXT REFERENCES installations(id),
    name TEXT NOT NULL CHECK (length(trim(name)) > 0),
    description TEXT NOT NULL CHECK (length(trim(description)) > 0),
    inhabitants TEXT,
    origin TEXT NOT NULL CHECK (origin IN ('USER', 'SYSTEM')),
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (
        (origin = 'SYSTEM' AND installation_id IS NULL AND substr(id, 1, 8) = 'builtin:')
        OR (origin = 'USER' AND installation_id IS NOT NULL AND substr(id, 1, 8) != 'builtin:')
    )
);
CREATE INDEX places_installation_idx ON places(installation_id);
INSERT INTO places(id, name, description, inhabitants, origin) VALUES
('builtin:woods', 'Whispering woods', 'A peaceful forest with glowing paths and old trees.', 'Gentle fireflies and an owl who knows every woodland path.', 'SYSTEM'),
('builtin:island', 'Starlight island', 'A little island with sandy coves and a sea that glows at night.', NULL, 'SYSTEM'),
('builtin:village', 'Cloudtop village', 'A cosy village floating between soft clouds.', 'Cloud gardeners and sleepy birds delivering letters.', 'SYSTEM');
