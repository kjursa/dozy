CREATE TABLE collection_etags (
    collection TEXT NOT NULL CHECK (collection IN ('characters', 'places')),
    owner_id TEXT NOT NULL,
    version TEXT NOT NULL DEFAULT (
        lower(hex(randomblob(4)) || '-' || hex(randomblob(2)) ||
              '-4' || substr(hex(randomblob(2)), 2) ||
              '-' || substr('89ab', (random() & 3) + 1, 1) || substr(hex(randomblob(2)), 2) ||
              '-' || hex(randomblob(6)))
    ),
    PRIMARY KEY (collection, owner_id)
);

INSERT INTO collection_etags(collection, owner_id)
SELECT 'characters', owner_id FROM characters GROUP BY owner_id
UNION
SELECT 'places', COALESCE(owner_id, '') FROM places GROUP BY owner_id
UNION
SELECT 'places', '';

CREATE TRIGGER version_character_insert AFTER INSERT ON characters
WHEN NEW.deleted = 0
BEGIN
    INSERT INTO collection_etags(collection, owner_id) VALUES ('characters', NEW.owner_id)
    ON CONFLICT (collection, owner_id) DO UPDATE SET version = excluded.version;
END;

CREATE TRIGGER version_character_update AFTER UPDATE ON characters
WHEN (OLD.deleted = 0 OR NEW.deleted = 0) AND (
    OLD.id IS NOT NEW.id OR OLD.owner_id IS NOT NEW.owner_id OR OLD.name IS NOT NEW.name OR
    OLD.personality IS NOT NEW.personality OR OLD.appearance IS NOT NEW.appearance OR
    OLD.type IS NOT NEW.type OR OLD.avatar_id IS NOT NEW.avatar_id OR
    OLD.avatar_url IS NOT NEW.avatar_url OR OLD.deleted IS NOT NEW.deleted
)
BEGIN
    INSERT INTO collection_etags(collection, owner_id) VALUES ('characters', OLD.owner_id)
    ON CONFLICT (collection, owner_id) DO UPDATE SET version = excluded.version;
    INSERT INTO collection_etags(collection, owner_id)
    SELECT 'characters', NEW.owner_id WHERE OLD.owner_id IS NOT NEW.owner_id
    ON CONFLICT (collection, owner_id) DO UPDATE SET version = excluded.version;
END;

CREATE TRIGGER version_character_delete AFTER DELETE ON characters
WHEN OLD.deleted = 0
BEGIN
    INSERT INTO collection_etags(collection, owner_id) VALUES ('characters', OLD.owner_id)
    ON CONFLICT (collection, owner_id) DO UPDATE SET version = excluded.version;
END;

CREATE TRIGGER version_place_insert AFTER INSERT ON places
BEGIN
    INSERT INTO collection_etags(collection, owner_id) VALUES ('places', COALESCE(NEW.owner_id, ''))
    ON CONFLICT (collection, owner_id) DO UPDATE SET version = excluded.version;
END;

CREATE TRIGGER version_place_update AFTER UPDATE ON places
WHEN OLD.id IS NOT NEW.id OR OLD.owner_id IS NOT NEW.owner_id OR OLD.name IS NOT NEW.name OR
    OLD.description IS NOT NEW.description OR OLD.inhabitants IS NOT NEW.inhabitants OR
    OLD.origin IS NOT NEW.origin OR OLD.thumbnail IS NOT NEW.thumbnail
BEGIN
    INSERT INTO collection_etags(collection, owner_id) VALUES ('places', COALESCE(OLD.owner_id, ''))
    ON CONFLICT (collection, owner_id) DO UPDATE SET version = excluded.version;
    INSERT INTO collection_etags(collection, owner_id)
    SELECT 'places', COALESCE(NEW.owner_id, '') WHERE OLD.owner_id IS NOT NEW.owner_id
    ON CONFLICT (collection, owner_id) DO UPDATE SET version = excluded.version;
END;

CREATE TRIGGER version_place_delete AFTER DELETE ON places
BEGIN
    INSERT INTO collection_etags(collection, owner_id) VALUES ('places', COALESCE(OLD.owner_id, ''))
    ON CONFLICT (collection, owner_id) DO UPDATE SET version = excluded.version;
END;
