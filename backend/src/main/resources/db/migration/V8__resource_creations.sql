CREATE TABLE resource_creations (
    installation_id TEXT NOT NULL REFERENCES installations(id),
    resource_type TEXT NOT NULL,
    request_key TEXT NOT NULL,
    request_hash TEXT NOT NULL,
    resource_id TEXT NOT NULL,
    PRIMARY KEY (installation_id, resource_type, request_key)
);
