# Character data and API1 integration

Characters use the existing screens, domain models, validated creation/edit inputs,
Room DAO and repository. CRUD now uses `DozyClient.characters.v1` through an injected
`CharactersClients` container. Wire models (`CharacterDto`, `AvatarDto`,
`SaveCharacterRequest`) live in `api/contracts`; domain types remain independent.

## Installation-scoped CRUD

Every request sends the persisted `X-Installation-Id` UUID. This supplies an
installation namespace without accounts or authentication.

- `GET /api/v1/characters` returns a complete bare array of non-deleted owned
  characters. An empty array is authoritative. Historical field values are read
  without applying current write limits.
- `PUT /api/v1/characters/{id}` accepts name, personality, appearance, type and an
  optional nullable avatar. The UUIDv7 ID is supplied only in the path. Android
  injects the common `UuidCharacterIdGenerator`, using Kotlin `Uuid.generateV7`;
  updates and retries retain the original ID. Success is 201 creation or 204
  replacement, both with empty bodies. Omitted/null avatars clear the reference.
- PUT trims required names (single line, at most 50 UTF-16 units), personality and
  appearance (at most 2000 each). Types are CHILD, ANIMAL, FANTASY and OTHER.
  Contract-owned `CharacterField` and `ValidationReason` specify field errors.
  Unknown types are `VALIDATION_FAILED` with type / INVALID_VALUE.
- Avatar ID and URL must exactly match an owned READY entry in the backend asset
  registry; otherwise PUT returns `REFERENCE_UNAVAILABLE`. Nullable avatars support
  historical records; new mobile forms still require an avatar.
- An inaccessible or permanently deleted PUT ID returns 409 `CONFLICT` and can
  never be claimed or revived. DELETE returns 204 for owned live/already-deleted
  rows, or 404 `CHARACTER_NOT_FOUND` for missing/inaccessible IDs. The client treats
  that known code as an idempotent acknowledgement; arbitrary 404s are failures.

All shared operations return typed `ApiResult` values. Structural errors,
malformed responses, transport failures and cancellation remain exceptions.
Known error codes are localized or mapped to generic UI states; backend diagnostic
text is never displayed.

## Local-first synchronization

Saves and edits persist pending rows before HTTP. Only acknowledged writes become
synced. Failed writes remain durable and pending. Repository mutations, refreshes
and acknowledgements are serialized so a late response cannot overwrite a newer
edit or resurrect a deleted row. Missing/deleted rows cannot be edited or revived.
Storage failures before a write prevent HTTP; acknowledgement-metadata failures
still report the durable save as pending. Cancellation propagates.

Deletion first writes a durable local tombstone, then attempts shared DELETE.
Network failures leave the deletion pending and hidden. Before explicit refresh,
tombstone DELETEs and pending live PUTs are retried. Failed writes remain protected
from the collection snapshot. There is no new background upload worker.

Snapshots are decoded and mapped fully before one transactional replacement:
pending local rows and tombstones win, synced absent rows disappear, and remote
rows replace synced records. Duplicate IDs or malformed transport data reject the
entire snapshot. Tombstones remain even after acknowledgement, across database
reopening, and are never uploaded as PUTs. They protect against stale snapshots.
Room schema v8 and existing migrations are unchanged.

## Avatar generation arrives in API2

The existing `CharacterApi` / `KtorCharacterApi` boundary now handles only avatar
submission and polling; it is retained for API2. This task adds no backend avatar
generation route or provider. API2 will populate the same asset metadata registry.
Only test fixtures seed owned READY assets today.

With the API disabled, `FakeAvatarGenerator` provides bundled previews. References
with `fake:` IDs or `bundled-avatar:` URLs stay local-only and are never uploaded,
including after enabling the server and refreshing. They remain pending. Real
remote avatar generation is required before new characters can use server assets.
The existing new-character form still requires a successful avatar; legacy reads
can omit it. Form restoration, editing, regeneration and navigation remain in the
existing flows; the retained HTTP generator does not make those endpoints available
on the current backend.
