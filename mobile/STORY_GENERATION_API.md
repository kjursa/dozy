# Proposed story generation contract (F9/F10)

This contract is a mobile proposal, not an implemented backend endpoint. It extends
CHARACTER_DATA.md and PLACE_DATA.md using the same string IDs and uppercase enums.
The client uses ApiConfig. A null base URL performs no HTTP requests and reports
unavailability; there is no simulated acceptance.

## Start and replay

`POST /story-generations`, JSON, `Idempotency-Key: <client UUID>`.

Request:

```json
{
  "cast": [{"id":"hero-1","name":"Luna","personality":"Kind","appearance":"Red hair","type":"CHILD","avatar":null}],
  "mode":"MANUAL",
  "length":"SHORT",
  "place":{"id":"builtin:woods","name":"Woods","description":"Quiet woods","ownership":"PREDEFINED","editable":false,"inhabitants":null},
  "idea":"Help a lost star",
  "ending":"Everyone returns home"
}
```

Cast and place are complete snapshots with source references, not live lookups.
The server must retain the submitted values, including for built-in/local objects
not yet synchronized. Later edits cannot alter the requested story. Place inhabitants
are setting context, never additional cast. SHORT/LONG mean approximately 3–5/8–12
minutes. Cast is nonempty with unique IDs. Idea is optional (1000 characters), ending
optional (500); whitespace is trimmed. AUTOMATIC includes cast, mode and length only;
manual place, idea and ending are omitted, including when retained in the draft.

Response: `202 {"jobId":"job-1","status":"QUEUED"}`. A replay returns 202 with
the same job ID and current status. IDs are opaque URL-safe strings (1–128 ASCII
letters, digits, underscores or hyphens). Status is QUEUED, CREATING_TEXT,
CREATING_COVER, READY or FAILED.

The server atomically binds an idempotency key to the request and resulting job,
retaining the binding for the lifetime of the job/story. Concurrent identical
requests must create one job. A different body with the same key returns 409
IDEMPOTENCY_CONFLICT. Do not expire keys while clients may replay uncertain requests.
The client persists the exact request before HTTP and accepted ID/status afterward
in DozyDatabase v6. Retry after timeout, process death, malformed response, or local
acceptance-write failure replays the saved body/key. The draft retains the key until
an input edit starts a new request. Accepted keys return the local job without POST.
A changed draft intentionally represents a new request; it does not cancel a prior job.

Errors use `{"code":"..."}`: 400 INVALID_REQUEST, 401 UNAUTHORIZED,
404 REFERENCE_UNAVAILABLE, 409 IDEMPOTENCY_CONFLICT, 429 RATE_LIMITED,
503 GENERATION_UNAVAILABLE. The client retains the form on any non-202 response,
transport failure or malformed acceptance; server text is not displayed. Retrying
uses the same key, never an automatic new execution. Coroutine cancellation propagates.

## Status, completion, and durable results (F10/F11 contract)

`GET /story-generations/{jobId}` returns 200:

```json
{"jobId":"job-1","status":"CREATING_TEXT"}
```

Statuses are QUEUED (accepted), CREATING_TEXT, CREATING_COVER (exactly one image),
READY, FAILED. These are stages, not percentages. READY requires `storyId`, e.g.
`{"jobId":"job-1","status":"READY","storyId":"story-1"}`. FAILED requires
`errorCode`: CONTENT_REJECTED or GENERATION_FAILED. These fields are absent in other
states. No raw server error text is displayed. Unknown failure codes, malformed
responses, 401, 404 JOB_NOT_FOUND, 429, 5xx, and transport errors mean **status
unavailable**, never FAILED. A subsequent successful read clears unavailability.
The server must serve consistent, monotonic stage reads for the current execution.

`GET /stories/{storyId}` returns 200 with the following JSON DTO:

```json
{
  "id":"story-1",
  "title":"A lost star",
  "text":"Once upon a time…",
  "coverUrl":"https://assets.example.test/story-1.png",
  "configuration":{"cast":[],"mode":"AUTOMATIC","length":"SHORT"}
}
```

The example configuration is abbreviated: the real cast contains the nonempty
original character snapshots from POST. Configuration uses the exact F9 request DTO.
The title and text are nonblank; the cover URL is a durable HTTPS reference. IDs
obey F9's URL-safe ID rules. 404 STORY_NOT_FOUND and 503 UNAVAILABLE are observation
failures. Publish READY only after the complete text and cover are durably readable.
The same logical job always returns the same immutable story ID and content.
Story IDs are independent of job IDs. The client checks returned IDs and snapshots,
then stores the complete DTO and READY status in **one atomic Room row update** in
DozyDatabase v7. A failed story fetch leaves the previous local stage plus unknown
status. Repeated reads do not re-download or duplicate an already saved story.
The shared repository exposes saved domain stories for Home, library, and a minimal
reader. Text is local; cover bytes load from the durable asset URL when available.
A full offline image cache and the remaining reader controls belong to F11/F12.

