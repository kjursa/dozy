# Dozy HTTP API

Open the [interactive endpoint reference](api/index.html) in a browser for request
and response examples. It works locally without a build step or API calls.

## Status and sources

This document combines implemented routes and proposals consolidated from the
mobile contracts. The backend implements `GET /hello`, `GET /api/v1/places`, and
`PUT /api/v1/places/{id}`, and character GET/PUT/DELETE under `/api/v1/characters`.
Story start/status/retry/read, protected PNG asset reads and fake avatar generation are implemented. Stable library listing is implemented. `/hello` returns
`200 {"message":"Hello world from Dozy"}`. Places and Characters use a local SQLite database;
see [runtime storage](DB.md#current-sqlite-implementation) and the root README for configuration.

Sources:

- [Character contract](../mobile/CHARACTER_DATA.md), including F2–F5 amendments.
- [Place contract](../mobile/PLACE_DATA.md).
- [Story contract](../mobile/STORY_GENERATION_API.md), including F9–F13 amendments.
- [Mobile specification](../mobile/SPEC.MD).
- Shared feature contracts and clients under `api/contracts` and `api/client`.
- Mobile feature repositories under
  `mobile/shared/src/commonMain/kotlin/com/dozy/mobile/feature/`.
- [Proposed PostgreSQL schema](DB.md).

Later feature amendments take precedence over historical implementation notes.
For example, characters now support deletion, and full story responses include
`createdAt`. Local Room schema versions and sync flags are not backend API fields.

## Shared conventions

- Feature paths use `/api/v1`, relative to the configured server root URL.
  `/hello` remains unversioned. Requests/responses use JSON unless stated otherwise.
- Personal resources belong to an installation, not a user account. As confirmed
  for this stage, send `X-Installation-Id: <client-generated UUID>` on every feature
  request, with no separate secret or login endpoint. Persist the UUID locally;
  the server registers it lazily on first use. The shared Places and Characters clients send this
  header automatically; the story clients use it as well. Missing/invalid headers
  return `400 INVALID_INSTALLATION_ID`. The ID partitions data but is not strong
  authentication: a caller knowing another ID could access its namespace. No
  library recovery is defined after losing the ID. Never take ownership from JSON.
- Character IDs are canonical UUIDv7 strings (case normalized at the boundary).
  Custom place IDs are opaque strings; predefined places use `builtin:` IDs. Encode path IDs as
  one path segment. New story/job/asset IDs are backend-generated UUIDv7 strings. Avatar job IDs use
  the same alphabet; a 128-character cap is a proposed server convention.
- Enum wire values are uppercase. Optional nullable fields can be omitted or null
  unless a specific operation requires omission. Responses must preserve required
  fields even if the server serializer normally omits defaults.
- Error responses use the same machine-readable fields: `code`, `field`, and `reason`.
  Unused detail fields are `null`. `code` identifies the API error
  category independently of HTTP status; it is not display-ready text. Mobile should
  localize known codes/reasons and use a localized generic fallback for unknown ones.
  Other proposed feature payloads do not yet define field-level error details.
- `401` is reserved for future authentication (and appears in mobile proposals);
  `403` means forbidden, `429` throttled, and `503` temporarily
  unavailable. For owner-scoped ID reads, use `404` for missing or inaccessible
  resources to avoid disclosing another installation's records. Collection reads include
  only visible records. All feature queries/mutations must be scoped to the supplied installation ID.
- A successful mutation acknowledgement means a durable write. HTTP timeout does
  not mean cancellation or rollback; clients may repeat operations as defined below.
- Validate new/edited input after trimming. Match Kotlin `String.length` (UTF-16
  code units), rather than counting PostgreSQL characters or bytes. Existing
  records and embedded historical snapshots must remain readable after rule changes.
- HTTPS asset references must remain usable for the lifetime of saved content.
  Mobile cover loading sends the installation header only for assets on its configured backend origin. Do not return
  short-lived signed URLs without extending the mobile contract first.

## Endpoint inventory

| Method | Path | Success | Purpose |
| --- | --- | --- | --- |
| GET | `/api/v1/characters` | 200 | Complete personal character collection |
| PUT | `/api/v1/characters/{id}` | 201 / 204 | Create or replace a character |
| DELETE | `/api/v1/characters/{id}` | 204 / 404 | Idempotent character deletion |
| POST | `/api/v1/character-avatars` | 202 | Start a standalone avatar job |
| GET | `/api/v1/character-avatars/{jobId}` | 200 | Observe an avatar job |
| GET | `/api/v1/places` | 200 | Personal and predefined places |
| PUT | `/api/v1/places/{id}` | 201 / 204 | Create or replace a custom place |
| POST | `/api/v1/story-generations` | 202 | Accept a durable story-generation job |
| GET | `/api/v1/story-generations/{jobId}` | 200 | Observe generation stages |
| POST | `/api/v1/story-generations/{jobId}/retry` | 202 | Explicitly retry a failed job |
| GET | `/api/v1/stories/{storyId}` | 200 | Read an immutable complete story |
| GET | `/api/v1/stories?limit=100&cursor=...` | 200 | Page through a persistent, installation-owned snapshot of story summaries |
| GET | `/api/v1/assets/{assetId}` | 200 | Read an owned durable PNG cover |

There are **13 method/path operations** above. `GET /api/v1/characters/{id}` is an optional
future extension described in mobile documents, not a current client requirement.
Home, character details, place editing, and reader settings introduce no additional
endpoints. There is no place/story deletion, story editing, device-token registration,
payment, balance, subscription, or authentication endpoint proposed by these clients.

## Characters

### Character representation

```json
{
  "id": "hero-1",
  "name": "Luna",
  "personality": "Kind and curious",
  "appearance": "Red hair and a green coat",
  "type": "CHILD",
  "avatar": {"id":"asset-1","url":"https://assets.example.test/asset-1.png"}
}
```

`type`: `CHILD`, `ANIMAL`, `FANTASY`, or `OTHER`. `avatar` may be null/omitted for
historical characters. New mobile forms require an avatar before saving. An avatar
contains a nonblank opaque ID and a stable HTTPS URL; validate ownership and that
ID/URL refer to the same ready asset, not merely the URL syntax.

New/edited names: trimmed, nonempty, single line, at most 50 UTF-16 units.
Personality and appearance: trimmed, required, at most 2000 units each. These are
current mobile input rules; adopt them for backend writes without truncating legacy
reads or forcing historical story snapshots through current creation validation.

### GET /api/v1/characters

No body or pagination. Return `200` and a **bare JSON array** of Character objects,
not `{ "characters": [...] }`. Include all non-deleted characters visible to the
owner in a consistent snapshot; IDs must be unique. `[]` is authoritative.

Non-200 responses are refresh failures; mobile retains known data. Pending local
edits and local deletion tombstones take precedence over a fetched snapshot.
There is no separate required character-details read endpoint.

### PUT /api/v1/characters/{id}

Implemented. Body: `SaveCharacterRequest` with `name`, `personality`, `appearance`,
`type`, and optional nullable `avatar`. The UUIDv7 ID appears only in the path;
an ID in the body is a structural error. A minimal request is:

```json
{"name":"Luna","personality":"Kind","appearance":"Green coat","type":"CHILD"}
```

PUT fully replaces editable fields; omitted/null avatar clears it. Repeated PUTs
reuse the same resource. Return 201 creation or 204 replacement, both empty.
Names are trimmed, required, single-line and at most 50 UTF-16 units. Personality
and appearance are trimmed, required and at most 2000 each. Reads preserve
historical values. Unknown types are field validation failures.

Errors: 400 `INVALID_REQUEST` for structure/invalid resource ID; 422
flat validation codes such as `validation.name.too_long` from the shared
`CharactersSaveError` contract for business
input; 422 `REFERENCE_UNAVAILABLE` for avatar references that do not match an owned
READY asset's ID and URL; 409 `CONFLICT` for another installation's or permanently
deleted ID. The name reasons are REQUIRED, TOO_LONG and SINGLE_LINE; personality
and appearance support REQUIRED / TOO_LONG; type supports INVALID_VALUE.

### DELETE /api/v1/characters/{id}

Implemented. No body. Return empty 204 for an owned live or already-deleted ID;
return 404 `CHARACTER_NOT_FOUND` for a missing/inaccessible ID. The shared client
acknowledges that known code, not arbitrary 404s. A permanent tombstone prevents
any subsequent PUT from claiming/reviving the ID, including by its former owner.
Deletion does not alter saved story snapshots. Missing/invalid installation IDs
return 400 `INVALID_INSTALLATION_ID` on every operation.

## Character avatars

Implemented with an injected deterministic PNG fake; no external model is called.

### POST /api/v1/character-avatars

```json
{"type":"CHILD","personality":"Curious","appearance":"Green coat"}
```

Name is not submitted. Validate type and current description rules. Return `202`:

```json
{"jobId":"avatar-job-1","status":"QUEUED","asset":null,"error":null}
```

Persist the job and request before acknowledging. Generation is asynchronous and
creates an asset only, never a character. There is **no Idempotency-Key in the
current mobile avatar request**: every explicit generation creates an independent
job, including retries after an uncertain response. Do not claim exactly-once
provider execution or charges. Retrying/polling limits do not cancel server work.

### GET /api/v1/character-avatars/{jobId}

Return `200` with the same envelope. Status: `QUEUED`, `RUNNING`, `READY`, `FAILED`.

```json
{"jobId":"avatar-job-1","status":"READY","asset":{"id":"asset-1","url":"https://assets.example.test/asset-1.png"},"error":null}
```

READY requires a complete durable asset; FAILED carries the contract code in **`error`**
(for example `GENERATION_FAILED`), not the story endpoint's `errorCode`. Use null or
omit non-applicable optional fields. Unknown/mismatched jobs, invalid responses,
non-success HTTP, or observation timeout are mobile failures, not implicit deletion.
The current client polls every second for up to 120 GETs. A later PUT explicitly
attaches the ready asset to a character. Jobs and unattached assets are retained without automatic cleanup during development.

Local fake avatars (`bundled-avatar://...`) exist only in disabled-API development
mode. They are not server assets. Users must regenerate them through backend jobs before uploading references.

## Places

### Place representation

```json
{
  "id":"builtin:woods",
  "name":"Whispering woods",
  "description":"A peaceful forest with glowing paths.",
  "inhabitants":null,
  "origin":"SYSTEM",
  "editable":false
}
```

`origin`: `USER` or `SYSTEM`. `editable` is computed from permissions;
clients cannot grant it by submitting true. Predefined places must not be editable.
Custom places use non-`builtin:` IDs. No image field is required. Normalize blank
inhabitants to null. Current form rules: name required/max 80, description
required/max 500, inhabitants optional/max 500, using trimmed UTF-16 lengths.

### GET /api/v1/places

Implemented. Requires `X-Installation-Id`. The backend returns custom places first,
then sorts each group by lowercase name and ID. A fresh database includes all
three predefined places, so it does not return an empty collection.

No body/query. Return `200 {"places":[<Place>,...]}` containing a complete consistent
collection of accessible custom and predefined places. Three bundled mobile IDs
are `builtin:woods`, `builtin:island`, `builtin:village`; preserve their identity.
The client sorts custom first, then lowercased name and ID. It merges the server
snapshot with bundled fallback entries and preserves authoritative local edits.
`{"places":[]}` is a valid empty remote snapshot. Non-200/invalid collections retain
known local data; there is no GET-by-ID requirement.

### PUT /api/v1/places/{id}

Implemented. Requires `X-Installation-Id` and `Content-Type: application/json`.

Body: `name`, `description`, and optional `inhabitants`. The ID comes only from
the path; the backend assigns `origin: "USER"` and computes `editable`.
Omitted/null inhabitants clears the previous value. Only the owner may upsert a
custom record; reject writes to the predefined namespace. Responses:
`201` creation, `204` update, no body; mobile accepts any 2xx.
Repeated identical PUTs replace one resource. A transaction checks ownership and
writes the replacement atomically. `editable: true` does not bypass permissions.

Implemented place business errors are shared serializable `PlacesSaveError` values
from `api/contracts`. Backend sends that object and the SDK deserializes the same
type. Its contract serializer encodes flat codes and preserves future values as
`Unknown(code)`, without a client-side error mapper.
The common envelope's `field`/`reason` properties are null and are not parsed for Places:

```json
{"code":"validation.name.too_long","field":null,"reason":null}
```

Validation codes are `validation.name.required`, `validation.name.too_long`,
`validation.description.required`, `validation.description.too_long`, and
`validation.inhabitants.too_long`. Required means trimmed empty input; too_long
means a trimmed value exceeds its write limit (UTF-16 units). Existing records
remain readable even if write limits change.

| Status | Code | Meaning |
| --- | --- | --- |
| 400 | `INVALID_REQUEST` / `INVALID_INSTALLATION_ID` | Invalid request structure or installation header |
| 422 | Flat validation code above | Invalid user input |
| 403 | `place.not_editable` | Any `builtin:` ID, including unknown ones |
| 409 | `place.conflict` | Custom ID belongs to another installation |
| 503 | `UNAVAILABLE` | SQLite operation failed |

Places SDK operations expose their own Success/ApiError/TechnicalError result.
Shared `TechnicalError.Server` represents every 5xx regardless of body;
`TechnicalError.Communication` holds all other Exceptions, including invalid
requests/responses, transport failures and local argument errors. Future decoded
business codes remain operation-specific `Unknown`. Cancellation propagates. All other clients now use the same result/technical-error policy and shared
business-error contracts.

GET can return the installation-header and database errors above. Rate limiting
and authentication are not implemented. Edits cannot rewrite existing story
configurations when those are introduced. Versioned
multi-device reconciliation is not defined; no version precondition is submitted.

## Story generation

### Request snapshot

```json
{
  "cast":[{"id":"hero-1","name":"Luna","personality":"Kind","appearance":"Red hair","type":"CHILD","avatar":null}],
  "mode":"MANUAL",
  "length":"SHORT",
  "place":{"id":"builtin:woods","name":"Woods","description":"Quiet woods","inhabitants":null,"origin":"SYSTEM","editable":false},
  "idea":"Help a lost star",
  "ending":"Everyone returns home"
}
```

Cast is nonempty, with unique source IDs. These are **complete immutable snapshots**,
not instructions to load current character/place values. Accept valid local/bundled
objects not yet synchronized; do not require a matching live SQL character/place row.
Do not reapply current creation length rules to legacy snapshot fields. Validate
shape, nonblank required fields, enums and accessible asset references. Ownership
labels in submitted place snapshots are descriptive, never authorization grants.

`mode`: `MANUAL` or `AUTOMATIC`. `length`: `SHORT` (about 3–5 minutes) or `LONG`
(about 8–12 minutes). MANUAL may omit place (random setting), idea and ending;
trim optional text and normalize blank to absent. Idea max 1000 and ending max
500 UTF-16 units. AUTOMATIC submits **only cast, mode, length**: place/idea/ending
are omitted, even when retained in the client draft. Reject contradictory automatic
requests rather than silently making hidden fields affect generation.

### POST /api/v1/story-generations

Header `Idempotency-Key: <client UUID>`; body: request snapshot above. Return
`202 {"jobId":"job-1","status":"QUEUED"}` after durable acceptance.

- Atomically bind the installation/key to the accepted request and job. Identical replays
  return 202 with the same job and current execution status, even after completion.
- A different body for the same key returns `409 IDEMPOTENCY_CONFLICT`. Define a
  deterministic canonical representation preserving array order and omission;
  object key ordering must not affect equality. Store the request and fingerprint.
- Retain bindings for the lifetime of the job/story. Check replay bindings **before**
  rejecting a new active job, so replaying an active job succeeds.
- Allow at most one active story job per installation (`QUEUED`, `CREATING_TEXT`,
  `CREATING_COVER`); a new conflicting job returns `409 ACTIVE_JOB_EXISTS`.
- Other documented errors: `400 INVALID_REQUEST`, `401 UNAUTHORIZED`,
  `404 REFERENCE_UNAVAILABLE`, `429 RATE_LIMITED`, `503 GENERATION_UNAVAILABLE`.
  REFERENCE_UNAVAILABLE applies to unavailable protected references (such as an
  avatar asset), not simply a missing live row for a valid submitted snapshot.

### GET /api/v1/story-generations/{jobId}

No body. Return 200 with a stage, not a percentage:

```json
{"jobId":"job-1","status":"CREATING_TEXT"}
```

Allowed stages: `QUEUED`, `CREATING_TEXT`, `CREATING_COVER`, `READY`, `FAILED`.
READY requires `storyId`; FAILED requires `errorCode` of `CONTENT_REJECTED` or
`GENERATION_FAILED`. The two fields are absent/null outside their corresponding
terminal state. `404 JOB_NOT_FOUND`, authentication errors, unknown codes, malformed
responses, and network/5xx failures mean observation unavailable, **not FAILED**.

Persist stages and serve monotonic reads within one execution. READY is published
only after text, exactly one cover, and the immutable result are durably readable.
A retry starts a new execution and may reset FAILED to QUEUED. Closing mobile does
not cancel generation. Internal worker retries must not publish a second story.

### POST /api/v1/story-generations/{jobId}/retry

No body. Header `Idempotency-Key: <new client UUIDv4>`. Return 202 `{jobId,status}`
with the **same logical job ID**. Reuse the original snapshot, never edited live data.

Only a FAILED job may start a new execution. Bind each retry key atomically to one
specific execution. Replay checks precede lifecycle checks: replay the recorded
execution's current status even if it is already FAILED/READY or a later execution
exists; never start another attempt using that key. A new explicit retry after a
confirmed later failure gets a new key. Return `409 JOB_NOT_FAILED` for a fresh key
on active/completed jobs, `409 ACTIVE_JOB_EXISTS` if another job is active, or
`409 IDEMPOTENCY_CONFLICT` if a key is bound to a different operation/job.
Missing/inaccessible jobs return `404 JOB_NOT_FOUND`. Original CREATE keys also conflict.
A logical job can have only one successful story.

## Stories

### GET /api/v1/stories/{storyId}

Return 200:

```json
{
  "id":"story-1",
  "title":"A lost star",
  "text":"Once upon a time…",
  "coverUrl":"https://assets.example.test/story-1.png",
  "configuration":{"cast":[{"id":"hero-1","name":"Luna","personality":"Kind","appearance":"Red hair","type":"CHILD","avatar":null}],"mode":"AUTOMATIC","length":"SHORT"},
  "createdAt":1794355200000
}
```

Configuration is the exact accepted request snapshot, not current character/place
records or generated plot metadata. Title/text are nonblank. `createdAt` is an
immutable nonnegative UTC epoch-millisecond timestamp; new backend results always
supply it (mobile only tolerates zero as a legacy fallback). Content and ID never
change on repeated reads. `404 STORY_NOT_FOUND` means missing; `503 UNAVAILABLE`
and other failures mean unavailable. GET never initiates generation.

### GET /api/v1/stories?limit=100&cursor=<opaque cursor>

No body. First page omits cursor. Return 200:

```json
{
  "stories":[{"id":"story-1","title":"A lost star","coverUrl":"https://assets.example.test/story-1.png","readingMinutes":4,"createdAt":1794355200000}],
  "nextCursor":null
}
```

Summaries omit text and configuration. `readingMinutes` is a positive integer,
`max(1, ceil(wordCount / 150))`, matching the reader's whitespace-delimited word
count. Order: descending createdAt, ascending story ID for ties. Use explicit
lexical ordering consistently rather than locale-dependent sorting.

Mobile requests limit 100; the backend accepts 1–100 and defaults to 100. Malformed/out-of-range limits return 422 `validation.limit.invalid_value`. Every page belongs
to the same stable snapshot, with no repeated IDs; stories published mid-pagination
appear on a later refresh, not halfway through the current traversal. Cursor is
opaque, URL-encoded, installation-bound and validated; last page omits it or returns null.
Errors: `400 INVALID_CURSOR`, `400 INVALID_INSTALLATION_ID`, `422 validation.limit.invalid_value`, `503 UNAVAILABLE`. Refresh never triggers POST/generation. See DB.md for one
concrete snapshot implementation.

## Background execution and delivery

The backend owns durable execution, recovery after restart, bounded concurrency,
provider throttling and publish-once completion. Do not keep a database transaction
or connection open while waiting for AI. A database-backed worker queue is sufficient
for the proposal; no Redis or microservice is required by the mobile contract.

Current mobile completion delivery uses polling and local notifications, not server
push. FCM/APNs registration endpoints, device tokens, and a notification outbox are
future scope. There is no backend reader-preference or reading-position API.

## Decisions and compatibility gaps before enabling production API

1. Installation-only ownership and a bare UUID header are confirmed for this stage.
   UUID persistence/shared headers are implemented for Places and Characters.
   Strong authentication, future account linking and library recovery remain separate
   scope; the UUID must not be presented as authenticated private storage.
2. Generated avatars remain in scope, as confirmed. Keep POST/poll/explicit save;
   bundled fake generation remains limited to disabled-API development mode.
3. Agree avatar job/unused-asset retention and request limits. The avatar POST lacks
   idempotency, unlike story submission. Client polling cancellation does not cancel
   provider work. Rate limits are not a substitute for product balances.
4. Define multi-device conflict resolution before enabling it. Current full PUTs
   have no version token; do not invent If-Match requirements the client cannot send.
5. Decide the asset privacy/delivery policy: current loaders require stable URLs
   without API auth; unguessable URLs do not provide authenticated private access.
6. Payments, free quotas and story/avatar balances were discussed but are not yet
   mobile endpoint contracts. They are intentionally excluded from this schema/API.
7. Existing fake-avatar records need a migration/regeneration path before real sync.

## Implemented API3 story behavior

The shared `contracts.v1.story` models define the shipped wire shapes. Start is
`POST /api/v1/story-generations` with UUIDv4 `Idempotency-Key`, returning durable
202 `{jobId,status}`. Status is `GET /api/v1/story-generations/{jobId}`, returning
200 `{jobId,status,storyId?,errorCode?}`; inaccessible/missing IDs return
404 `JOB_NOT_FOUND`. Reading `GET /api/v1/stories/{storyId}` returns the complete
immutable result or 404 `STORY_NOT_FOUND`. Reads never generate content.

Snapshots accept unsynchronized cast and bundled/custom places without live rows.
Cast must be nonempty and source IDs unique. Historical snapshot text is preserved
without current creation length caps. Unknown enums/malformed structures return
400 `INVALID_REQUEST`. Protected avatar references must match owned READY assets,
including their URL, or return 422 `REFERENCE_UNAVAILABLE`. Snapshot origin and
editable flags confer no permissions. MANUAL optional idea/ending are trimmed and
limited to 1000/500 UTF-16 units. AUTOMATIC forbids submitting place/idea/ending,
even as null. Validation returns shared error objects with flat 422 codes, e.g.
`validation.cast.required`: cast supports REQUIRED
and INVALID_VALUE; idea/ending support TOO_LONG.

Bindings include installation, operation, UUIDv4 key, original canonical request
and UUIDv7 logical job. JSON object keys are sorted recursively; array order and
null versus omission remain significant. Replay returns the same job's current
status before checking the active limit or live references. Changed content under
the key returns 409 `IDEMPOTENCY_CONFLICT`. Another key while a job is active returns
409 `ACTIVE_JOB_EXISTS`. One active story job is allowed per installation.

Stages are QUEUED → CREATING_TEXT → CREATING_COVER → READY, or FAILED. READY exposes
storyId only after text, cover and story are committed together. FAILED carries
`CONTENT_REJECTED` or `GENERATION_FAILED` in errorCode; no other stage carries it.
Text/title/ID/configuration/createdAt/cover are immutable. createdAt is nonnegative
UTC epoch milliseconds. The same cover URL serves the library thumbnail and reader.
`GET /api/v1/assets/{assetId}` returns local `image/png` bytes, requires the installation
header, and returns 404 `ASSET_NOT_FOUND` for missing/inaccessible assets. Stable
asset URLs default to HTTPS; the explicit development HTTP settings are documented
in the root README. There is no third-party image request or real AI provider.

Stable paginated listing is implemented through the shared Stories client and persistent SQLite snapshots. Snapshots and cursor tokens are retained without expiry in this development phase; unknown, malformed, cross-installation and limit-mismatched tokens return INVALID_CURSOR.

Explicit retry is implemented through the shared SDK. `DOZY_STORY_TEXT_MODE=FAIL_ONCE` fails ordinal 1 of each
logical job and succeeds on later executions, independent of user text. Existing
SUCCESS, FAILURE, CONTENT_REJECTED and cover modes remain available.

## API2 asset delivery and recovery

Missing and other-installation avatar jobs both return 404 `AVATAR_JOB_NOT_FOUND`.
FAILED observations return HTTP 200 with `error: "GENERATION_FAILED"`, no asset,
and no story `errorCode`. QUEUED/RUNNING contain neither asset nor error; READY
contains one complete asset and no error. The shared client validates combinations.
POST has no name or idempotency key and persists the owner/request before 202.

`GET /assets/{assetId}` delivers immutable PNG bytes without an installation header;
it is static public delivery, not a feature SDK endpoint. Its URL must match the
owned READY metadata for character PUT. Normal URLs use HTTPS; explicitly enabled
loopback/emulator HTTP development URLs are supported. See root README for settings.
A bounded worker pool recovers RUNNING jobs to QUEUED at process startup. Database
transactions cover only state changes, never generation, file writes or delay.
The job's UUIDv7 asset ID is fixed at acceptance; repeated publication is a no-op.
This local worker runs in one backend process per database; multi-process leasing
is outside this development implementation.

## Shared request, response and business-error types

All feature routes deserialize request DTOs and serialize success DTOs from
api/contracts. Story submission also retains raw JSON for canonical idempotency
binding (including null versus omitted), then decodes GenerationRequestDto before
domain validation. Query routes build the shared request type after query syntax
validation. BusinessApiException carries an ApiBusinessError contract object;
StatusPages serializes it with the shared code-envelope serializer. The client
decodes the operation-specific subtype directly. Validation codes use
validation.<field>.<reason>; fields/reasons are exposed as typed contract values,
not separate wire metadata. Unknown future codes remain Unknown(code).
