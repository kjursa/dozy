# dozy

Dozy is a multi-repository project for a mobile app and its backend. The app creates bedtime stories featuring characters defined by the user. The mobile app targets Android and iOS and is built with Kotlin Multiplatform (KMP); the backend is built with Kotlin and Ktor.

## Backend

The backend is a Ktor 3.6.0 application. Run it with `gradle -p backend run`; the `/hello` endpoint returns `{"message":"Hello world from Dozy"}`.

Run the backend tests from the repository root with `./test-all` (requires Gradle installed).
