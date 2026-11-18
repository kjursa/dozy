CREATE TABLE auth_users (
    owner_id TEXT PRIMARY KEY,
    email TEXT NOT NULL UNIQUE
);

CREATE TABLE auth_email_codes (
    session_id TEXT PRIMARY KEY,
    email TEXT NOT NULL,
    code TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    expires_at INTEGER NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    consumed INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX auth_email_codes_email_created ON auth_email_codes(email, created_at);

CREATE TABLE auth_refresh_tokens (
    token_hash TEXT PRIMARY KEY,
    owner_id TEXT NOT NULL REFERENCES auth_users(owner_id),
    expires_at INTEGER NOT NULL,
    revoked INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX auth_refresh_tokens_owner ON auth_refresh_tokens(owner_id);
