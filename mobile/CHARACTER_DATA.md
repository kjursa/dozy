# Character persistence and API boundary

The inspected backend (`backend/src/main/kotlin/com/dozy/Application.kt`) exposes
only `GET /hello`. There is no character database model, endpoint, authentication,
or character validation contract to mirror. This change does not add backend routes.

Characters are stored in a shared Room 3 `DozyDatabase` (`dozy.db`), with string
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
Both views observe the durable local collection; the list can refresh it using the F3 proposal below.

Generated avatar references use the F2 proposal documented below; an unconfigured backend cannot generate a preview.

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
reuse, form state, saving, and reactive list updates. A Robolectric integration
test also exercises the production database/DAO bindings with a real Room file,
checks every character field after reopening, and verifies Koin closes the database.
The SQLite driver is injected at the Android boundary: production uses the bundled
driver, while Robolectric supplies its native framework SQLite driver for JVM tests.

Koin 4.1.1 is pinned for the current Kotlin 2.2.20 toolchain. The multiplatform
`koin-compose-viewmodel` API can move with the shared graph; only Android startup
and platform construction use `koin-android`. A future KMP migration will also
need to move Android presentation resources out of any shared ViewModels.

## Package layout

- `feature.character.domain.model`: character models, nested value types, and
  creation validation; `.repository`: the CharacterRepository contract;
  `.usecase`: SaveCharacter, ObserveCharacters, and their CharacterIdGenerator dependency.
- `feature.character.data`: repository implementation and entity/DTO mapping
  extensions; `database` contains the feature-owned DAO and entity files; `api`
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

The application-wide Room definition lives in `core.data.database.DozyDatabase`;
its exported v1 schema lives in the matching qualified-name directory. Character and place tables are registered. Android DI opens one `dozy.db` instance,
provides its character DAO, and closes the database when Koin closes. Schema v3 adds avatar references through an automatic migration. By product decision,
there is no migration or copying from the former `characters.db` file: the new
database starts empty. Existing repository and API contracts remain unchanged.

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

## F2 avatar contract and lifecycle

The following proposal extends the character save contract; it does not imply any
implemented backend/generator routes. `ApiConfig.baseUrl = null` disables all
requests. Generation then returns `Unavailable`, without creating an asset,
character, or success preview. A previously obtained avatar can still be saved
locally while the network is unavailable.

- `POST /character-avatars`: JSON `{ "type": "CHILD", "personality": "Curious",
  "appearance": "Green hat" }`. Uses the current validated input, without photos.
  `202` returns `{ "jobId": "job-123", "status": "QUEUED", "asset": null,
  "error": null }`. Job IDs are opaque nonempty ASCII letters, digits, `_`, or `-`.
- `GET /character-avatars/{jobId}`: `200` returns the same job envelope. Status is
  `QUEUED`, `RUNNING`, `READY`, or `FAILED`. `READY` requires
  `asset: { "id": "asset-123", "url": "https://assets.example/asset-123.png" }`.
  Asset IDs are opaque, nonempty strings; URLs must be stable HTTPS references.
  The backend must retain saved assets and make their URLs readable by the image
  client without additional API authentication. `FAILED` may include a machine
  error code such as `GENERATION_FAILED`; raw error text is never displayed.
- Non-success HTTP responses (including 400 validation, 404 missing job, 429 rate
  limiting, and 5xx), malformed responses, unknown statuses, and a mismatched job
  ID expose a retryable generation failure. The client polls at one-second
  intervals for at most 120 status reads; timeout is a failure to observe readiness.
  It does not imply server cancellation. Client coroutine cancellation propagates.
- There are no automatic POST retries. Explicit Generate/Regenerate starts a new
  independent job; a failed observation may leave a server job running. A future
  server idempotency extension can avoid duplicate asset costs. No job ever saves
  a character. Abandoned job results are ignored, even for noncooperative responses.
- `PUT /characters/{id}` retains the existing local-first, idempotent save proposal
  and adds nullable `avatar: { "id": "asset-123", "url": "https://..." }` to the
  existing DTO. New app saves require a ready avatar; historical records may omit
  it. The server must upsert the same client-generated character ID, acknowledge
  only durable writes with 2xx, validate asset references, and preserve asset IDs.
  Repeating an identical PUT must not create duplicate characters. Editing reuses
  the existing character ID. No automatic sync worker or conflict resolution is
  included; a failed network save leaves the durable row pending as before.

Room schema v3 adds nullable `avatarId`/`avatarUrl` to the existing character table
through an automatic v2→v3 migration. Legacy rows remain readable without an
avatar and without reapplying current creation restrictions. No extra database
is introduced. List, Home, details, and story hero selection use real saved rows.

The form persists raw text, type, editing ID, and the successful asset reference.
Errors, loading, and active attempts are transient. Restoring a form never resumes
generation or saves it automatically. Fields retain the successful preview after
any edit. Save uses the current validated fields and current asset. Regeneration
blocks fields and Save, then replaces the preview only on success; failure retains
any earlier preview. Leaving cancels polling and invalidates the active attempt.
Home/list creation opens the saved details; story creation returns to the same
navigation entry and adds the saved ID once without replacing other draft input.

