# Dozy database

## Current SQLite implementation

The initial backend stores installations and places in a local SQLite file.
`application.yaml` supplies the path and lock timeout through typed `AppConfig` /
`DatabaseConfig`; use `DOZY_DATABASE_PATH` and `DOZY_DATABASE_BUSY_TIMEOUT_MS` to
override them. Defaults are `data/dozy.db` and 5000 ms. The path is relative to the
server working directory. There is no external database service requirement.

`SqliteDatabase` creates parent directories and runs Flyway 13.6.0 before serving
requests. Flyway discovers versioned SQL in `classpath:db/migration`, applies
pending migrations in order, and tracks versions and checksums in
`flyway_schema_history`. Applied script checksum mismatches fail startup.
`V1__places.sql` creates `installations` and `places`, including an owner index and the three
read-only `builtin:` places. These SQLite IDs are TEXT, and timestamps use
`CURRENT_TIMESTAMP`; the PostgreSQL DDL below is not run by the application.

Data access uses Exposed 1.6.1 SQL DSL over the SQLite JDBC driver, without ORM
entities. Feature-owned `Table` definitions and queries map `ResultRow` directly
to domain models. Flyway and Exposed share the configured SQLite data source.
Application code no longer parses SQL scripts or tracks `PRAGMA user_version`.

Add future changes as `V2__description.sql`, `V3__description.sql`, and so on;
do not edit an applied migration. There is no automatic baseline for unmanaged
non-empty databases. For a database created by the pre-Flyway version of this PR,
verify it has the V1 schema, then explicitly baseline it at version 1 before
starting this backend. For example, with the Flyway CLI:

```sh
flyway -url=jdbc:sqlite:/absolute/path/dozy.db -baselineVersion=1 baseline
```

This preserves existing data and lets Flyway apply subsequent migrations.
SQLite migration startup should run in one process at a time; Flyway does not
provide concurrent migration locking for SQLite.

Each operation uses a connection with foreign keys enabled and the configured
busy timeout. Exposed manages transactions, statement execution, and connection
cleanup on `Dispatchers.IO`. The SQLite driver uses `IMMEDIATE` transaction mode
to acquire the write reservation before reading ownership. These transactions make owner checks and upserts
atomic, including concurrent claims of an ID. Reads register the installation
and collect its custom places plus shared places in one transaction. This simple
single-file implementation is intended for the initial local backend, not as a
production capacity claim. Keep the file on persistent writable storage.

Ktor DI unregisters the Exposed database when the application stops. SQL statement
logging is disabled to keep user-provided place content out of logs; request
status logging remains enabled.

API code depends on `PlaceService` and the domain `PlaceRepository` contract.
SQLite queries and repository implementation remain in the data layer, so a
future PostgreSQL implementation can replace them through DI.

## Proposed PostgreSQL database

## Scope

This schema supports [API.md](API.md), derived from the current mobile contracts.
It is a design proposal, not an installed migration or the Android Room schema.
Use one PostgreSQL database with feature-owned tables. Store image bytes in object
storage; PostgreSQL holds references, input snapshots, generated text, and job state.

Confirmed product decisions:

- No user accounts or login for now. Resources belong to an installation identified
  by a client-generated UUID in `X-Installation-Id`, without a separate secret.
- Keep the existing generated-avatar contract, including explicit generation before
  character save. The local fake generator is a development convenience only.
- Reinstallation without preserving the installation ID creates a separate namespace;
  there is no cross-device library/account recovery contract.

The UUID is a namespace identifier, not strong authentication. A caller presenting
another installation's ID could access its resources. Use TLS, keep IDs out of
public URLs/logs, and do not describe this as authenticated private storage.
Mobile must persist/send this new header; it does not do so today. The backend can
register an installation on its first valid request, without inventing a separate
registration endpoint. Future account linking is separate scope.

## Table responsibilities

| Table | Responsibility / API |
| --- | --- |
| `installations` | Data ownership and per-installation generation coordination |
| `assets` | Durable avatar/cover IDs, storage keys and HTTPS URLs |
| `characters` | Character PUT/GET and permanent deletion tombstones |
| `places` | Shared predefined catalog and installation-owned custom settings |
| `avatar_jobs` | Independent avatar POST/poll jobs and recoverable worker state |
| `story_jobs` | Logical job, immutable configuration, current attempt, active-job limit |
| `story_attempts` | Each accepted initial/retry execution, stages and intermediate text |
| `story_request_keys` | Durable initial/retry idempotency bindings |
| `stories` | One immutable successful result per logical job |
| `library_snapshots` | Stable story-list traversal across multiple HTTP requests |
| `library_snapshot_items` | Frozen ordered story membership for a traversal |

