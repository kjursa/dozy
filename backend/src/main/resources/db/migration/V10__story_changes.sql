CREATE TABLE story_changes (
    revision INTEGER PRIMARY KEY AUTOINCREMENT,
    owner_id TEXT NOT NULL REFERENCES installations(id),
    story_id TEXT NOT NULL,
    operation TEXT NOT NULL CHECK(operation IN ('UPSERT', 'DELETE'))
);

CREATE INDEX story_changes_by_owner_and_revision ON story_changes(owner_id, revision);

INSERT INTO story_changes(owner_id, story_id, operation)
SELECT owner_id, id, 'UPSERT' FROM stories ORDER BY id;

CREATE TRIGGER record_story_insert AFTER INSERT ON stories
BEGIN
    INSERT INTO story_changes(owner_id, story_id, operation) VALUES (NEW.owner_id, NEW.id, 'UPSERT');
END;

CREATE TRIGGER record_story_update AFTER UPDATE OF title, text, cover_url, created_at ON stories
WHEN NEW.title != OLD.title OR NEW.text != OLD.text OR NEW.cover_url != OLD.cover_url OR NEW.created_at != OLD.created_at
BEGIN
    INSERT INTO story_changes(owner_id, story_id, operation) VALUES (NEW.owner_id, NEW.id, 'UPSERT');
END;

CREATE TRIGGER record_story_delete AFTER DELETE ON stories
BEGIN
    INSERT INTO story_changes(owner_id, story_id, operation) VALUES (OLD.owner_id, OLD.id, 'DELETE');
END;
