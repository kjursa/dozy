# Backend implementation guidelines

## Run and test

- Start the backend from `backend/` with `./gradlew run`.
- Run backend tests from `backend/` with `./gradlew test`.
- Run the repository-wide test command from the repository root with `./test-all`; it runs backend and mobile tests.

## Architecture

Keep the backend feature-oriented and lightweight. The layers below are package boundaries; they do not require separate Gradle modules. Do not create a package or class until there is code with that responsibility.

```text
src/main/kotlin/com/dozy/
  config/                          # Application settings and configuration loading
  di/                              # Application composition root and dependency wiring
  plugins/                         # Shared Ktor plugins, such as authentication and logging
  features/<feature>/
    api/                           # Ktor routes, request/response models, HTTP mapping
    application/                   # Use cases or a small feature service
    domain/
      model/                        # Business models, values, and domain rules
      repository/                   # Repository contracts required by the application
    data/
      repository/                   # Repository implementations
      database/                     # Database access, schema, migrations, queries
      remote/<provider>/            # Clients and DTOs for third-party APIs
  infrastructure/<concern>/        # Optional shared technical integrations
```

Use feature-specific packages such as `com.dozy.features.story` and `com.dozy.features.auth`. Keep shared code outside a feature only when multiple features genuinely use it. `infrastructure/` is optional; third-party API clients and database adapters normally belong in the feature's `data/` package.

### Domain — models and contracts

- Put business models, value objects, and domain rules in `domain.model`.
- Model business rules for individual fields (such as requiredness, maximum length, UUID syntax, or a required hex format) in domain value types. Prefer types nested in their owning model when they are specific to it, such as `Place.Name` or `Place.Id`.
- Create validated value types through parsing/factory functions that return an explicit success-or-error result; do not use `null` when callers need to know why parsing failed, and do not use exceptions for expected invalid input. Give each value type its own error type containing only errors that can actually occur for that value. A small generic result container may be shared.
- Normalize input such as trimming whitespace in the value type parser when that is part of the domain rule. Successful parsing returns the normalized, valid value; invalid parsing returns a domain-level error without HTTP status codes or presentation text.
- Put data-access contracts required by the application in `domain.repository`; group other contracts by business area.
- Keep domain independent of other layers, frameworks, databases, and HTTP transport.
- Keep database models and external-service DTOs outside the domain.

### Application — use cases

- Put business operations and orchestration here. Use a focused `<Action><Feature>UseCase` when it contains meaningful logic, coordinates dependencies, or is reused.
- For several small, closely related operations in one feature, a single concrete `<Feature>Service` can be simpler than one class per operation. Do not create both a service and a use case when they duplicate responsibilities.
- Receive domain contracts and other collaborators through constructors.
- Have services/use cases accept validated domain values rather than raw strings or transport DTOs. When an operation needs several values, group them in a focused command/domain parameter object such as `SavePlace`, rather than growing a long parameter list. The object should contain the exact validated values required by that operation.
- Keep state-dependent checks (such as whether an ID exists, belongs to an installation, or may be changed) in the application/service or repository flow; validated value types cover the value's own format and intrinsic business constraints.
- Do not create concrete repositories, network clients, or adapters inside use cases.
- Keep SQL, HTTP, process handling, and framework configuration out of application code.
- Name use cases for the action they perform, such as `CreateStoryUseCase`. Avoid a class that only delegates one call without adding responsibility.

### Data — data access

- Put repository implementations in `data.repository`.
- Put database connections, schema, migrations, and queries in `data.database`.
- Put third-party API clients, provider-specific DTOs, and adapters in `data.remote.<provider>`.
- Reuse configured Ktor `HttpClient` instances; do not create a client for every request.
- Keep provider-specific authentication, serialization, timeouts, and error translation in the remote adapter/configuration.
- Map database and remote DTOs to domain models at the data boundary. Repositories coordinate the database and remote sources when needed.
- Keep transactions, caching, and storage details in this layer.

### API — communication boundary

- Put endpoints, request/response models, and HTTP error mapping here.
- Pass requests to application operations and map results to HTTP responses.
- Keep business logic, database queries, and third-party integration details out of route handlers.
- Validate input format at the API boundary; keep business rules in the domain/application layers.
- Convert request fields to validated domain value types at the API boundary. Map their typed domain errors to the appropriate HTTP error response there; do not make domain types depend on HTTP or presentation models.
- Name inbound models `<Action><Feature>Request`, such as `CreateStoryRequest`, and outbound models `<Feature>Response`, such as `StoryResponse`.
- Keep request/response models separate from domain models. Use simple top-level mapping extensions in feature-specific `*Mappers.kt` files; use mapper classes only when mapping needs injected dependencies or state.

### Authentication and logging

- Configure Ktor's Authentication plugin in shared plugin/configuration code; keep login and account operations in the `auth` feature.
- Keep authorization decisions close to the application/domain operation that requires them, while routes declare which authentication is required.
- Configure inbound request logging centrally with Ktor's CallLogging plugin. Do not log passwords, access tokens, cookies, or sensitive user/story content.
- If enabling Ktor Client Logging for third-party calls, sanitize authorization headers and avoid logging sensitive request/response bodies.
- Load secrets and environment-specific settings through config; keep secrets and local configuration outside the repository.

### Config, plugins, and DI

- Keep app-wide configuration and shared Ktor plugins in `config/` and `plugins/`; these are technical setup, not business layers.
- Create objects and connect layers in `di/`, the composition root.
- Bind interfaces explicitly to implementations and provide dependencies through constructors.
- Do not resolve dependencies from a container inside use-case or domain methods.
- Prefer constructor injection and simple wiring. Use Ktor's built-in DI (`ktor-server-di` and its `dependencies {}` DSL) when manual wiring becomes cumbersome; avoid third-party DI without a concrete need.
- Keep `com.dozy.di.Dependencies.kt` as the application composition root and call it once from `Application.module()` before routes that resolve dependencies. Split bindings into feature files only when the composition root grows enough to benefit.
- In route tests, override production DI bindings with fakes using Ktor's test application DI support.

### General rules

- Keep classes focused on one responsibility and functions small and clearly named.
- Direct dependencies inward: API depends on application, application depends on domain contracts, and data/infrastructure implement contracts. Domain does not know adapters.
- Avoid cyclic package dependencies.
- Add a layer, interface, or abstraction only when it separates real responsibility, external technology, or a useful test seam.
- Organize tests like production packages under the standard `src/test` source set; do not create a separate Gradle module for tests.

## Testing

- Put tests under `src/test/kotlin` using the same package structure as production code. Name them after the class or behavior under test, for example `StoryRoutesTest` and `CreateStoryUseCaseTest`.
- Test routes with Ktor's `testApplication` and inject test dependencies where practical.
- Test application operations directly with fake or in-memory repository implementations. Do not add an interface solely to mock a concrete use case or feature service.
- Add domain or repository tests where they cover meaningful rules or behavior. Keep test fixtures under `src/test`.
- Every behavior change must add or update an automated test that verifies it.
- After backend code, test, or Gradle configuration changes, run `./test-all` from the repository root and report whether the tests passed. Report failures accurately; do not claim tests passed unless the command completed successfully.