## Failed-job retry

`POST /story-generations/{jobId}/retry`, no body,
`Idempotency-Key: <new client UUID>` returns 202 `{jobId,status}` using the same
logical job ID. Only FAILED jobs can start a new execution. The backend reuses the
original immutable configuration; the client never reconstructs it from edited or
deleted characters/places. The logical job has at most one successful durable result.

The server atomically binds each retry key to one execution. Replay must return its
current status even when that execution has already become FAILED or READY; it must
never execute again. A new explicit retry after a confirmed subsequent failure uses
a new key. A different logical job/key binding returns 409 IDEMPOTENCY_CONFLICT;
retrying an active or completed job returns 409 JOB_NOT_FAILED. GET remains valid
through retries. Retain all bindings for the lifetime of the job/story.

The client saves its pending retry key before HTTP, retains it on timeout or process
death, and replays it from background reconciliation. Acceptance clears the pending
key. A confirmed progressing/completed GET can also acknowledge the pending retry.
The local mutex and disabled action prevent duplicate concurrent submissions. A
completed local job cannot be retried. One active job is allowed; starting another
or retrying an older failed job while one is active is rejected locally. The backend
must enforce the same per-account limit (409 ACTIVE_JOB_EXISTS), including uncertain
initial POSTs and other devices. No cancellation or multi-job management UI is added.

## Scheduling and notification delivery

Generation executes on the backend, independent of Android processes and screens.
Android WorkManager runs unique immediate reconciliation work and a persistent
15-minute recovery sweep. An active work run polls every 10 seconds for up to eight
minutes, then asks WorkManager to retry with linear backoff starting at 10 seconds.
Network failures preserve known state and retry on later polls. Launching the app
resumes work from Room; accepted jobs never submit another initial POST. New durable
job changes enqueue a check. Navigation, system back, or closing the activity does
not cancel work. The periodic sweep closes scheduling gaps if a process dies after
saving acceptance and before enqueuing the immediate work. WorkManager persists
across normal process death/reboot, subject to Android scheduling and battery limits.

A successful background reconciliation saves the result first, then posts a local
notification with a stable story-ID tag and persists delivery acknowledgement.
Replay across a crash replaces the same notification instead of adding another.
The explicit immutable PendingIntent has unique `dozy://stories/{storyId}` data and
opens that story on cold or warm start, with Home below it. IDs are validated at the
activity boundary. No external deep-link intent filter is exposed. Notification
permission is requested only after the parent taps the opt-in action on progress
(Android 13+); generation never waits for it. Permission/channel denial is normal:
Home and library still show the saved result. Granting permission schedules a check.
Open progress stays on its ready card with a Read story action; no automatic jump.

**Backend/system integration requirements:** these proposed endpoints and a durable
backend job executor must be implemented before enabling ApiConfig. The current
client uses polling, not push, so completion alerts can be delayed by Doze, OS quotas,
network loss, or manufacturer restrictions. Android force-stop suppresses background
work until the user opens the app again; permission cannot override this. Guaranteed
prompt delivery while the process is absent requires a separate FCM integration:
register/rotate device tokens, authenticate account ownership, deliver a data message
containing validated job/story IDs after durable READY, enqueue the same reconciliation
worker, and fetch/save before notifying. No device-token endpoint or push service is
claimed by this mobile slice. A normal close does not require an open ViewModel or
an activity coroutine for progress or completion delivery.

F9 acceptance removes the submitted form from the stack and opens
`generation/{jobId}`. Back to Home or system back preserves the same observed job.
Home's latest-job card supports progress, status refresh, failed-job retry, and opening
the exact saved result. The library reads the same persisted collection. Notification
and ready-card navigation use `reader/{storyId}`; unavailable local stories show a
return action rather than a fictitious result. Existing creation return-to-origin
and cast-only new-adventure entry points remain shared with F9.

## Reader (F11)

The reader reuses the exact `GET /stories/{storyId}` and `StoryDto` above; there is
no request body, reader-specific content endpoint, or preferences endpoint. Reading
time is a client estimate from text at 150 words/minute (minimum one). Saved text
and cast snapshots are authoritative and are read before any HTTP request. A fetched
story is validated and persisted in the existing story_generations table with a
reserved `story:<id>` key and no job ID, making it available to the same library.
404 STORY_NOT_FOUND means missing; disabled ApiConfig, authentication errors,
rate limits, malformed/ID-mismatched data, transport failures and 5xx mean unavailable.
Retry is an explicit idempotent GET, with no concurrent duplicate reads per reader;
no generation or new image is triggered. Coroutine cancellation propagates.

