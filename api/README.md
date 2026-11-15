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
  `PlacesClients` and `CharactersClients` version containers, with its contracts dependency exposed to consumers. Engines,
  authentication/installation identity storage, and the HTTP client lifetime belong
  to the caller.

Places and Characters are implemented in the shared contracts/client and backend.
Story DTOs remain in mobile until their backend API is implemented. Character DTOs
and avatar references are shared; avatar job request/poll DTOs remain at the mobile
generator boundary until API2. Remote avatar generation arrives in API2; this task
provides only the asset metadata registry and reference checks.

## Usage

```kotlin
val dozy = DozyClient(
    httpClient = httpClient,
    config = DozyClientConfig(baseUrl = "http://localhost:8080"),
    installationIdProvider = InstallationIdProvider { persistedInstallationUuid },
)
val places = dozy.places.v1.list()
dozy.places.v1.save(
    id = placeId,
    request = SavePlaceRequest("Garden", "Quiet garden", "Bees"),
)
```

Inject `client.place.PlacesClients` into consumers rather than the entire `DozyClient` when only
Places is needed. The container exposes singular version interfaces (`places.v1.list()`,
`places.v1.save(...)`) through a public interface. Internal implementations receive version clients through
constructors; tests provide interface stubs without constructing production implementations. Mobile registers the root client in Koin and exposes its Places
container to `DefaultPlaceRepository`. Android stores the UUID in installation
preferences; the iOS boundary uses `NSUserDefaults`. These IDs are a namespace,
not authentication credentials. Clearing application data can lose the namespace.

`contracts.v1.ApiErrorCode` is the shared source of known wire codes for backend
and client. Error envelopes retain a string code; `fromValue` returns null for
unrecognized values so future codes can still become `Unknown`.

For HTTP error responses, `body.code` determines the error meaning independently
of the HTTP status. Status is used for success validation and retained in diagnostic
exceptions/unknown errors. The backend emits 400 for invalid request structure and
422 with `VALIDATION_FAILED` for invalid user-entered values.

`PlaceField` and `ValidationReason` likewise define validation wire values.
`PlaceField.reasons` declares the allowed combinations. The backend emits these
contract values; the client parses them with `fromValue` and returns them directly
in typed validation failures, without maintaining duplicate field/reason enums.
Envelope fields remain strings to preserve unknown future values.

Each operation returns `ApiResult<SuccessPayload, OperationError>`. V1 list returns
`ApiResult<PlacesResponse, PlacesListError>`; save returns
`ApiResult<Unit, PlacesSaveError>`. Handle `Success` and `Failure` exhaustively.
Save has typed validation (field/reason), forbidden, conflict, unavailable and
unknown errors. List has unavailable and unknown errors. `Unknown` preserves the
HTTP status and the original error envelope for a new code, field, reason or
unrecognized operation error code, without requiring a new SDK release.

Structural `INVALID_REQUEST` and `INVALID_INSTALLATION_ID` response codes throw
`InvalidRequestException`, outside the result. `VALIDATION_FAILED` with a known
field/reason is a validation failure value. Unknown validation fields/reasons
become `Unknown`; missing required validation metadata throws
`InvalidResponseException`. Invalid JSON or missing required response fields
throw serialization exceptions. Unexpected success statuses also throw
`InvalidResponseException`. Transport failures (including timeout, DNS and TLS)
and coroutine cancellation propagate; they are never converted into ordinary API
failures. A null base URL throws `ApiDisabledException` before sending a request.
Mobile maps failures to local-only/pending behavior without acknowledging a write.

Responses tolerate additive unknown JSON fields. Write serialization is owned by
the library and does not require caller ContentNegotiation. The SDK never closes
the caller's HttpClient or engine.

## Characters

Inject `client.character.CharactersClients` and call `characters.v1.list()`,
`save(id, SaveCharacterRequest(...))`, or `delete(id)`. List returns
`ApiResult<List<CharacterDto>, CharactersListError>` with the existing bare-array
wire shape. Save/delete return `ApiResult<Unit, CharactersSaveError>` and
`ApiResult<Unit, CharactersDeleteError>`; acknowledgements are explicit success
values. Save accepts only 201/204, delete accepts 204 and the known
`CHARACTER_NOT_FOUND` error code. Unknown 404 codes remain `Unknown`.

The write request has name, personality, appearance, type and an optional avatar;
ID is supplied only in the path. All operations send `X-Installation-Id`.
`CharacterField.reasons` defines allowed validation combinations, including
SINGLE_LINE and INVALID_VALUE. Save also classifies CONFLICT and
REFERENCE_UNAVAILABLE. Structural errors and malformed/transport responses follow
the exception policy above. See [character integration](../mobile/CHARACTER_DATA.md)
and [backend API](../backend/API.md) for limits, ownership and deletion semantics.

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
check versioned paths, installation headers, serialization, acknowledgements,
structured/non-JSON errors, disabled configuration, additive fields, cancellation,
and malformed payloads. Backend route tests retain raw HTTP/JSON coverage. These
are unit/component tests, not E2E.
