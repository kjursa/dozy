# Dozy HTTP API

Open the [interactive endpoint reference](api/index.html) in a browser for request
and response examples. It works locally without a build step or API calls.

## Status and sources

This document combines implemented routes and proposals consolidated from the
mobile contracts. The backend implements `GET /hello`, `GET /places`, and
`PUT /places/{id}`. All other feature endpoints remain proposals. `/hello` returns
`200 {"message":"Hello world from Dozy"}`. Places use a local SQLite database;
see [runtime storage](DB.md#current-sqlite-implementation) and the root README for configuration.

Sources:

- [Character contract](../mobile/CHARACTER_DATA.md), including F2–F5 amendments.
- [Place contract](../mobile/PLACE_DATA.md).
- [Story contract](../mobile/STORY_GENERATION_API.md), including F9–F13 amendments.
- [Mobile specification](../mobile/SPEC.MD).
- Clients: `KtorCharacterApi`, `KtorPlaceApi`, and `KtorGenerationApi` under
  `mobile/shared/src/commonMain/kotlin/com/dozy/mobile/feature/`.
- [Proposed PostgreSQL schema](DB.md).

Later feature amendments take precedence over historical implementation notes.
For example, characters now support deletion, and full story responses include
`createdAt`. Local Room schema versions and sync flags are not backend API fields.

## Shared conventions

- Paths are relative to the configured API base URL; no `/v1` prefix is currently
  hardcoded in mobile. Requests/responses use JSON unless stated otherwise.
- Personal resources belong to an installation, not a user account. As confirmed
  for this stage, send `X-Installation-Id: <client-generated UUID>` on every feature
  request, with no separate secret or login endpoint. Persist the UUID locally;
  the server registers it lazily on first use. This header is a proposed shared
  mobile HTTP addition, not present in current clients. Missing/invalid headers
  return `400 INVALID_INSTALLATION_ID`. The ID partitions data but is not strong
  authentication: a caller knowing another ID could access its namespace. No
  library recovery is defined after losing the ID. Never take ownership from JSON.
- Character/place IDs are opaque strings, not SQL UUID assumptions. Custom mobile
  IDs are UUID strings; predefined places use `builtin:` IDs. Encode path IDs as
  one path segment. Story/job IDs match `[A-Za-z0-9_-]{1,128}`. Avatar job IDs use
  the same alphabet; a 128-character cap is a proposed server convention.
- Enum wire values are uppercase. Optional nullable fields can be omitted or null
  unless a specific operation requires omission. Responses must preserve required
  fields even if the server serializer normally omits defaults.
- Proposed common errors: `{"code":"INVALID_REQUEST"}`. Codes are machine-readable,
  not display-ready messages. The current clients mostly show localized generic
  errors. Field-level server error payloads are not yet defined.
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
  Current mobile image loading does not send API credentials. Do not return
  short-lived signed URLs without extending the mobile contract first.

## Endpoint inventory

| Method | Path | Success | Purpose |
| --- | --- | --- | --- |
| GET | `/characters` | 200 | Complete personal character collection |
| PUT | `/characters/{id}` | 201 / 204 | Create or replace a character |
| DELETE | `/characters/{id}` | 204 / 404 | Idempotent character deletion |
| POST | `/character-avatars` | 202 | Start a standalone avatar job |
| GET | `/character-avatars/{jobId}` | 200 | Observe an avatar job |
| GET | `/places` | 200 | Personal and predefined places |
| PUT | `/places/{id}` | 201 / 204 | Create or replace a custom place |
| POST | `/story-generations` | 202 | Accept a durable story-generation job |
| GET | `/story-generations/{jobId}` | 200 | Observe generation stages |
| POST | `/story-generations/{jobId}/retry` | 202 | Explicitly retry a failed job |
| GET | `/stories/{storyId}` | 200 | Read an immutable complete story |
| GET | `/stories?limit=100&cursor=...` | 200 | Page through saved story summaries |

There are **12 method/path operations** above. `GET /characters/{id}` is an optional
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

### GET /characters

No body or pagination. Return `200` and a **bare JSON array** of Character objects,
not `{ "characters": [...] }`. Include all non-deleted characters visible to the
owner in a consistent snapshot; IDs must be unique. `[]` is authoritative.

Non-200 responses are refresh failures; mobile retains known data. Pending local
edits and local deletion tombstones take precedence over a fetched snapshot.
There is no separate required character-details read endpoint.

### PUT /characters/{id}

Body: complete Character object. Path and body IDs must match. Use the client ID
for an idempotent upsert; repeating an identical PUT creates no duplicate.
Proposed success: `201` for creation, `204` for replacement, no body. Mobile accepts
any 2xx. Name/description edits may retain an earlier valid avatar: do not require
its generation input to match the updated descriptions.

Reject invalid input with `400`/`422`, unauthorized writes with `401`/`403`, ownership
or deleted-ID conflicts with `409`, and unavailable resources with `404` where
appropriate. No version token is currently sent: the mobile proposal uses last
acknowledged write wins, not optimistic version reconciliation. Never revive a
previously deleted ID through PUT. Server error messages are not shown verbatim.

### DELETE /characters/{id}

No body. `204` acknowledges durable removal; `404` also completes mobile deletion.
Repeated deletion is safe. Other statuses fail the operation. Preserve an ID
reservation/tombstone so later PUT cannot reuse the ID. Deletion must not change
saved story text, cast snapshots, covers, or in-flight job configurations.

## Character avatars

### POST /character-avatars

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

### GET /character-avatars/{jobId}

Return `200` with the same envelope. Status: `QUEUED`, `RUNNING`, `READY`, `FAILED`.

```json
{"jobId":"avatar-job-1","status":"READY","asset":{"id":"asset-1","url":"https://assets.example.test/asset-1.png"},"error":null}
```

READY requires a complete durable asset; FAILED may carry a code in **`error`**
(for example `GENERATION_FAILED`), not the story endpoint's `errorCode`. Use null or
omit non-applicable optional fields. Unknown/mismatched jobs, invalid responses,
non-success HTTP, or observation timeout are mobile failures, not implicit deletion.
The current client polls every second for up to 120 GETs. A later PUT explicitly
attaches the ready asset to a character. Retention/cleanup of unattached results
needs a policy that accommodates persisted unsaved form drafts.

Local fake avatars (`bundled-avatar://...`) exist only in disabled-API development
mode. They are not server assets or accepted HTTPS transport references. Users
must regenerate them with the real service before uploading those references.

## Places

### Place representation

```json
{
  "id":"builtin:woods",
  "name":"Whispering woods",
  "description":"A peaceful forest with glowing paths.",
  "inhabitants":null,
  "ownership":"PREDEFINED",
  "editable":false
}
```

`ownership`: `CUSTOM` or `PREDEFINED`. `editable` is computed from permissions;
clients cannot grant it by submitting true. Predefined places must not be editable.
Custom places use non-`builtin:` IDs. No image field is required. Normalize blank
inhabitants to null. Current form rules: name required/max 80, description
required/max 500, inhabitants optional/max 500, using trimmed UTF-16 lengths.

### GET /places

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

### PUT /places/{id}

Implemented. Requires `X-Installation-Id` and `Content-Type: application/json`.

Body: full Place with matching ID, `ownership: "CUSTOM"`, `editable: true`.
Omitted/null inhabitants clears the previous value. Only the owner may upsert a
custom record; reject writes to the predefined namespace. Responses:
`201` creation, `204` update, no body; mobile accepts any 2xx.
Repeated identical PUTs replace one resource. A transaction checks ownership and
writes the replacement atomically. `editable: true` does not bypass permissions.

Implemented place errors use `{"code":"..."}`:

| Status | Code | Cause |
| --- | --- | --- |
| 400 | `INVALID_INSTALLATION_ID` | Missing, duplicated, or non-canonical UUID header |
| 400 | `INVALID_REQUEST` | Malformed/missing JSON fields, path/body ID mismatch, incorrect ownership/editable flags |
| 422 | `INVALID_REQUEST` | Invalid trimmed name, description, or inhabitants |
| 403 | `FORBIDDEN` | Attempt to write any `builtin:` ID, including unknown ones |
| 409 | `CONFLICT` | Custom ID already belongs to another installation |
| 503 | `UNAVAILABLE` | SQLite operation failed |

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
  "place":{"id":"builtin:woods","name":"Woods","description":"Quiet woods","inhabitants":null,"ownership":"PREDEFINED","editable":false},
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

### POST /story-generations

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

### GET /story-generations/{jobId}

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

### POST /story-generations/{jobId}/retry

No body. Header `Idempotency-Key: <new client UUID>`. Return 202 `{jobId,status}`
with the **same logical job ID**. Reuse the original snapshot, never edited live data.

Only a FAILED job may start a new execution. Bind each retry key atomically to one
specific execution. Replay checks precede lifecycle checks: replay the recorded
execution's current status even if it is already FAILED/READY or a later execution
exists; never start another attempt using that key. A new explicit retry after a
confirmed later failure gets a new key. Return `409 JOB_NOT_FAILED` for a fresh key
on active/completed jobs, `409 ACTIVE_JOB_EXISTS` if another job is active, or
`409 IDEMPOTENCY_CONFLICT` if a key is bound to a different operation/job.
A logical job can have only one successful story.

## Stories

### GET /stories/{storyId}

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

### GET /stories?limit=100&cursor=<opaque cursor>

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

Mobile requests limit 100; a proposed backend bound is 1–100. Every page belongs
to the same stable snapshot, with no repeated IDs; stories published mid-pagination
appear on a later refresh, not halfway through the current traversal. Cursor is
opaque, URL-encoded, installation-bound and validated; last page omits it or returns null.
Errors: `400 INVALID_CURSOR`, `401 UNAUTHORIZED`, `429 RATE_LIMITED`,
`503 UNAVAILABLE`. Refresh never triggers POST/generation. See DB.md for one
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
   Implement UUID persistence/shared HTTP headers in mobile before enabling the API.
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
