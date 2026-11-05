# Backend implementation guidelines

## Structure

Organize production code by feature. Keep each feature's HTTP API, domain, service, and data code together:

```text
src/main/kotlin/com/dozy/
  di/
    Dependencies.kt     # Application-wide composition root and Ktor DI bindings
  features/<feature>/
    api/                 # Ktor routes, HTTP request/response models, mapping extensions
    di/                  # Ktor DI registrations owned by this feature
    domain/
      model/             # Framework-independent domain models
      <Feature>Repository.kt
    service/             # Feature logic and orchestration
    data/                # Repository implementations and data sources
  plugins/               # Shared Ktor plugin configuration
```

Use a feature-specific package name, such as `com.dozy.features.story`. Keep shared code in `shared/` only when multiple features genuinely use it. Do not create a separate Gradle module for tests; use the standard `src/test` source set.

## Naming and responsibilities

- Name inbound HTTP models `<Action><Feature>Request`, such as `CreateStoryRequest`.
- Name outbound HTTP models `<Feature>Response`, such as `StoryResponse`. Request and response models belong to `api` and must not be used as domain models.
- Put domain models in `domain/model` and give them domain names such as `Story`. Keep them independent of Ktor and HTTP serialization details.
- Put API mapping extensions in `api`, in a feature-specific `*Mappers.kt` file. For simple, stateless conversions, use top-level extension functions such as `CreateStoryRequest.toDomain()` and `Story.toResponse()`; do not wrap them in a mapper class or `companion object`.
- Use a mapper class only when mapping genuinely needs injected dependencies or state. Keep lookups and business decisions in the service rather than hiding them in a mapper.
- Name the feature orchestrator `<Feature>Service`, such as `StoryService`. Implement it as a concrete class by default; it coordinates feature logic and calls repositories. Do not add a separate use-case class when it would only delegate to the service or repository.
- Define repository interfaces in the feature's `domain` package, such as `StoryRepository`. Put implementations in `data`, named for their technology or source, such as `SqlStoryRepository`.
- Keep routes focused on HTTP concerns: read and validate requests, call the service, and return the appropriate response and status.
- Prefer constructor injection and simple manual wiring. Add a dependency-injection framework or extra abstraction only when the project has a concrete need for it.
- When dependency injection is useful, use Ktor's built-in DI (`ktor-server-di` and its `dependencies {}` DSL); do not add a third-party DI framework without a concrete need.
- Put each feature's DI registrations in `features/<feature>/di/<Feature>Dependencies.kt`, exposed as an `Application.configure<Feature>Dependencies()` function. Register that feature's repository implementation and concrete service there. Do not create an empty DI module for a feature with no dependencies to wire.
- Keep one application-level composition root in `com.dozy.di.Dependencies.kt`, exposed as `Application.configureDependencies()`. It should register shared infrastructure dependencies and call each feature's DI registration function. Call it once from `Application.module()` before registering routes or modules that resolve dependencies. Do not register dependencies inside route handlers.
- Register repository interfaces to their data implementations and services by their concrete class. Keep service classes concrete unless a real substitution boundary requires an interface.
- In route tests, override production DI bindings with fakes using Ktor's test application DI support.

## Dependencies and testing

- Keep dependencies flowing inward: `api` calls `service`; `service` uses `domain`; `data` implements domain repository interfaces. Domain code must not depend on Ktor or data implementations.
- Put tests under `src/test/kotlin` using the same package structure as production code. Name them after the class or behavior under test, for example `StoryRoutesTest` and `StoryServiceTest`.
- Test routes with Ktor's `testApplication` and inject test dependencies where practical.
- Test service logic directly by constructing the concrete service with a fake or in-memory repository. Do not add a service interface solely to mock the service.
- Add domain or repository tests where they cover meaningful rules or behavior. Keep test fixtures under `src/test`.
- Every behavior change must add or update an automated test that verifies it.
- Keep the architecture lightweight: add layers, interfaces, and shared abstractions only when they isolate real logic, external dependencies, or a needed test seam.