The last two tables are a concrete proposal for stable pagination, not a mobile
requirement to use a particular storage technology. They may later be replaced by
an equivalent durable snapshot/cursor mechanism. Home and reader settings need no
additional tables. Do not copy Room's `pendingSync`, notification flags, reserved
`library:`/`story:` row keys, or serialized UI state to the backend.

## Proposed DDL

IDs generated by the backend are supplied by application code; no UUID extension
is required. Story/job ID checks match the mobile URL-safe format. Character/place
IDs remain `text` because they are opaque and predefined places contain colons.
Use `timestamptz` internally; expose story creation times as integer UTC epoch
milliseconds, truncated consistently for both list sorting and response serialization.

```sql
CREATE TABLE installations (
    id uuid PRIMARY KEY,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE assets (
    id text PRIMARY KEY CHECK (btrim(id) <> ''),
    installation_id uuid NOT NULL REFERENCES installations(id),
    kind text NOT NULL CHECK (kind IN ('AVATAR', 'STORY_COVER')),
    object_key text NOT NULL UNIQUE CHECK (btrim(object_key) <> ''),
    public_url text NOT NULL CHECK (public_url LIKE 'https://%'),
    created_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (id, installation_id)
);
CREATE INDEX assets_installation_idx ON assets (installation_id);

CREATE TABLE characters (
    installation_id uuid NOT NULL REFERENCES installations(id),
    id text NOT NULL CHECK (btrim(id) <> ''),
    name text NOT NULL CHECK (btrim(name) <> ''),
    personality text NOT NULL,
    appearance text NOT NULL,
    type text NOT NULL CHECK (type IN ('CHILD', 'ANIMAL', 'FANTASY', 'OTHER')),
    avatar_asset_id text,
    deleted_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (installation_id, id),
    FOREIGN KEY (avatar_asset_id, installation_id)
        REFERENCES assets (id, installation_id)
);
CREATE INDEX characters_visible_idx ON characters (installation_id, id)
    WHERE deleted_at IS NULL;
CREATE INDEX characters_avatar_idx ON characters (avatar_asset_id);

CREATE TABLE places (
    id text PRIMARY KEY CHECK (btrim(id) <> ''),
    installation_id uuid REFERENCES installations(id),
    ownership text NOT NULL CHECK (ownership IN ('CUSTOM', 'PREDEFINED')),
    name text NOT NULL CHECK (btrim(name) <> ''),
    description text NOT NULL CHECK (btrim(description) <> ''),
    inhabitants text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CHECK (
        (ownership = 'PREDEFINED' AND installation_id IS NULL
            AND starts_with(id, 'builtin:'))
        OR
        (ownership = 'CUSTOM' AND installation_id IS NOT NULL
            AND NOT starts_with(id, 'builtin:'))
    ),
    CHECK (inhabitants IS NULL OR btrim(inhabitants) <> '')
);
CREATE INDEX places_installation_idx ON places (installation_id);

CREATE TABLE avatar_jobs (
    id text PRIMARY KEY CHECK (id ~ '^[A-Za-z0-9_-]{1,128}$'),
    installation_id uuid NOT NULL REFERENCES installations(id),
    request_snapshot jsonb NOT NULL CHECK (jsonb_typeof(request_snapshot) = 'object'),
    status text NOT NULL CHECK (status IN ('QUEUED', 'RUNNING', 'READY', 'FAILED')),
    asset_id text,
    error_code text,
    available_at timestamptz NOT NULL DEFAULT now(),
    lease_token uuid,
    lease_until timestamptz,
    worker_attempts integer NOT NULL DEFAULT 0 CHECK (worker_attempts >= 0),
    provider_request_id text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    FOREIGN KEY (asset_id, installation_id) REFERENCES assets (id, installation_id),
    CHECK ((status = 'READY') = (asset_id IS NOT NULL)),
    CHECK (status = 'FAILED' OR error_code IS NULL)
);
CREATE INDEX avatar_jobs_owner_idx ON avatar_jobs (installation_id, id);
CREATE INDEX avatar_jobs_work_idx ON avatar_jobs (available_at, lease_until)
    WHERE status IN ('QUEUED', 'RUNNING');

CREATE TABLE story_jobs (
    id text PRIMARY KEY CHECK (id ~ '^[A-Za-z0-9_-]{1,128}$'),
    installation_id uuid NOT NULL REFERENCES installations(id),
    configuration jsonb NOT NULL CHECK (jsonb_typeof(configuration) = 'object'),
    status text NOT NULL CHECK (status IN
        ('QUEUED', 'CREATING_TEXT', 'CREATING_COVER', 'READY', 'FAILED')),
    current_attempt integer NOT NULL DEFAULT 1 CHECK (current_attempt > 0),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (id, installation_id)
);
CREATE UNIQUE INDEX one_active_story_per_installation
    ON story_jobs (installation_id)
    WHERE status IN ('QUEUED', 'CREATING_TEXT', 'CREATING_COVER');

CREATE TABLE story_attempts (
    job_id text NOT NULL,
    installation_id uuid NOT NULL,
    attempt_no integer NOT NULL CHECK (attempt_no > 0),
    status text NOT NULL CHECK (status IN
        ('QUEUED', 'CREATING_TEXT', 'CREATING_COVER', 'READY', 'FAILED')),
    error_code text CHECK (error_code IN ('CONTENT_REJECTED', 'GENERATION_FAILED')),
    generated_title text,
    generated_text text,
    available_at timestamptz NOT NULL DEFAULT now(),
    lease_token uuid,
    lease_until timestamptz,
    worker_attempts integer NOT NULL DEFAULT 0 CHECK (worker_attempts >= 0),
    text_provider_request_id text,
    cover_provider_request_id text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (job_id, attempt_no),
    UNIQUE (job_id, attempt_no, installation_id),
    FOREIGN KEY (job_id, installation_id) REFERENCES story_jobs (id, installation_id),
    CHECK ((status = 'FAILED') = (error_code IS NOT NULL))
);
ALTER TABLE story_jobs ADD CONSTRAINT story_jobs_current_attempt_fk
    FOREIGN KEY (id, current_attempt) REFERENCES story_attempts (job_id, attempt_no)
    DEFERRABLE INITIALLY DEFERRED;
CREATE INDEX story_attempts_work_idx ON story_attempts (available_at, lease_until)
    WHERE status IN ('QUEUED', 'CREATING_TEXT', 'CREATING_COVER');

CREATE TABLE story_request_keys (
    installation_id uuid NOT NULL REFERENCES installations(id),
    idempotency_key uuid NOT NULL,
    operation text NOT NULL CHECK (operation IN ('START', 'RETRY')),
    job_id text NOT NULL,
    attempt_no integer NOT NULL,
    request_fingerprint text NOT NULL CHECK (btrim(request_fingerprint) <> ''),
    created_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (installation_id, idempotency_key),
    FOREIGN KEY (job_id, attempt_no, installation_id)
        REFERENCES story_attempts (job_id, attempt_no, installation_id)
);
CREATE UNIQUE INDEX one_start_key_per_story ON story_request_keys (job_id)
    WHERE operation = 'START';
CREATE INDEX story_request_keys_attempt_idx ON story_request_keys (job_id, attempt_no);

CREATE TABLE stories (
    id text PRIMARY KEY CHECK (id ~ '^[A-Za-z0-9_-]{1,128}$'),
    installation_id uuid NOT NULL REFERENCES installations(id),
    job_id text NOT NULL UNIQUE,
    title text NOT NULL CHECK (btrim(title) <> ''),
    text text NOT NULL CHECK (btrim(text) <> ''),
    cover_asset_id text NOT NULL,
    reading_minutes integer NOT NULL CHECK (reading_minutes > 0),
    created_at timestamptz NOT NULL,
    UNIQUE (id, installation_id),
    FOREIGN KEY (job_id, installation_id) REFERENCES story_jobs (id, installation_id),
    FOREIGN KEY (cover_asset_id, installation_id) REFERENCES assets (id, installation_id),
    CHECK (created_at >= timestamptz '1970-01-01 00:00:00+00'),
    CHECK (created_at = date_trunc('milliseconds', created_at))
);
CREATE INDEX stories_library_idx
    ON stories (installation_id, created_at DESC, id COLLATE "C" ASC);
CREATE INDEX stories_cover_idx ON stories (cover_asset_id);

CREATE TABLE library_snapshots (
    id uuid PRIMARY KEY,
    installation_id uuid NOT NULL REFERENCES installations(id),
    page_size integer NOT NULL CHECK (page_size BETWEEN 1 AND 100),
    created_at timestamptz NOT NULL DEFAULT now(),
    expires_at timestamptz,
    UNIQUE (id, installation_id),
    CHECK (expires_at IS NULL OR expires_at > created_at)
);
CREATE INDEX library_snapshots_expiry_idx ON library_snapshots (expires_at);

CREATE TABLE library_snapshot_items (
    snapshot_id uuid NOT NULL,
    installation_id uuid NOT NULL,
    position bigint NOT NULL CHECK (position > 0),
    story_id text NOT NULL,
    PRIMARY KEY (snapshot_id, position),
    UNIQUE (snapshot_id, story_id),
    FOREIGN KEY (snapshot_id, installation_id)
        REFERENCES library_snapshots (id, installation_id) ON DELETE CASCADE,
    FOREIGN KEY (story_id, installation_id) REFERENCES stories (id, installation_id)
);
CREATE INDEX library_snapshot_items_story_idx ON library_snapshot_items (story_id);
```

