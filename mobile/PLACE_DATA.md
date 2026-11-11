# Places collection and proposed API

This mobile contract is a proposal. The backend currently has no Places endpoints;
this task does not add backend or generator code. It follows `CHARACTER_DATA.md`:
opaque string identifiers, a platform-provided Ktor client, `ApiConfig`, and a
single durable `DozyDatabase`.

## Collection request

`GET /places`, no query parameters or request body. With a configured base URL,
the client sends one request on entry and one per explicit retry. Concurrent
retries are ignored. There is no automatic retry loop. GET is safe to repeat.

A successful response is HTTP 200 with `Content-Type: application/json`:

```json
{
  "places": [
    {
      "id": "c48b54fb-033f-49ac-b48c-c665644775d1",
      "name": "Grandma's garden",
      "description": "A sunny garden with an old apple tree.",
      "inhabitants": "A friendly hedgehog",
      "ownership": "CUSTOM",
      "editable": true
    },
    {
      "id": "builtin:woods",
      "name": "Whispering woods",
      "description": "A peaceful forest with glowing paths and old trees.",
      "ownership": "PREDEFINED",
      "editable": false
    }
  ]
}
```

IDs are stable, unique opaque strings; local custom places use UUIDs. The
`builtin:` namespace is reserved for predefined catalogue entries. `ownership`
is `CUSTOM` or `PREDEFINED`; only a custom place with `editable: true` offers
editing. Predefined entries must have `editable: false`. `inhabitants` may be
omitted or null; blank values are treated as absent. No image field is required.
Existing names/descriptions are not subjected to new-form length restrictions.
Missing required fields, blank identifiers/names/descriptions, unknown ownership,
duplicate IDs, or editable predefined entries reject the entire snapshot before
cache mutation.

Any non-200 status is a failed refresh, including 401/403 (authorization), 404
(endpoint unavailable), 429 (rate limited), and 5xx (server unavailable). The
client displays a resource-backed error and retains available places. Error
bodies are not displayed or interpreted as user-facing text. A future backend
may define structured error DTOs before authentication or mutation is enabled.
Network/decoding/storage failures have the same retry behavior. Coroutine
cancellation propagates. A null base URL makes no requests and explicitly returns
an offline result rather than claiming a remote refresh succeeded.

## Offline availability, order, and synchronization

Three resource-backed suggestions ship in the app: `builtin:woods`,
`builtin:island`, and `builtin:village`. They are available on first launch and
with a disabled API. Persisted entries with matching IDs override those bundled
fallbacks. The collection is sorted by custom ownership first, then by
lowercased name and ID within each source. It has no source-based headings.
Home uses the first three entries from the same collection.

Room schema version 2 adds the feature-owned Places table to `DozyDatabase` with
an automatic migration from version 1, preserving characters. Observation reads
Room and the bundled fallback without HTTP. A successful GET is an authoritative
snapshot of remote rows: replace those rows in one transaction, retain all local
custom rows, and preserve local edits when a remote ID collides. Failed requests
leave the cache intact. An empty remote snapshot still leaves bundled suggestions
and local places. Storage observation errors retain bundled suggestions and show
a retry action. A genuine empty collection is also supported by the screen.

The add/edit form saves custom places durably before attempting upload, including
when the backend is disabled. Schema version 3 adds character avatar fields;
schema version 4 adds `pendingSync` with an automatic migration from version 3.
Existing local rows are pending; remote rows are never
queued. Save returns `SYNCED` only after acknowledgement and durable metadata,
otherwise `PENDING`. Only failure to persist the initial row fails the form save.
A pending local save can be selected immediately and is never described as a
successful backend operation. Cancellation propagates and leaves durable work
pending. Local rows remain authoritative on this device, even after upload;
GET cannot overwrite them. Multi-device conflict resolution is deferred. New-form limits
follow SPEC section 10: trimmed required name (80 characters), required description
(500), optional inhabitants (500), with all field errors collected.

## Custom-place mutation proposal

`PUT /places/{id}` uses the same opaque UUID on every retry. Path identifiers are
encoded as a single segment. The JSON request is `PlaceDto`, matching GET items:
`id`, `name`, `description`, `ownership: "CUSTOM"`, `editable: true`, and optional
nullable `inhabitants`. Empty optional input becomes null. PUT replaces the full setting: an omitted or
null `inhabitants` field clears previously saved inhabitants. No image or character
record is needed. The server must require path/body ID equality and authorize
ownership; the client cannot grant permissions through `editable`.

Any 2xx status acknowledges the upsert (proposed 201 for creation, 204 for update;
no response body is required). Repeated identical PUTs produce one resource.
400/422 indicate invalid input, 401/403 authorization, 404 unavailable resource or unsupported endpoint,
409 ownership conflict, 429 rate limiting, and 5xx temporary failure. Error bodies
are not consumed or shown as user-facing copy. All non-2xx, network failures, and
acknowledgement-storage failures retain the pending row, with no fictitious remote
success. No backend endpoint is implemented in this task.

A save attempts one PUT. Entry/explicit refresh retries persisted pending rows
once each before GET; there is no background retry loop. Saving and refresh
mutations are serialized to avoid an old acknowledgement overwriting a newer
local edit. Null `ApiConfig.baseUrl` produces no requests. Reopening a form after
an interrupted save should edit the existing saved item from the list. Pending
rows survive process death and keep their IDs on refresh, including when the
server accepted a request whose response was lost. This matches the local-first
character contract while adding a durable refresh-driven retry queue.

## Editing existing places

Edit resolves the ID from the same local/cache/bundled collection as selection;
no separate GET-by-ID endpoint is required. Loading blocks editing and saving.
A missing ID displays an unavailable message; storage failures display a load
error with retry; predefined or non-editable custom records display a permission
error. Back remains available. The form restores unsaved input and the original
ID through `StatePersistence`, never overwriting restored input with a reload.
Saving rechecks existence and editability in the use case. The repository also
checks stored/bundled permissions under its mutation lock, and rejects custom
writes into the reserved `builtin:` namespace, even if a caller forges ownership.

Edits upsert the original ID, with no duplicate row or new ID generation. Clearing
inhabitants writes null. Collection and Home observe the same database and see
changes immediately. Only the setting record is changed: existing story text and
covers are never regenerated or rewritten. The pending-sync and retry policy
above applies equally to edits, including server permission/conflict responses;
such responses do not acknowledge remote success or discard the local draft.
Server-side authorization remains mandatory. Multi-device permission revocation
and conflict reconciliation require a future contract decision.

## Selection and navigation

Selection resolves an available place but never updates its saved row. A story
draft persists the setting ID independently of heroes, length, adventure text,
ending, and automatic plot mode. Inhabitants remain setting inspiration; no hero
is added by selecting a place. A random setting clears only the explicit place ID
and leaves automatic plot mode unchanged.

Places opened from a story returns to that same navigation-owned, persisted
draft. Back changes no draft input. Home opens a new story draft. Add/edit retains
the Places entry on the back stack; cancel returns there, and save selects the
place through the same selection flow. Form fields and draft input use typed
`StatePersistence`; errors, progress, and derived names are transient. One-off
navigation uses effects. Buttons guard duplicate selection, refresh, and save.
