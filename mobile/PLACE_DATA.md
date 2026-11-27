# Places collection and API

Android uses the shared Places SDK and caches the server's catalogue in Room.

## Collection request

`GET /api/v1/places`, no query parameters or request body. With a configured base URL,
the client sends one request on entry and one per explicit retry. Concurrent
retries are ignored. There is no automatic retry loop. GET is safe to repeat.

A successful response is HTTP 200 with `Content-Type: application/json`:

```json
{
  "places": [
    {
      "id": "place-019a0000-0000-7000-8000-000000000001",
      "name": "Grandma's garden",
      "description": "A sunny garden with an old apple tree.",
      "inhabitants": "A friendly hedgehog",
      "origin": "USER",
      "editable": true
    },
    {
      "id": "place-00000000-0000-7000-8000-000000000001",
      "name": "Whispering woods",
      "description": "A peaceful forest with glowing paths and old trees.",
      "origin": "SYSTEM",
      "editable": false
    }
  ]
}
```

The backend assigns stable `place-<uuidv7>` IDs. Android treats them as opaque
strings. Only a place with `origin: USER` and `editable: true` offers editing.
System entries have `editable: false`. Optional `imageUrl` values point to
authenticated thumbnails. Blank or omitted inhabitants are treated as absent.
Existing names/descriptions are not subjected to new-form length restrictions.
Missing required fields, blank identifiers/names/descriptions, unknown origin,
duplicate IDs, or editable predefined entries reject the entire snapshot before
cache mutation.

A conditional GET can return HTTP 304 to reuse the local snapshot. Responses other
than 200 or 304 fail the refresh, including 401/403 (authorization), 404
(endpoint unavailable), 429 (rate limited), and 5xx (server unavailable). The
client displays a resource-backed error and retains available places. Error
bodies are not displayed or interpreted as user-facing text. The shared SDK returns operation-specific `PlacesListResult`/`PlacesSaveResult`
with Success, ApiError and TechnicalError branches. Shared technical errors are
`TechnicalError.Server(status)` for 5xx and `TechnicalError.Communication(exception)`
for all other Exceptions, including request/response/transport and argument errors.
Cancellation propagates; clients need no exception handling around these calls. Places validation uses shared serializable `contracts.v1.place.PlacesSaveError`
values selected by `code` and carrying typed `field` and `reason` details. Unknown
codes or incomplete known variants decode as `Unknown(code)`; extra fields are ignored.
Network/decoding/storage failures have the same retry behavior. Coroutine
cancellation propagates. A null base URL makes no requests and explicitly returns
an offline result rather than claiming a remote refresh succeeded.

## Offline availability, order, and synchronization

Android sorts user places first, then by lowercased name and ID. Home uses the
first three entries from the same collection. Observation reads the Room cache
without HTTP. A full GET response replaces the synced places and stores its ETag in
one transaction. Pending edits stay local. Subsequent refreshes send the stored
ETag as `If-None-Match`. Failed refreshes leave cached places available.

Creation sends a POST and stores the server-assigned ID upon success. Editing an
existing place persists the change before attempting PUT. A pending edit survives
process death and is retried on refresh. New-form limits
follow SPEC section 10: trimmed required name (80 characters), required description
(500), optional inhabitants (500), with all field errors collected.

## Custom-place mutations

`PUT /api/v1/places/{id}` uses the same opaque UUID on every retry. Path identifiers are
encoded as a single segment. The JSON request uses `SavePlaceRequestDto`:
`name`, `description`, and optional `inhabitants`. The ID is supplied only in the path;
`origin` and `editable` are response fields assigned by the backend.
Empty optional input becomes null. PUT replaces the full setting: an omitted or
null `inhabitants` field clears previously saved inhabitants. No image or character
record is needed. The server takes the ID from the path and independently authorizes
writes using the bearer token and the stored place's origin and owner. The client does not submit `origin` or `editable`.

POST returns 201 with the assigned ID and an optional thumbnail URL. PUT returns
204 after replacing the existing place. Repeated identical PUTs update the same resource.
400/422 indicate invalid input, 401/403 authorization, 404 unavailable resource or unsupported endpoint,
409 ownership conflict, 429 rate limiting, and 5xx temporary failure. Error bodies
are not consumed or shown as user-facing copy. All non-2xx, network failures, and
acknowledgement-storage failures retain the pending row, with no fictitious remote
success.

A save attempts one PUT. Entry/explicit refresh retries persisted pending rows
once each before GET; there is no background retry loop. Saving and refresh
mutations are serialized to avoid an old acknowledgement overwriting a newer
local edit. Null `ApiConfig.baseUrl` produces no requests. Reopening a form after
an interrupted save should edit the existing saved item from the list. Pending
rows survive process death and keep their IDs on refresh, including when the
server accepted a request whose response was lost. This matches the local-first
character contract while adding a durable refresh-driven retry queue.

## Editing existing places

Edit resolves the ID from the same local collection as selection;
no separate GET-by-ID endpoint is required. Loading blocks editing and saving.
A missing ID displays an unavailable message; storage failures display a load
error with retry; predefined or non-editable custom records display a permission
error. Back remains available. The form restores unsaved input and the original
ID through `StatePersistence`, never overwriting restored input with a reload.
Saving rechecks existence and editability in the use case. The repository also
checks the stored origin and editability under its mutation lock.

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