## Mapping and validation

- `characters.avatar_asset_id` joins to `assets` to produce `{id,url}`. A soft-deleted
  character disappears from GET but its primary key remains reserved. Never use
  unconditional UPSERT to revive a tombstone. Character IDs are installation-scoped;
  no other installation's rows are affected by the same submitted ID.
- `places` has no stored `editable` flag: derive it as true only for the current
  installation's custom place. Shared predefined places have no owner. Seed
  `builtin:woods`, `builtin:island`, `builtin:village` without changing their IDs.
  A custom place ID belonging to another installation cannot be overwritten.
- `avatar_jobs.request_snapshot` holds type/personality/appearance. It is not a
  character record. `asset_id` is assigned only after successful image persistence.
- `story_jobs.configuration` holds the exact accepted GenerationRequest DTO,
  including ordered cast and optional place snapshots. There are deliberately no
  foreign keys to live characters or places: unsynchronized snapshots are valid,
  and later deletion/editing cannot affect a story or retry.
- Full story responses join `stories` to their job configuration and cover asset.
  Job configuration and completed stories are immutable by application policy;
  the DDL alone does not prevent arbitrary UPDATEs. Restrict write paths accordingly.
- JSONB preserves values and array order, not the original HTTP whitespace/object
  key order. Store a canonical fingerprint for idempotency, not the raw JSON bytes.
  Include operation and target job in retry fingerprints; do not treat a START key
  as a RETRY key even if the bodies look similar.
