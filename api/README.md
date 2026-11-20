# Dozy API contracts and client

This independent Kotlin Multiplatform build is shared through `includeBuild("../api")`
by `backend/` and `mobile/`. No Maven publication is needed for local development.
Both modules use Kotlin 2.3.21, JVM bytecode 17 (also consumed by Android), and
`iosArm64` / `iosSimulatorArm64`. Build tooling uses JDK 21. No iOS application
launch or E2E suite is included.

## Modules

- `contracts` (`com.dozy.api:contracts:0.1.0`): transport-only DTOs, shared error
  envelopes, and API version paths. Depends on Kotlin serialization, not Ktor,
  server code, database code, or either application's domain models.
- `client` (`com.dozy.api:client:0.1.0`): Ktor Client-based `DozyClient` and
  `AuthClients`, `PlacesClients`, `CharactersClients`, `CharacterAvatarsClients`, `StoryGenerationsClients`, and `StoriesClients` version containers, with its contracts dependency exposed to consumers. Engines,
  session storage, and the HTTP client lifetime belong
  to the caller.

Places, Characters, story submission/status/retry and immutable story reading are implemented in the shared contracts/client and backend. Story wire models live in `contracts.v1.story`; explicit retry uses `storyGenerations.v1.retry(jobId, idempotencyKey)`; listing uses `stories.v1.list(ListStoriesRequest)`. Avatar jobs are implemented by API2.

Avatar DTOs,
states and failure codes live in `contracts.v1.characteravatar` and reuse the
character asset reference and description validation contracts.

`DozyClient.characterAvatars.v1.start(request)` performs one POST (202), and
`status(jobId)` performs one GET (200). They return `AvatarStartResult` and `AvatarStatusResult`.
FAILED jobs are successful payloads with `error`; unknown error codes and validation
details remain Unknown. Invalid request structure, malformed responses and transport
failures are `TechnicalError.Communication`; cancellation propagates.
Polling belongs to consumers. Every explicit POST creates an independent job.


## Usage

```kotlin
val dozy = DozyClient(
    httpClient = httpClient,
    config = DozyClientConfig(baseUrl = "http://localhost:8080"),
    sessionStorage = platformAuthSessionStorage,
)
val result = dozy.places.v1.create(
    request = SavePlaceRequest("Garden", "Quiet garden", "Bees"),
    requestKey = creationRequestKey,
)
if (result is PlacesCreateResult.Success) {
    val placeId = result.response.id
    // Use placeId for future edits via places.v1.save(placeId, request).
}
dozy.close()
```

`sessionStorage` is a platform-provided `AuthSessionStorage`; persist its account
tokens in platform storage. `dozy.auth.v1` provides anonymous identity creation,
email-code requests and verification, refresh, and logout. `dozy.sessionManager`
persists successful login/refresh responses through that storage and exposes a
`StateFlow<SessionState>`. The client attaches bearer tokens to feature and asset requests for the configured API origin and
uses the refresh endpoint to retry after `401`; a rejected refresh clears the stored
session and emits `SessionState.Expired`. The default in-memory session storage suits
tests and short-lived clients. Pass platform storage to persist sessions across restarts.
Close `DozyClient` before the caller-owned `HttpClient`. Use `dozy.assetHttpClient`
for protected images.

Inject `client.place.PlacesClients` into consumers rather than the entire `DozyClient` when only
Places is needed. The container exposes singular version interfaces (`places.v1.list()`,
`places.v1.save(...)`) through a public interface. Internal implementations receive version clients through
constructors; tests provide interface stubs without constructing production implementations. Mobile registers the root client in Koin and exposes its Places
container to `DefaultPlaceRepository`.
Authenticated requests use the JWT subject for resource ownership.
List and create responses may include a place thumbnail `imageUrl`.
Fetch thumbnails with `dozy.assetHttpClient`.
The Places and Characters `create` methods submit fields without a resource ID
and return the backend-assigned UUIDv7. Supply a UUIDv4 as the optional
`requestKey` and reuse it for an uncertain retry with the same input.

