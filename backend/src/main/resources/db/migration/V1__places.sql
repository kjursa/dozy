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
    ownership TEXT NOT NULL CHECK (ownership IN ('CUSTOM', 'PREDEFINED')),
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (
        (ownership = 'PREDEFINED' AND installation_id IS NULL AND substr(id, 1, 8) = 'builtin:')
        OR (ownership = 'CUSTOM' AND installation_id IS NOT NULL AND substr(id, 1, 8) != 'builtin:')
    )
);
CREATE INDEX places_installation_idx ON places(installation_id);
INSERT INTO places(id, name, description, inhabitants, ownership) VALUES
('builtin:woods', 'Whispering woods', 'A peaceful forest with glowing paths and old trees.', 'Gentle fireflies and an owl who knows every woodland path.', 'PREDEFINED'),
('builtin:island', 'Starlight island', 'A little island with sandy coves and a sea that glows at night.', NULL, 'PREDEFINED'),
('builtin:village', 'Cloudtop village', 'A cosy village floating between soft clouds.', 'Cloud gardeners and sleepy birds delivering letters.', 'PREDEFINED');