- Validate JSON shapes, cast uniqueness, mode-dependent omissions, text limits,
  enums and asset ownership in application code. SQL checks are defensive structural
  constraints, not a replacement for domain validation. Current UTF-16 length rules
  must not become `varchar(n)`/`char_length` restrictions on historical records.
- Require `AVATAR` assets for character references and `STORY_COVER` for results;
  the owner-composite foreign keys enforce owner equality but not asset kind.
  Restrict asset URLs to the server's durable storage namespace at the API boundary;
  the SQL HTTPS prefix check alone does not validate URLs or protect against SSRF.
- Fake `bundled-avatar://` references never become assets. Also define how mobile
  story snapshots referencing fake previews are migrated before enabling the API.

## Transactions and worker recovery

### Character/place mutation

Authorize by installation and lock the target row when necessary. A character
DELETE sets `deleted_at`; later PUT returns a conflict. Repeated DELETE returns
204 or 404. Never cascade into stories or asset deletion. PUT must acknowledge only
a committed write; `updated_at` is server metadata, not a mobile version token.
Character/place edits currently use last accepted write wins. Future versioned
conflict reconciliation requires an API change.

### Initial story acceptance

In one transaction:

1. Ensure the installation row exists, then lock it (`SELECT ... FOR UPDATE`) to
   serialize initial/retry acceptance for that installation.