## F3 collection refresh contract

`GET /characters` has no request body and returns `200` with a JSON array of the
same `CharacterDto` objects used by F2 PUT, including optional avatar references.
IDs are stable client-generated opaque strings. Duplicate/blank IDs, blank names,
unknown types, malformed assets, decoding errors, and non-200 responses reject the
entire snapshot before any local replacement. Existing description/name values are
not revalidated against current creation restrictions.

Pagination is deliberately absent for the initial small personal character library.
The array must be a complete, consistent snapshot of all characters visible to the
current user; `[]` is authoritative. A future paginated contract must accumulate a
complete snapshot before applying deletions. Authentication remains to be agreed
before enabling the API. `401`/`403`, `429`, and `5xx` all surface a retryable generic
failure without displaying server text or deleting known rows. No automatic HTTP
retry is performed; Refresh/Retry explicitly repeats this read-only operation.

The DAO applies the snapshot in one transaction: preserve every pending local row,
remove synced rows, then insert remote rows whose IDs do not have pending local
changes. Thus absent synced records represent remote deletion, but pending new
characters and edits always win, including saves made while GET was in flight.
A failed request leaves all known data intact. There is no upload worker, local
delete operation, or tombstone protocol in F3; pending uploads remain as described
above. A future local delete feature must define tombstones before syncing them.

The list initially shows loading until the first local observation. Empty copy
appears only after loading/refresh completes successfully or in local-only mode.
Refresh retains visible cards and disables repeated refresh clicks. Failures retain
known cards and expose Retry; a null base URL sends no requests and shows an offline
notice alongside locally saved characters or the empty CTA. Cards display saved
avatars, names, and personality descriptions (at most two visible lines). Room
emissions update creation, editing, and collection membership without resetting the
whole collection. Existing navigation opens details or an empty form and preserves
bottom-tab owners and scroll state. No list/form input persistence is added here.

## F4 details and deletion contract

Details observes `Character.Id` directly through the shared repository and DAO.
A missing/deleted ID is unavailable; a storage read failure is a distinct retryable
state. Retry resubscribes to storage. The existing F3 `GET /characters` remains the
network read contract: details does not require a separate GET request. A future
`GET /characters/{id}` may return `200` with the same `CharacterDto`, or `404` for
an unavailable ID, with no request body; it is not used by this client. Keeping the
complete collection read avoids introducing a second snapshot consistency policy.

`DELETE /characters/{id}` uses the same stable opaque ID as PUT, encoded as one
path segment, with no request or response DTO. `204` acknowledges durable removal;
`404` also acknowledges success, so retrying a deletion is idempotent. All other
statuses (including `401`, `403`, `409`, `429`, and `5xx`) and transport failures
produce a generic retryable failure; server bodies are not user-facing. There is
no automatic HTTP retry or implicit authentication change. The backend must never
reuse a deleted ID and must not cascade deletion into saved stories or their
embedded cast/text/assets. Backend implementation remains outside this change.

With a configured backend, DELETE must be acknowledged before local deletion;
HTTP failure retains the visible character and confirmation dialog. If local
persistence then fails, the UI reports failure; retrying DELETE (possibly receiving
404) safely completes the local change. Cancellation propagates. Without a
configured backend, no request is sent: confirmation means a durable local
pending deletion, as explained in the confirmation copy, not server success.

Room schema v5 adds `deleted = 0` via an automatic v4→v5 migration. Confirmation
sets only the selected row's `deleted` and `pendingSync` flags. Both collection and
ID observation hide tombstones. Snapshot replacement preserves pending rows and
never overwrites their IDs. Tombstones survive database reopening and remote
acknowledgement, protecting against stale snapshots; they are never uploaded as
PUTs. Before an explicit collection refresh, the repository retries each tombstone
DELETE. Failure stops refresh and retains all local data. No background worker is
included. Tombstones intentionally remain durable until a future versioned sync
contract can safely garbage-collect them.

Details uses actions/state/effects: Cancel/dismiss/Back in the dialog changes no
data; confirmation disables dismissal and all navigation until completion. Repeated
confirmation and navigation clicks are guarded. Delete failure allows retry or
keep; successful deletion returns to the existing main entry and selects its
Characters tab with current rows. Ordinary Back returns to the entry context;
edit returns to the same details owner, and story creation selects this domain ID.
Only the navigation ID needs restoration; dialog, progress, errors, and loaded data
are transient and reconstructed rather than persisted or replayed.

A new adventure keeps only still-available character IDs from its previous cast;
missing/deleted heroes are dropped when the current library is observed. Remaining
heroes and other draft input are retained; an empty cast requires selecting a hero.
Saved stories and their previous cast remain unchanged. The current saved-story
reader is still a prototype, so this change touches no story persistence.