`contracts.v1.ApiErrorCode` defines common request/server and reusable machine codes.
Operation error contracts define their known business cases and flat validation
codes. Shared serializers retain future codes as `Unknown(code)`.

### Places error handling

Places returns operation-specific sealed results `PlacesListResult`,
`PlacesCreateResult`, and `PlacesSaveResult`. Each exposes `Success`, `ApiError(error)` and
`TechnicalError(error)`, so the caller handles one exhaustive `when`. List success
contains `response: PlacesResponse`. Create success contains `response: CreatedPlaceResponse`. Save success has no payload and accepts every HTTP 2xx status.
Business errors use serializable `contracts.v1.place.PlacesListError`/`PlacesSaveError`.
Backend sends the shared error object and the client deserializes that exact type,
just like success DTOs. The contract serializer owns the flat code lookup and
`Unknown(code)` fallback; there is no client-side business-error mapper. Shared technical failures
live in `api.client.TechnicalError`, not the transport contracts.

- `ApiError(error)`: a decoded HTTP 4xx business failure. Save recognizes flat
  contract-owned `PlacesSaveError` values for required/too-long fields, forbidden
  and conflict. An unfamiliar code remains the operation's shared `Unknown(code)`.
- `TechnicalError(TechnicalError.Server(status))`: every HTTP 5xx, before reading or decoding its body,
  including HTML or an empty proxy response.
- `TechnicalError(TechnicalError.Communication(exception))`: transport/timeout/TLS failures, invalid request
  or malformed JSON/missing response fields, unexpected
  response status, disabled API configuration, or any other `Exception`, including
  local argument/programming errors. The exception is diagnostic,
  never user-facing text. A failure does not prove a write was not committed.

The backend uses 422 for validation, 403 for forbidden and 409 for conflict. The
body code determines the business detail; the client does not parse field/reason
combinations. Example: `{"code":"validation.name.too_long"}`. Envelope field/reason
properties remain nullable legacy metadata; business-error mapping needs only code. Every public Places operation catches
all `Exception` values except `CancellationException`, which propagates. Local checks
(such as a blank save ID) are inside this boundary. Fatal errors are not caught.
Consumers do not need try/catch around Places calls. Mobile retains cached/pending
data on failure.

### Shared error handling for all clients

Every operation exposes its own sealed result with `Success`, `ApiError(error)`
and `TechnicalError(error)`, following Places. Business error classes live in
`contracts.v1.<feature>` and implement `ApiBusinessError`. Backend serializes the
same objects the SDK decodes, using shared contract serializers. Validation wire
codes are flat (`validation.<field>.<reason>`); operation-specific `Validation`
values expose typed fields/reasons without client-side lookup or translation.
Future codes become the operation's `Unknown(code)`.

`ClientTransport` applies the same policy to every client: 5xx wins over body
content and becomes `TechnicalError.Server`; every other Exception becomes
`TechnicalError.Communication`. Cancellation propagates; fatal Errors are not
caught. Consumers need no try/catch around client calls. Payload-free writes
accept all 2xx; payload-returning operations also decode and validate success
bodies, so a missing/invalid body remains a communication failure.

Responses tolerate additive unknown JSON fields. Write serialization is owned by
the library and does not require caller ContentNegotiation. The SDK never closes
the caller's HttpClient or engine.

## Characters

Inject `client.character.CharactersClients` and call `characters.v1.list()`,
`save(id, SaveCharacterRequest(...))`, or `delete(id)`. List returns
`CharactersListResult` with a `Success.response: List<CharacterDto>` bare-array
payload. Save/delete return `CharactersSaveResult`/`CharactersDeleteResult` with
payload-free Success objects. Both accept every 2xx; delete also acknowledges the
known `CHARACTER_NOT_FOUND` business code. Unknown 404 codes remain ApiError.Unknown;
5xx never acknowledges deletion even if its body contains a not-found code.