The cover URL must remain readable for the lifetime of the saved story, rather than
be a short-lived signed URL. Library, opening cover and full-screen preview use the
same reference. Text works offline; cover availability offline depends on the image
loader cache. Expiring signed URLs require a future stable-reference resolution
contract before enabling the API. Reader settings and reading position are local to
the restored navigation entry as described in SPEC §13, with no server synchronization.

## Saved library (F12)

`GET /stories?limit=100&cursor=<opaque cursor>` returns 200:

```json
{
  "stories": [
    {"id":"story-1","title":"A lost star","coverUrl":"https://assets.example.test/story-1.png","readingMinutes":4,"createdAt":1794355200000}
  ],
  "nextCursor": null
}
```

There is no request body. IDs, titles and durable cover references match
`GET /stories/{id}`. Summaries omit text and configuration. `readingMinutes` is a
positive integer estimated at 150 words/minute, minimum one, consistent with the
reader. `createdAt` is the immutable story creation timestamp in UTC epoch
milliseconds (nonnegative). Add the same `createdAt` field to the full story DTO;
legacy full results without it are supported as timestamp zero. New generation
results must supply it. Sort newest first, then by ascending story ID to break ties.
No series, editing or deletion operation is implied.

Pages belong to a stable snapshot, have unique story IDs across pages, and use
opaque URL-encoded cursors. The last page omits `nextCursor` or returns null. A
refresh starts at the first page and follows all cursors; repeated/blank cursors,
duplicate IDs, invalid metadata or malformed pages fail the refresh. The client
validates all pages before merging. Cancellation propagates. Retry is an explicit
idempotent GET starting at page one; no generation, cover generation or POST occurs.

Errors use the existing `{code}` envelope: 400 INVALID_CURSOR, 401 UNAUTHORIZED,
429 RATE_LIMITED, 503 UNAVAILABLE. All non-200 responses and invalid bodies show a
retryable library error while retaining known cards; raw server messages are not
shown. Transport I/O failures and null ApiConfig show the offline library, without
claiming a successful synchronization. Null configuration sends no requests.

The existing shared GenerationRepository owns both jobs and stories; no parallel
list repository is introduced. DozyDatabase v8 adds nullable summary JSON to the
feature's existing table with an automatic migration from v7. Remote summaries use
reserved `library:<id>` rows without jobs. Locally completed jobs and durable reader
fetches continue storing the complete result in their existing rows. Observation
merges by story ID, choosing the metadata with the newest creation timestamp and
sorting deterministically. Summary refresh never overwrites full text, job status,
configuration or notification state and never deletes absent stories, including on
empty responses or temporary failures. The reader fetches full text only when no
local result exists. A remote summary alone therefore does not promise offline text.


## Home composition (F13)

Home adds no endpoint, DTO, storage table, or repository. It composes F2/F3
`ObserveCharacters` / `RefreshCharacters` ([character contract](CHARACTER_DATA.md)),
F6 `ObservePlaces` / `RefreshPlaces` ([place contract](PLACE_DATA.md)), F10
`ObserveGenerations` / `RefreshGeneration` / `RetryGeneration`, and F12
`ObserveStoryLibrary` / `RefreshStories`. The existing flat repository implementations
own DAO/API access in the single DozyDatabase; the existing Koin feature modules
provide these constructor-injected use cases to Home.

On entry/resume, existing GET collection contracts refresh characters, places, and
`GET /stories?limit=100&cursor=...`; Home previews the persisted collections with
limits/order documented in SPEC §4. These are client preview limits, not new server
pagination parameters. Existing response DTOs, stable character/place/story IDs,
error envelopes and cache merging rules remain authoritative. Errors retain each
section's last known data and provide section-specific retry. Bundled predefined
places are intentional offline content, not sample user data.

The displayed job follows F10's active/pending-retry priority, otherwise the latest
persisted job. Resume uses `GET /story-generations/{jobId}` and the existing status DTO;
READY still requires the full `GET /stories/{storyId}` result to be saved atomically.
An unavailable status remains unknown rather than failed. Retry explicitly uses
`POST /story-generations/{jobId}/retry` with the existing preserved configuration and
durable Idempotency-Key; acceptance returns to generating. Repeated taps cannot
start parallel retries. Background reconciliation and notification delivery continue
to belong to F10's scheduler, independent of Home.

All GET retries are idempotent; POST retries retain F10's uncertain-response key.
ApiConfig(baseUrl = null) sends no requests and cannot simulate successful generation
or retry. Collections remain locally readable; job status can show unavailable.
Home consumes the same Ktor clients verified by the existing MockEngine character,
place, generation and story-library contract tests. No backend/generator change is
included in this mobile proposal.
