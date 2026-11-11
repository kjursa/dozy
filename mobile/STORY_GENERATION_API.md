# Proposed story generation contract (F9)

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

## Extension points for F10–F12

Proposed `GET /story-generations/{jobId}` returns 200 with jobId, status, and
`storyId` only when READY, or a typed `errorCode` when FAILED. 404 JOB_NOT_FOUND,
network errors and 5xx mean observation is unavailable, not that generation failed.
The locally persisted request configuration supports resuming observation. A deliberate
retry of a terminal failed job starts a new execution with a new key and the original
snapshots; transport retries continue to use the original key.

Proposed `GET /stories/{storyId}` returns 200 with id, title, text, coverUrl and
configuration (the original request snapshots). READY is published only after story
and cover are durably saved. Story IDs are independent of job IDs. 404 STORY_NOT_FOUND
and 503 UNAVAILABLE are typed errors. Domain Story and GenerationJob live in the story
feature, independently of the existing library presentation fixture.

F9 navigates only after acceptance to `generation/{jobId}`, removing the submitted
form from the stack. Its entry screen displays acceptance and the job reference;
status polling, progress, notifications and reader integration belong to F10–F12.
Reader entry can call `openStory(null, castIds)` to create a clean draft with only
the cast preselected. Character/place creation returns to the existing draft owner.
