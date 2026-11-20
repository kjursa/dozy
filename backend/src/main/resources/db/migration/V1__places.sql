CREATE TABLE installations (
    id TEXT PRIMARY KEY NOT NULL,
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE places (
    id TEXT PRIMARY KEY NOT NULL CHECK (length(trim(id)) > 0),
    owner_id TEXT REFERENCES installations(id),
    name TEXT NOT NULL CHECK (length(trim(name)) > 0),
    description TEXT NOT NULL CHECK (length(trim(description)) > 0),
    inhabitants TEXT,
    origin TEXT NOT NULL CHECK (origin IN ('USER', 'SYSTEM')),
    thumbnail TEXT NOT NULL DEFAULT 'COTTAGE' CHECK (thumbnail IN ('COTTAGE', 'FOREST')),
    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CHECK (
        (origin = 'SYSTEM' AND owner_id IS NULL)
        OR (origin = 'USER' AND owner_id IS NOT NULL)
    )
);
CREATE INDEX places_owner_idx ON places(owner_id);
INSERT INTO places(id, name, description, inhabitants, origin, thumbnail) VALUES
('place-00000000-0000-7000-8000-000000000001', 'Whispering woods', 'A peaceful forest with glowing paths and old trees.', 'Gentle fireflies and an owl who knows every woodland path.', 'SYSTEM', 'FOREST'),
('place-00000000-0000-7000-8000-000000000002', 'Starlight island', 'A little island with sandy coves and a sea that glows at night.', NULL, 'SYSTEM', 'COTTAGE'),
('place-00000000-0000-7000-8000-000000000003', 'Cloudtop village', 'A cosy village floating between soft clouds.', 'Cloud gardeners and sleepy birds delivering letters.', 'SYSTEM', 'COTTAGE');
