# Backend implementation guidance

The backend is a Kotlin/Ktor 3.6.0 application. Keep its structure feature-first as it grows, and keep simple features simple: a route that only returns a response does not need separate use-case, domain, and data layers.

For features that need those responsibilities, use packages under `com.dozy`:

```text
backend/src/main/kotlin/com/dozy/
  features/<feature>/
    api/           # Ktor routes and transport DTOs
    application/   # use cases when business logic requires them
    domain/        # framework-independent models and interfaces
    data/          # data access and implementations
  plugins/         # shared Ktor configuration
  shared/          # code genuinely shared across features
```

Keep dependencies pointing inward: `api -> application -> domain`. Domain code must not depend on Ktor or transport details. Put interfaces owned by the inner layers near those layers; data implementations satisfy those interfaces. Wire implementations to application services at the application boundary (for example, in Ktor module setup). Add a layer, interface, or shared abstraction only when it solves a real need; avoid empty packages and speculative indirection.

Test HTTP behavior with Ktor's `testApplication`, including status and response content. Add focused unit tests for domain and application logic when that logic exists. Keep tests under the matching package in `backend/src/test/kotlin` and use the project's Gradle test task (`./test-all` from the repository root).
