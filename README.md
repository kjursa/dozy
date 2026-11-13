# dozy

Dozy is a personalized bedtime-story app, primarily operated by a parent who prepares an adventure and reads it to their child. Reusable characters—children, animals, or imaginary heroes—and familiar or fantastical places are the building blocks for new stories.

This repository contains both parts of the project:

- `mobile/`: an Android app built with Kotlin and Jetpack Compose, with boundaries intended to support a future Kotlin Multiplatform (KMP) migration. There is no implemented iOS client yet.
- `backend/`: the Kotlin and Ktor backend.

## Mobile

The Android app is an early implementation. It supports Home, My Stories, and Characters tabs, plus character creation with validation and local database storage. Saved characters appear on Home and in the character list. Story cards and artwork are sample content; the story creation screen is a prototype form whose generate action currently returns to Home. Real story and avatar generation, saved generated stories, and completion notifications are not implemented.

The intended flow is **characters and places → your own idea or “Surprise me” → generated text and one cover → automatically saved story → reading**. Parents will select reusable heroes and a setting, or let “Surprise me” invent the setting, events, and ending. The cover will also serve as the library thumbnail and reader's opening image; the rest of the story will be text. “New adventure with these heroes” will reuse the cast for an independent adventure, without continuing an earlier plot.

The screens below summarize the current app and the planned experience:

| Screen | Purpose and implementation status |
| --- | --- |
| Home | Available with sample recent stories, saved characters, and entry points for creating a story or character. Places and generation status cards are planned. |
| My Stories | Available as a library layout with sample covers and titles. A persisted story library and opening stories are planned. |
| Characters | Available list of locally saved characters, with an action to add a character. Opening details is planned. |
| Character creation / editing | Creation and saving of name, type, personality, and appearance are available. The avatar preview uses sample artwork; generating an avatar from the description and editing saved characters are planned. |
| Character details | Planned: avatar and descriptions, editing, deletion, and starting a story with this hero. |
| Places | Planned: browse custom places followed by predefined suggestions and select a setting from Home or the story form. |
| Place creation / editing | Planned: save a custom place's name, description, and optional inhabitants for reuse. |
| Story creation | Prototype form with sample heroes, theme and length choices, and optional wishes. Selecting saved characters and places, “Surprise me”, and submitting a real generation job are planned. |
| Generation progress | Planned: text and cover stages, readiness, failure, and retry. |
| Reader | Planned: cover and scrollable story text, text-size settings, evening mode, and a new independent adventure with the same cast. |

Generation is intended to continue after leaving the progress screen or closing the app. A completion notification, when system permission is granted, will open the ready story; it will also be available on Home and in the library. Background jobs and these completion paths are planned, not operational services in the current app.

See [mobile/README.md](mobile/README.md) for running and building the Android app, and [mobile/SPEC.MD](mobile/SPEC.MD) for detailed intended behavior, screen variants, and open decisions. Open the [HTML mockup catalog](mobile/design/screens/index.html) locally in a browser to explore the designs. These mockups simulate interactions with example content; they do not implement generation, notifications, or durable app storage.

## Backend

The backend is a Ktor 3.6.0 application using constructor injection, Ktor DI, and Exposed SQL DSL over SQLite. Run it from `backend/` with `./gradlew run` (JDK 21). It implements `GET /hello`, `GET /places`, and `PUT /places/{id}`. Other endpoints in [the API reference](backend/API.md) remain proposals.

Settings are loaded from `backend/src/main/resources/application.yaml` into `AppConfig`. Environment overrides:

| Variable | Default | Purpose |
| --- | --- | --- |
| `HOST` | `0.0.0.0` | Listen address |
| `PORT` | `8080` | Listen port |
| `DOZY_DATABASE_PATH` | `data/dozy.db` | Persistent SQLite file, relative to the server working directory |
| `DOZY_DATABASE_BUSY_TIMEOUT_MS` | `5000` | SQLite lock wait, from 1 to 60000 ms |

The database and its parent directory are created automatically. Version 1 creates installation/place tables and seeds three shared predefined places. Local database files are ignored by Git when running from `backend/`.

Place requests require a canonical UUID in `X-Installation-Id`. Custom places are private to that installation; predefined places are read-only. This is the initial installation-based contract, without accounts or authentication. The mobile clients do not send this shared header yet. See [API.md](backend/API.md) for bodies and errors, and [DB.md](backend/DB.md) for the SQLite implementation and future PostgreSQL design.

```sh
curl http://localhost:8080/places \
  -H 'X-Installation-Id: 60c55c02-8d8b-46c2-a0cb-9a2d241eeea0'
```

Run backend and mobile tests from the repository root with `./test-all` (uses each project's Gradle wrapper).
