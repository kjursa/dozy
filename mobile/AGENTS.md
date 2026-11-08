# Mobile architecture and UI guidance

## Run and test

- Open `mobile/` in Android Studio and run the `app` configuration on an emulator or connected Android device.
- Build the debug APK from `mobile/` with `./gradlew :app:assembleDebug`.
- Install the debug app on a connected emulator/device with `./gradlew :app:installDebug`.
- Run mobile JVM unit tests from `mobile/` with `./gradlew test`.
- Before committing Kotlin changes, run `./gradlew ktlintFormat` from `mobile/` to apply automatic formatting. Ignore its exit status and any remaining ktlint findings for now; they can be reviewed and fixed manually later.
- Run the repository-wide test command from the repository root with `./test-all`; it runs backend and mobile tests.

## Architecture

- Keep the architecture simple and organize code by `presentation`, `domain`, and `data` responsibilities. Prefer packages first; create extra Gradle modules only when a real boundary or reuse need justifies them.
- `presentation` owns Compose screens, screen-specific UI state/actions/effects, and ViewModels. Keep screen content stateless where practical and connect MVI screens with `MviScreen` from `:compose-ui-kit`.
- `domain` contains pure Kotlin business models and repository contracts. It must not depend on Android, AndroidX, Compose, platform SDKs, or data-layer implementations. Keep business rules independent of framework types.
- Represent domain concepts backed by a primitive or `String` with distinct `@JvmInline value class` types, such as `Name` and `Description`, so values with different meanings cannot be accidentally swapped. Use `data class` models to group cohesive use case inputs and results, and pass those models to use cases instead of several unrelated parameters. When construction must enforce invariants, provide a named factory in the value class's companion object. Do not add value classes for primitives that have no distinct domain meaning.
- `data` contains repository implementations, local/remote data sources, transport/database models, and mapping to domain models. ViewModels and use cases must not access databases or APIs directly.
- Keep repository interfaces in `domain` and implement them in `data`. Data implementations may depend on domain contracts; domain must not depend on data.
- Keep repository implementations flat: inject and use the DAO and API directly whenever practical. Avoid `LocalDataSource`/`RemoteDataSource` wrappers that only forward calls; add an abstraction when it owns meaningful behavior or creates a useful boundary.
- Add a use case when it contains meaningful business rules, coordinates multiple repositories, or is reused. Avoid use cases that only delegate one call without adding behavior.
- Keep dependencies pointing inward: `presentation` may depend on `domain`; `data` may depend on `domain`; `domain` stays framework-independent.
- Prefer Kotlin/Flow APIs and platform-neutral types in code that may later move to KMP `commonMain`. Keep Android-specific behavior in platform-specific code.

## UI

- Follow Compose best practices and consider recomposition performance, but do not optimize at the expense of clarity or maintainability.
- Keep composable functions stateless where practical. Hoist state and event handling to the caller when doing so makes the UI easier to reuse, test, or reason about.
- When the main layout contains a logically distinct section, extract that section into a separate composable in the same file to keep the screen structure readable.

## Compose UI Kit

- Use `:compose-ui-kit` for reusable Compose UI foundations, theme entry points, spacing, and shared MVI infrastructure.
- Keep Dozy-specific palette, fonts, assets, navigation, and screen behavior in `:app`.
- Use Material 3 components directly unless the library adds distinct reusable behavior or styling; avoid wrappers that only duplicate Material APIs.
- Before changing public library APIs, read `compose-ui-kit/AGENTS.md` and update `compose-ui-kit/README.md` as needed.
