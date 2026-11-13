# Mobile architecture and UI guidance

## Run and test

- Open `mobile/` in Android Studio and run the `app` configuration on an emulator or connected Android device.
- Build the debug APK from `mobile/` with `./gradlew --no-daemon :app:assembleDebug`.
- Install the debug app on a connected emulator/device with `./gradlew :app:installDebug`.
- Run mobile JVM unit tests from `mobile/` with `./gradlew --no-daemon :app:test :foundation:testAndroidHostTest :shared:testAndroidHostTest`.
- Before committing Kotlin changes, run `./gradlew --no-daemon ktlintFormat` from `mobile/` to apply automatic formatting. Ignore its exit status and any remaining ktlint findings for now; they can be reviewed and fixed manually later.
- Run the repository-wide test command from the repository root with `./test-all`; it runs backend and mobile tests.

## Architecture

### Package structure and dependencies

- Organize code by feature first. Use the following structure within `feature.<area>`:

  ```text
  data/
    api/
    database/
  di/
  domain/
    model/
    repository/
    usecase/
  presentation/
    <screen>/
  ```

- Keep repository implementations and data mapping in `data`, HTTP clients and DTOs in `data.api`, and database definitions, entities, and DAOs in `data.database`.
- Put domain models and operation input/validation types in `domain.model`, repository interfaces in `domain.repository`, and use cases with their operation-specific collaborators in `domain.usecase`.
- Keep screen layouts, ViewModels, UI state, actions, effects, and presentation mapping in `presentation`. Use separate files for the ViewModel, state, actions, and effects; group screens belonging to the same feature under their own packages.
- Keep domain models shared across features in their owning feature's domain package. Presentation models may be specific to a screen; do not duplicate domain models for each screen or move them into a generic shared package solely because multiple screens use them.
- Keep dependencies pointing inward: presentation and data may depend on domain; domain must remain pure Kotlin and independent of Android, AndroidX, Compose, platform SDKs, DTOs, database entities, and data implementations. ViewModels depend on use cases, and use cases depend on repository contracts rather than DAOs or API clients.
- Keep app-wide technical helpers in `core`, with no dependencies on features. Follow the Foundation section for reusable library APIs. Prefer packages first; add Gradle modules when an actual boundary or reuse need justifies them.
- Use platform-neutral Kotlin and coroutine/Flow APIs wherever practical to support a future KMP migration. Isolate platform-specific construction and APIs at the platform boundary.

### Domain models and validation

- Represent semantically distinct primitive and string values with `@JvmInline value class` types. Nest closely related value types and enums within their owning domain model to make their context explicit and avoid repeated name prefixes. Use `data class` for cohesive data structures; avoid wrapping primitives without distinct domain meaning.
- Distinguish existing domain data from requests to create or modify it. Current input restrictions must not automatically invalidate existing values read from the database or API; handle malformed transport/storage data at the data boundary.
- For operations requiring validation of user input, introduce a dedicated domain input type with a private constructor and a named companion factory. Prevent unchecked construction or copying that bypasses validation.
- The factory validates and normalizes raw input and returns a typed success/invalid outcome. Collect errors across all fields, with at most one typed error per field for now, rather than throwing exceptions for expected invalid input or stopping at the first field.
- Group field errors in a dedicated domain error model with nullable properties for individual fields. Nest error enums or sealed types in that model when they are used only for that operation. Keep domain errors independent of resource IDs, localized strings, and UI types.
- Pass validated input objects to use cases instead of unrelated primitive parameters. Enforce input rules during construction; keep business checks requiring repositories or other external information in the appropriate use case.

### Use cases and results

- Give each use case one clear operation. Keep operation-specific result types in the same file, nested within the use case.
- For operations with success/failure outcomes, return the use case's own sealed `Result` with explicit variants so callers can handle it exhaustively. Use Flow for observable data where appropriate.
- Use Kotlin `Result` internally for technical exception handling and map it to the operation's typed outcome at the use case boundary. When capturing failures from suspend operations, prefer `runCatchingSuspend` from `:foundation` (`com.kjursa.foundation.coroutines`) where appropriate instead of repeating `try/catch` blocks that manually rethrow `CancellationException` or using Kotlin `runCatching`. Cancellation must propagate rather than become an ordinary failure. Keep `try/finally` for resource cleanup and `Flow.catch` for flow error handling where appropriate.

### Repositories, mapping, and dependency injection