The write request has name, personality, appearance, type and an optional avatar;
ID is supplied only in the path. A bearer access token identifies the owner.
`CharacterField.reasons` defines allowed validation combinations, including
SINGLE_LINE and INVALID_VALUE. Save also classifies CONFLICT and
REFERENCE_UNAVAILABLE. All failure categories follow the shared result policy above. See [character integration](../mobile/CHARACTER_DATA.md)
and [backend API](../backend/API.md) for limits, ownership and deletion semantics.

## Story generation and reading

Inject `client.storygeneration.StoryGenerationsClients` and `client.story.StoriesClients`.
Call `storyGenerations.v1.start(idempotencyKey, request)`, `.status(jobId)` and
`stories.v1.read(storyId)`. Every operation requires a bearer access token. Start also
sends the persisted UUIDv4 `Idempotency-Key`. The client owns serialization and
omits `place`/`idea`/`ending` in AUTOMATIC mode even if a local draft retains them.
Cast and place are complete immutable snapshots, with nullable character avatars.
`GenerationField.reasons` defines start validation combinations; mode, length,
stage and terminal failure values are defined beside the story DTOs. A READY status
includes the full immutable story in `response.story`, with the same job and story IDs.
The result can be saved without another HTTP request.

Start errors distinguish idempotency conflict, active job, unavailable reference,
validation and unknown errors. Status/read distinguish the known
`JOB_NOT_FOUND` / `STORY_NOT_FOUND` codes from other errors. Classification uses
body codes for 4xx; every 5xx is a technical server failure. Structural errors,
malformed terminal combinations and transport failures are communication results;
cancellation propagates.
A missing story never triggers generation. HTTP covers require an explicitly HTTP
API base URL and the same origin; otherwise the SDK requires HTTPS. Story cover
bytes are fetched with `dozy.assetHttpClient`, which adds bearer credentials on the
configured API origin.

## Versioning

HTTP versioning and library versioning are independent:

- `places.v1` is permanently bound to `ApiVersion.V1` and `/api/v1`. There is no
  global version selector in `DozyClientConfig`. Pass the server root as `baseUrl`, without
  `/api/v1`; an optional hosting path prefix is preserved. `/hello` is unversioned.
  Legacy unversioned feature routes are not retained during development.
- The initial library version is `0.1.0`. Local composite builds substitute matching
  Maven coordinates with these projects. Publishing artifacts is outside this change.
- Wire types live in `contracts.v1`. A future incompatible HTTP contract gets a new
  version and type package. Add only changed operations to `places.v2`; a mobile
  repository may use v1 list and v2 save. Backend routes live in `api.v1` with
  explicit `placeV1Routes`; a future version gets its own routing boundary.
  Preserve old versions once released clients depend on
  them. Sharing DTOs only proves the current source agrees; it does not replace
  compatibility tests or backward-compatibility review.

## Tests

From the repository root:

```sh
./backend/gradlew -p api --no-daemon :contracts:jvmTest :client:jvmTest
./test-all
```

Contracts tests assert the JSON shape. Client unit tests use Ktor MockEngine to
check versioned paths, bearer credentials, serialization, acknowledgements,
structured/non-JSON errors, disabled configuration, additive fields, cancellation,
and malformed payloads. Backend route tests retain raw HTTP/JSON coverage. These
are unit/component tests, not E2E.

## Story library listing (API4)

`stories.v1.list(ListStoriesRequest(limit = 100, cursor = null))` returns `StoryListResult`. Summaries contain id, title, immutable coverUrl, readingMinutes and createdAt. Reading time counts whitespace-delimited words, rounds up at 150 words/minute, and has minimum one. Limits are 1–100; `StoryListField.LIMIT` permits `ValidationReason.INVALID_VALUE`. Known business errors include InvalidCursor and Validation; future codes remain Unknown. Structural errors and malformed pages become TechnicalError.Communication. Pages are ordered by descending story ID. The client URL-encodes the cursor, which is the last returned story ID when more results are available. The final page returns nextCursor null.
