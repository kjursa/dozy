# Character data and API1 integration

Characters use the existing screens, domain models, validated creation/edit inputs,
Room DAO and repository. CRUD now uses `DozyClient.characters.v1` through an injected
`CharactersClients` container. Wire models (`CharacterDto`, `AvatarDto`,
`SaveCharacterRequest`) live in `api/contracts`; domain types remain independent.

## Owner-scoped CRUD

The API client attaches a bearer token to authenticated requests. The backend
uses the token subject as the resource owner.

- `GET /api/v1/characters` returns a complete bare array of non-deleted owned
  characters. An empty array is authoritative. Historical field values are read
  without applying current write limits.
- `POST /api/v1/characters` accepts name, personality, appearance, type and an
  optional avatar. The backend generates a UUIDv7 and returns it in the 201
  response. Mobile persists a UUIDv4 request key with the draft and reuses it
  when retrying the same creation.
- `PUT /api/v1/characters/{id}` replaces an existing character and returns an
  empty 204. Omitted/null avatars clear the reference. Updates retain the
  original ID.
- Both writes trim required names (single line, at most 50 UTF-16 units), personality and
  appearance (at most 2000 each). Types are CHILD, ANIMAL, FANTASY and OTHER.
  Contract-owned `CharacterField` and `ValidationReason` specify field errors.
  Unknown types return the shared validation error with code `validation.type.invalid_value`.
- Avatar ID and URL must exactly match an owned READY entry in the backend asset
  registry; otherwise the write returns `REFERENCE_UNAVAILABLE`. Nullable avatars support
  historical records; new mobile forms still require an avatar.
- An inaccessible or permanently deleted PUT ID returns 409 `CONFLICT` and can
  never be claimed or revived. DELETE returns 204 for owned live/already-deleted
  rows, or 404 `CHARACTER_NOT_FOUND` for missing/inaccessible IDs. The client treats
  that known code as an idempotent acknowledgement; arbitrary 404s are failures.

All SDK operations return operation-specific Success/ApiError/TechnicalError
results. Error objects are shared serializable contracts; validation uses flat
wire codes with typed field/reason values. HTTP 5xx becomes TechnicalError.Server;
all other Exceptions become TechnicalError.Communication. Cancellation propagates.
Known error codes are localized or mapped to generic UI states; backend diagnostic
text is never displayed.

## Local drafts and existing-record updates

Mobile stores one creation draft per form type in Room. A draft remains local
until the user submits it and receives a server ID. The form restores the latest
draft when reopened. New characters and places enter the resource cache after
the server acknowledges creation. Existing edits persist pending rows before HTTP. Only acknowledged writes become
synced. Failed writes remain durable and pending. Repository mutations, refreshes
and acknowledgements are serialized so a late response cannot overwrite a newer
edit or resurrect a deleted row. Missing/deleted rows cannot be edited or revived.
Storage failures before a write prevent HTTP; acknowledgement-metadata failures
still report the durable save as pending. Cancellation propagates.

Deletion first writes a durable local tombstone, then attempts shared DELETE.
Network failures leave the deletion pending and hidden. Before explicit refresh,
tombstone DELETEs and pending live PUTs are retried. Failed writes remain protected
from the collection snapshot. There is no new background upload worker.

Refresh sends the last stored ETag in `If-None-Match`. HTTP 304 keeps the cache.
Full snapshots are decoded and mapped before one transactional replacement that
also stores the new ETag.
Pending local rows and tombstones win, synced absent rows disappear, and remote
rows replace synced records. Duplicate IDs or malformed transport data reject the
entire snapshot. Tombstones remain even after acknowledgement, across database
reopening, and are never uploaded as PUTs. They protect against stale snapshots.
Room stores creation drafts in a dedicated table.

## API2 avatar generation

The repository directly injects `CharacterAvatarsClients`, selects `.v1`, and
performs the bounded observation loop: at most 120 one-second GETs after one POST.
Each SDK method performs one HTTP operation. The legacy `CharacterApi` and its
transport DTOs have been removed. Cancellation propagates through delays and HTTP;
leaving the form stops observation without cancelling server work or saving.

Generate Avatar is explicit, blocks form actions and shows progress. Initial Save
requires a generated avatar. Later text edits retain it; Save can use the current
avatar without regeneration. Failures store no character. Existing form tests
cover these behaviors. Enabled API uses backend jobs/assets; disabled API uses
bundled local fakes. `fake:`/`bundled-avatar:` references are never uploaded.

HTTPS asset URLs are the default. Explicit debug local mode also accepts HTTP
loopback/emulator hosts, with a matching debug-only Android network policy.
Release defaults remain HTTPS. See root README for local backend and app commands.
