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
  `PlacesClients` version container, with its contracts dependency exposed to consumers. Engines,
  authentication/installation identity storage, and the HTTP client lifetime belong
  to the caller.

Places is the first extracted feature because it is the only currently implemented
backend feature. Character/story clients and DTOs remain in mobile for now, using
the same v1 path convention. Move them into these modules as their backend APIs
are implemented. Do not introduce mobile domain types into the contract modules.
Mobile's Place DTO typealiases refer to the shared definitions; they do not copy
or maintain a second wire schema. The backend imports the same definitions directly.

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
`places.v1.save(...)`) and accepts them through its constructor, including test fakes. Mobile registers the root client in Koin and exposes its Places
container to `DefaultPlaceRepository`. Android stores the UUID in installation
preferences; the iOS boundary uses `NSUserDefaults`. These IDs are a namespace,
not authentication credentials. Clearing application data can lose the namespace.

Each operation returns `ApiResult<SuccessPayload, OperationError>`. V1 list returns
`ApiResult<PlacesResponse, PlacesListError>`; save returns
`ApiResult<Unit, PlacesSaveError>`. Handle `Success` and `Failure` exhaustively.
Save has typed validation (field/reason), forbidden, conflict, unavailable and
unknown errors. List has unavailable and unknown errors. `Unknown` preserves the
HTTP status and the original error envelope for a new code, field, reason or
unrecognized status/code combination, without requiring a new SDK release.

Structural `400 INVALID_REQUEST` and `400 INVALID_INSTALLATION_ID` responses throw
`InvalidRequestException`, outside the result. `422 INVALID_REQUEST` with a known
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
./backend/gradlew -p api :contracts:jvmTest :client:jvmTest
./test-all
```

Contracts tests assert the JSON shape. Client unit tests use Ktor MockEngine to
check versioned paths, installation headers, serialization, acknowledgements,
structured/non-JSON errors, disabled configuration, additive fields, cancellation,
and malformed payloads. Backend route tests retain raw HTTP/JSON coverage. These
are unit/component tests, not E2E.