- Keep repository implementations flat: inject and use the DAO and API client directly whenever practical. Add another abstraction only when it owns meaningful behavior; avoid local/remote data-source wrappers that merely forward calls.
- Put mapping in separate files as extension functions near the layer that knows both models: transport/database mapping in data and domain-to-UI mapping in presentation. Use a constructor-injected mapper when mapping is complex or needs collaborators.
- Define feature dependencies in `di` using Koin modules. Keep platform-specific providers separate from definitions intended for shared code, and assemble feature modules at the application composition root.
- Register ViewModels with Koin's ViewModel lifecycle support and obtain them through lifecycle-aware Koin injection at screen/route boundaries. Scope them to the appropriate navigation owner rather than registering them as singletons or maintaining manual factories.
- Supply dependencies through constructors. Domain, data, and ViewModels must not resolve collaborators from a service locator internally; constructor-based unit tests should work without starting Koin.

### Form state and text

- Screens requiring state restoration must receive `StatePersistence<State>` through the ViewModel constructor, configured at the DI boundary with a `KSerializer<State>` for that exact state type. Mark the state `@Serializable`. Persist the minimum input/IDs needed to reconstruct the screen; mark errors, loading/saving flags, and derived or non-restorable values as `@Transient` with safe defaults. Prefer automatic state persistence over manual per-field SavedStateHandle keys. State updates and persistence run on the main thread; keep persistence outside pure reducer lambdas.
- Keep pure, named state-update methods in the UI state type when they make updates easy to find and express intent. Use field updates or grouped updates such as `withErrors` where useful; do not generate a method for every property or combination of properties. Use ordinary `copy(...)` for ad hoc multi-field updates instead of forced setter chains or methods named after a list of fields. State methods only return a new state; keep validation, domain-to-presentation conversion, use case calls, and effects outside them.
- Store one nullable `UiText` error per form field in UI state. Map typed domain validation errors to those fields in a presentation mapper; clear or update them when input changes or validation succeeds.
- Represent localized messages as resource-backed `UiText` and runtime user-facing messages as plain `UiText`. Resolve them in Compose with the presentation text resolver so localization happens when rendering. Map known backend error codes to resources and display raw server text only when the API defines it as suitable for users.
- Validate before starting a save operation. While saving, expose a loading state and prevent duplicate submissions; on failure, restore the action and expose an appropriate error. Handle successful navigation and other transient UI actions through one-off effects, while keeping renderable information in state.

## UI

- Use `design/screens/` as the visual reference for UI work; start at `design/screens/index.html` and inspect the relevant rendered screen and variants. Follow their layout, typography, colors, and components. Sidebar variant selectors and simulation controls are review tools, not application UI; images alone do not define the layout.
- For new screens or changes to UI flows, read the relevant sections of `SPEC.MD` for behavior, states, actions, and navigation. For a focused behavior fix, use the task and existing implementation as context; consult `SPEC.MD` when the expected behavior is unclear.
- Follow Compose best practices and consider recomposition performance, but do not optimize at the expense of clarity or maintainability.
- Keep composable functions stateless where practical. Hoist state and event handling to the caller when doing so makes the UI easier to reuse, test, or reason about.
- When the main layout contains a logically distinct section, extract that section into a separate composable in the same file to keep the screen structure readable.

## Foundation

- Use `:foundation` for reusable coroutine utilities, presentation text, Compose foundations, theme entry points, spacing, and shared MVI infrastructure. Keep it independent of app features and branding.
- Keep application-specific resources and presentation state/ViewModels in `:shared`. Screens, Dozy palette/typography and navigation live in `:shared/commonMain`; Foundation must stay independent of both.
- Use Material 3 components directly unless the library adds distinct reusable behavior or styling; avoid wrappers that only duplicate Material APIs.
- Before changing public library APIs, read `foundation/AGENTS.md` and update `foundation/README.md` as needed.

### Localized UI copy

- Do not hardcode static user-facing strings in UI code, ViewModels, or UI state. Define screen copy in `shared/src/commonMain/composeResources/values/strings.xml`, use `org.jetbrains.compose.resources.stringResource` in Composables, and supply presentation-state copy through `UiText.Resource`, resolved when rendering. Use Compose `StringResource`, `PluralStringResource`, `DrawableResource` and `FontResource` rather than Android integer IDs. Keep only Android manifest/launcher/notification-channel copy in app `res/values/strings.xml`.
- Keep actual user-entered and backend-provided content dynamic. Use stable IDs or enums for selections and saved state rather than translated display labels.
