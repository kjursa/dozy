# Compose UI Kit

A reusable Android library module for Jetpack Compose UI foundations.

## Current API

- `ComposeUiKitTheme` accepts Material 3 color, typography, and shape configuration.
- `UiSpacing` provides an eight-dp spacing scale (`x1` through `x8`).
- `MviViewModel<State, Intent, Effect>` provides `StateFlow` state, intent dispatch,
  and a channel-backed stream for one-off effects.

Use Material 3 components directly unless Compose UI Kit adds a distinct reusable
behavior or visual variant; avoid wrappers that only duplicate Material APIs.

Keep app-specific colors, fonts, navigation, and screen behavior in the consuming app.
The module is consumed locally with `implementation(project(":compose-ui-kit"))`.
