# dozy

Dozy is a personalized bedtime-story app, primarily operated by a parent who prepares an adventure and reads it to their child. Reusable characters—children, animals, or imaginary heroes—and familiar or fantastical places are the building blocks for new stories.

This repository contains both parts of the project:

- `mobile/`: an Android app built with Kotlin and Jetpack Compose, with boundaries intended to support a future Kotlin Multiplatform (KMP) migration. There is no implemented iOS client yet.
- `backend/`: the Kotlin and Ktor backend.

## Mobile

The Android app supports saved characters and places, manual or automatic story submission, durable progress, and an immutable reader. Configure the local fake backend to generate deterministic text and one cover image. Requests retain their snapshots and idempotency key across uncertain responses and restarts. Home and the local library expose completed stories; Android WorkManager reconciles jobs and posts completion notifications when permission is granted.

| Screen | Implementation status |
| --- | --- |
| Home / My Stories | Persisted local stories and jobs; remote paginated listing is reserved for API4. |
| Characters / Places | Local-first collections and forms using shared SDK clients. Remote avatar generation is reserved for API2; cast snapshots may have no avatar. |
| Story creation | Saved cast, optional manual setting/idea/ending, automatic mode, SHORT/LONG lengths. |
| Generation progress | Backend stages, leave/reopen, status recovery and completion. Explicit server retry is reserved for API5. |
| Reader | Immutable cover and text, reading settings, and a new independent adventure with the same heroes. |

Server execution continues after leaving the screen. Android background observation and notifications retain their existing WorkManager behavior; iOS has no background scheduler or server push.

See [mobile/README.md](mobile/README.md) for running and building the Android app, and [mobile/SPEC.MD](mobile/SPEC.MD) for detailed intended behavior, screen variants, and open decisions. Open the [HTML mockup catalog](mobile/design/screens/index.html) locally in a browser to explore the designs. These mockups simulate interactions with example content; they do not implement generation, notifications, or durable app storage.

## Backend

The backend is a Ktor 3.6.0 application using constructor injection, Ktor DI, and Exposed SQL DSL over SQLite. Run it from `backend/` with `./gradlew run` (JDK 21). It implements `GET /hello`, `GET /api/v1/places`, and `PUT /api/v1/places/{id}`, plus installation-scoped character GET/PUT/DELETE. It also implements story submission/status, immutable story reads, and installation-scoped PNG cover delivery. Remote library listing and explicit retry remain proposals for API4/API5 in [the API reference](backend/API.md).

Settings are loaded from `backend/src/main/resources/application.yaml` into `AppConfig`. Environment overrides:

| Variable | Default | Purpose |
| --- | --- | --- |
| `HOST` | `0.0.0.0` | Listen address |
| `PORT` | `8080` | Listen port |
| `DOZY_DATABASE_PATH` | `data/dozy.db` | Persistent SQLite file, relative to the server working directory |
| `DOZY_DATABASE_BUSY_TIMEOUT_MS` | `5000` | SQLite lock wait, from 1 to 60000 ms |
| `DOZY_STORY_TEXT_MODE` | `SUCCESS` | `SUCCESS`, `FAILURE`, or `CONTENT_REJECTED` |
| `DOZY_STORY_COVER_MODE` | `SUCCESS` | `SUCCESS` or `FAILURE` |
| `DOZY_STORY_DELAY_MS` | `250` | Delay per fake generator in milliseconds |
| `DOZY_STORY_CONCURRENCY` | `2` | Worker concurrency, 1–32 |
| `DOZY_ASSET_BASE_URL` | `https://localhost:8080` | Stable externally reachable backend origin, including optional hosting prefix |
| `DOZY_ASSET_ALLOW_HTTP` | `false` | Explicit development HTTP opt-in |

The database and its parent directory are created automatically. Flyway runs versioned migrations from `db/migration` at startup and tracks their checksums in `flyway_schema_history`. Version 1 creates installation/place tables and seeds three shared predefined places. Version 2 adds characters, permanent deletion reservations and the shared asset metadata registry. Version 3 adds durable story jobs, immutable results and local cover bytes. Local database files are ignored by Git when running from `backend/`.

Place requests require a canonical UUID in `X-Installation-Id`. Custom places are private to that installation; predefined places are read-only. This is the initial installation-based contract, without accounts or authentication. The shared Places client sends this header automatically using a persisted installation ID. See [API.md](backend/API.md) for bodies and errors, and [DB.md](backend/DB.md) for the SQLite implementation and future PostgreSQL design.

```sh
curl http://localhost:8080/api/v1/places \
  -H 'X-Installation-Id: 60c55c02-8d8b-46c2-a0cb-9a2d241eeea0'
```

Run backend and mobile tests from the repository root with `./test-all` (uses each project's Gradle wrapper).

## Shared API

`api/` contains an independent Kotlin Multiplatform build included by both backend
and mobile through Gradle composite builds. `api/contracts` owns the v1 Places
and Characters request/response DTOs, story snapshots/jobs/results and shared errors; `api/client` provides
`DozyClient.places.v1`, `DozyClient.characters.v1`, `DozyClient.storyGenerations.v1` and `DozyClient.stories.v1` with injectable plural feature containers. The client has JVM/Android-compatible and iOS
targets and does not depend on mobile domain models, Room, Compose, or Koin.
See [API library setup and versioning](api/README.md).

Characters use client-generated UUIDv7 IDs and durable local-first pending writes
and deletion tombstones. Server avatar references must match owned READY asset
metadata. Remote avatar generation arrives in API2; no generator or provider is
implemented by API1, and bundled development avatars are never uploaded.

For a local HTTP backend, run from `backend/`:

```sh
DOZY_ASSET_BASE_URL=http://10.0.2.2:8080 DOZY_ASSET_ALLOW_HTTP=true ./gradlew --no-daemon run
```

Set the Android API base URL to `http://10.0.2.2:8080` (the emulator host bridge).
Use a matching reachable origin for other devices. Images are generated from a checked-in PNG fixture and stored in SQLite; no image bytes are fetched from third parties. Production-style URLs default to HTTPS. API and assets must use the same externally reachable origin so the mobile image client can attach the installation header safely.

Build the Android debug app against that backend with
`./mobile/gradlew -p mobile --no-daemon :app:assembleDebug -Pdozy.apiBaseUrl=http://10.0.2.2:8080`.
Without this property, remote APIs remain disabled. Debug builds allow cleartext
for local development; release builds retain Android's HTTPS-only default.