2. Look up `(installation_id, idempotency_key)` first. For a matching START
   fingerprint return its bound job/attempt status; otherwise return conflict.
3. Reject another active logical job. Insert job, attempt 1, and START binding.
   The deferred current-attempt foreign key is satisfied before commit.
4. Commit before returning 202 or calling a provider. The partial unique index is
   a second concurrency guard; map a competing active-job violation to the API error.

Job rows are the durable queue. A process restart must not lose accepted work.
Do not perform generation inside the HTTP request transaction.

### Claiming work and publishing a result

Claim eligible rows in short transactions with `FOR UPDATE SKIP LOCKED`. Set a
lease token/expiry and release the transaction before remote I/O. Heartbeat leases
for long operations; after expiry another worker can recover the job. Persist
intermediate title/text before generating a cover so cover failures need not
regenerate successful text. Use provider idempotency/request IDs where supported;
a timeout can still cause duplicate external work or charges even if SQL publishes
only one logical result.

Every update/publish must compare the current lease token and attempt number so an
expired worker cannot overwrite a newer execution. Update current attempt and job
stages together in short transactions; normal GET reads the job/current attempt.
A RETRY key replay reads its explicitly bound historical attempt instead.

Write the image durably, then insert its asset reference. In the final transaction,
verify the current lease, insert the unique story result, and update the attempt/job
to READY. A unique `stories.job_id` prevents multiple published stories. READY must
never be committed without a complete readable result; this is a cross-table
application invariant, not a CHECK constraint. A failed final database write can
leave an orphan storage object; reconcile it without deleting referenced assets.

### Explicit failed-job retry

Use the same installation lock and check idempotency bindings before state checks.
For a fresh RETRY key, require FAILED, no other active job, and no completed result.
Increment `current_attempt`, insert the new attempt and key binding, and change the
logical job to QUEUED atomically. Keep prior attempt rows and all key bindings for
the job/story lifetime. The retry uses the unchanged configuration. When safe,
validated intermediate text may be copied from a prior failed cover attempt;
provider work must never alter the original configuration.

### Avatar jobs

Avatar jobs use the same lease/recovery pattern, independently of story jobs. A new
POST always creates a new logical avatar job; there is no public avatar retry/key
binding in the existing contract. Generation never inserts or updates `characters`.
READY assets can be attached only through explicit character PUT. Define quotas
and garbage-collection rules separately; a client's abandoned polling is not proof
that an asset is disposable.

## Stable library pagination

On the first page, create a library snapshot and populate its ordered story IDs in
one consistent transaction using `row_number() OVER (ORDER BY created_at DESC,
id COLLATE "C" ASC)`. All results are already immutable. Return the first page and
an opaque cursor containing/referencing snapshot ID and the last returned position.
For subsequent pages, verify installation ownership and page size, then select
positions greater than that position in ascending order. Cursor contents must be
validated; do not trust a client-supplied owner or arbitrary SQL/order expression.

New stories do not enter an existing traversal. A forged, missing or expired cursor
returns INVALID_CURSOR, never silently starts a different snapshot. Snapshot expiry
is an operational policy, not a decided product lifetime; configure it to allow a
full library traversal and document it before production. Expiring a snapshot may
delete its item rows but must not delete stories. This avoids relying on timestamp
cutoffs alone, which can miss concurrent insert/commit ordering.

## Retention, security, and future scope

- Preserve deleted character ID reservations, saved story assets, configurations,
  attempts and idempotency bindings as required by the current contract. No public
  story/place deletion endpoint or account deletion policy is specified here.
- Store provider credentials in configuration, never these tables. Provider traces
  must not leak raw child/personality/story content into ordinary logs.
- Asset URLs currently work without API auth; storage policy must account for that.
  A namespace UUID alone does not provide authenticated access control.
- No billing ledger, credits, free quotas, subscriptions, FCM/APNs tokens, or reading
  preferences are inferred from these endpoints. Add them only with an agreed contract.
- Future accounts can own/link installation namespaces, but merge/recovery behavior
  needs its own design. Do not add email/password columns preemptively.

The schema uses composite foreign keys and partial unique indexes supported by
[PostgreSQL constraints](https://www.postgresql.org/docs/current/ddl-constraints.html).
Cross-row invariants described above require transactions/application checks;
PostgreSQL CHECK constraints cannot enforce arbitrary cross-table conditions.
