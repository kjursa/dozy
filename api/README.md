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
  `PlacesClient`, with its contracts dependency exposed to consumers. Engines,
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
val places = dozy.places.list()
dozy.places.save(
    id = placeId,
    request = SavePlaceRequest("Garden", "Quiet garden", "Bees"),
)
```

Inject `PlacesClient` into consumers rather than the entire `DozyClient` when only
Places is needed. Mobile registers the root client in Koin and exposes its Places
client to `DefaultPlaceRepository`. Android stores the UUID in installation
preferences; the iOS boundary uses `NSUserDefaults`. These IDs are a namespace,
not authentication credentials. Clearing application data can lose the namespace.

`list()` unwraps the collection response into `List<PlaceResponse>`. `save()` returns
`Unit` only for 201/204 acknowledgements. HTTP failures throw `DozyApiException`
with the HTTP status and a decoded error envelope when available; transport,
serialization, and coroutine cancellation failures propagate. A null base URL
throws `ApiDisabledException` before any request; mobile maps this to local-only
behavior. Responses tolerate additive unknown JSON fields. Write serialization is
owned by the library and does not require the caller to install ContentNegotiation.
The SDK never closes the caller's HttpClient or engine.

## Versioning

HTTP versioning and library versioning are independent:

- `ApiVersion.V1` targets `/api/v1`. Pass the server root as `baseUrl`, without
  `/api/v1`; an optional hosting path prefix is preserved. `/hello` is unversioned.
  Legacy unversioned feature routes are not retained during development.
- The initial library version is `0.1.0`. Local composite builds substitute matching
  Maven coordinates with these projects. Publishing artifacts is outside this change.
- Wire types live in `contracts.v1`. A future incompatible HTTP contract gets a new
  version and type package. Preserve old versions once released clients depend on
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
