# Character persistence and API boundary

The inspected backend (`backend/src/main/kotlin/com/dozy/Application.kt`) exposes
only `GET /hello`. There is no character database model, endpoint, authentication,
or character validation contract to mirror. This change does not add backend routes.

Characters are stored in a versioned Room 3 database (`characters.db`), with string
identity, name, personality, appearance, type, and a local `pendingSync` flag. Room
3.0.3 compiles with the existing Kotlin 2.2.20 / AGP 8.13.2 setup using KSP
2.2.20-2.0.4. The DAO uses Flow and suspend operations; the Android construction
boundary supplies a bundled SQLite driver. The exported v1 schema is checked in.
The domain and data operations use Kotlin types and coroutine APIs. Android-specific
construction (including the Ktor Android engine) lives in `di/AndroidModule.kt`.

## Save outcomes

- Validate required name, personality, appearance and type before persistence.
  Private value-class constructors and `from()` factories enforce these rules before
  the use case runs. Name is a single line of 1–50 trimmed `String.length` units;
  personality and appearance are required and limited to 2000 trimmed units each.
  These are local product rules until a backend contract is agreed.
- Generate an identity and write a pending row before attempting HTTP. A failed
  local write reports failure and does not call the API.
- A remote acknowledgement marks the row synced. Network errors, missing routes,
  HTTP errors, or an unconfigured API leave the durable row pending and return a
  successful local save. Failure to update sync metadata also preserves local success.
- Cancellation propagates. Any row already written remains pending.
- The form validates before calling the save use case, replaces Save with a progress
  indicator while saving, and closes immediately after a successful local save.
  A failed save restores the button and shows a retryable error. Home and Characters
  observe the database, including pending rows.
- No automatic sync worker is included. Pending rows remain on the device until a
  future sync feature implements the agreed backend contract.

## Proposed API (disabled until implemented by the backend)

`KtorCharacterApi` accepts an injected `HttpClient` and nullable base URL. The app
passes `null`, so no unsupported request is sent. Its tested provisional operation
is idempotent `PUT /characters/{id}` with JSON containing `id`, `name`, `personality`,
`appearance`, and uppercase `type` (`CHILD`, `ANIMAL`, `FANTASY`, `OTHER`). A 2xx
acknowledges persistence; no response DTO is assumed. This shape is a proposal,
not an existing backend contract. Before enabling it, agree endpoint semantics,
authentication, validation, and synchronization/retry behavior with the backend.
No remote list endpoint is assumed: both views load the durable local collection.

Artwork remains a presentation placeholder because the backend has no image contract.

## Dependency injection and KMP migration

Koin starts once in `DozyApplication`. `di/CharacterModule.kt` declares the shared
repository, API, use cases, and lifecycle-managed ViewModels with constructor
injection. It has no Android imports. `di/AndroidModule.kt` supplies the Room
construction/DAO, HTTP engine, and ID generator; a future iOS module will supply
the same dependency types. Domain and data collaborators do not resolve Koin
services themselves. Composable routes use `koinViewModel()` so each navigation
entry owns its ViewModel and supplies creation extras for `SavedStateHandle`.
The existing constructor-based unit tests remain independent of Koin; the DI
integration test replaces platform dependencies and checks owner-scoped ViewModel
reuse, form state, saving, and reactive list updates.

Koin 4.1.1 is pinned for the current Kotlin 2.2.20 toolchain. The multiplatform
`koin-compose-viewmodel` API can move with the shared graph; only Android startup
and platform construction use `koin-android`. A future KMP migration will also
need to move Android presentation resources out of any shared ViewModels.
