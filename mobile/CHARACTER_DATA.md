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
  The private constructor of `CreateCharacterInput` and its `from()` factory enforce
  creation rules before the use case runs. Name is a single line of 1–50 trimmed `String.length` units;
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

Koin starts once in `DozyApplication`. `feature/character/di/CharacterModule.kt` declares the shared
repository, API, use cases, and character ViewModels with constructor
injection. Home and Story have their own feature DI modules. It has no Android imports. `di/AndroidModule.kt` supplies the Room
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

## Package layout

- `feature.character.domain.model`: character models, nested value types, and
  creation validation; `.repository`: the CharacterRepository contract;
  `.usecase`: SaveCharacter, ObserveCharacters, and their CharacterIdGenerator dependency.
- `feature.character.data`: repository implementation and entity/DTO mapping
  extensions; `database` contains Room database, DAO, and entity files; `api`
  contains the API contract, Ktor implementation, and DTO.
- `feature.character.presentation.create` and `.list`: creation form and character
  list, each with separate ViewModel, UI state, actions, effects, and mapping files.
- `feature.home.presentation` and `feature.story.presentation`: Home and Story
  screens and their presentation models; each feature declares its own DI module.
- `:foundation` / `com.kjursa.foundation.coroutines`: reusable coroutine helpers such as `runCatchingSuspend`.
- `:foundation` / `com.kjursa.foundation.presentation.text`: `UiText` and its `resolve()` extension.
- `core.presentation.components` and `.layout`: generic UI helpers with no feature
  dependencies. Story-specific cards remain in `feature.story.presentation.components`.
- `navigation`: the application bottom navigation shell.

The Room database class moved packages; its exported schema moved to the matching
qualified-name directory. Table definitions, schema version, and identity hash
are unchanged, so this package refactor does not require a database migration.

## Creation validation and field messages

`CreateCharacterInput.from()` returns either a valid creation request or typed
errors grouped by name, personality, appearance, and type, with one error per
field. Its constructor is private and it has no data-class copy method, so callers
cannot construct an unchecked creation request. `SaveCharacter` accepts only this
validated request.

Existing `Character.Details` values read from storage/API are separate from creation
requests. Their value classes wrap stored values without trimming or reapplying
current creation limits, so a legacy 55-character name can still be displayed.
Technical schema/serialization failures remain data-layer failures.

Presentation maps domain errors to `UiText.Resource` values in the four nullable
UI-state fields (`nameError`, `personalityError`, `appearanceError`, `typeError`).
`UiText.Plain` can carry an API message intended for users. The foundation library’s `resolve()`
Composable extension selects current localized resources and formatting arguments
or returns the plain text. Domain code contains no resource IDs or UI text.
Resource IDs currently use Android resources; switching the presentation to
Compose Multiplatform resources belongs to the future KMP migration.

Reusable technical APIs are collected in the single `:foundation` Android library
(formerly `:compose-ui-kit`): coroutine helpers, presentation text, Compose MVI,
spacing, and the theme entry point. The app retains its feature code, visual
branding, and screen-specific UI helpers. Coroutine tests live in the foundation
module; app tests exercise the library through the feature integrations.

Character value types are nested in the domain entity: `Character.Id`, `.Name`,
`.Personality`, `.Appearance`, `.Type`, and `.Details`. They can be used without
an enclosing Character instance. Existing records use `Character.Details`; the
creation-only validation remains in `CreateCharacterInput`. Operation and contract
names such as `SaveCharacter` and `CharacterRepository` stay explicit and live in
separate files.

## Form restoration

The form state uses Kotlin serialization. All raw input fields are saved under one
`STATE` key through the optional foundation persistence hook in `MviViewModel`.
Koin supplies `SavedStatePersistence` with the screen's SavedStateHandle and state
serializer. The ViewModel no longer writes fields to individual keys or depends on
SavedStateHandle directly. New serializable state fields are included automatically.

Field errors and saving/failure status are transient and restore to null/EDITING,
so a system-recreated form retains its input without stale errors or stuck progress.
This restores the form input, not an in-flight save operation. Unit tests recreate
ViewModels from the serialized snapshot; device process-death checks have not been run.
