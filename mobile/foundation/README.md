# Foundation

A small reusable Kotlin Multiplatform library for coroutine utilities, presentation text, and Compose foundations.

## Packages

- `com.kjursa.foundation.coroutines`: coroutine helpers independent of Android/Compose APIs.
- `com.kjursa.foundation.presentation.text`: presentation text and its Compose resolver.
- `com.kjursa.foundation.compose.mvi`: reusable state, actions, effects, and screen binding.
- `com.kjursa.foundation.compose.theme`: theme entry point and spacing scale.

The Android, iosArm64 and iosSimulatorArm64 targets share coroutine helpers, Compose,
MVI, UiText and SavedStatePersistence in `commonMain`. Portable tests live in
`commonTest`; Android SavedStateHandle restoration integration stays in
`androidHostTest`. Lifecycle is pinned to org.jetbrains.androidx.lifecycle 2.10.0
and SavedState to org.jetbrains.androidx.savedstate 1.4.0. This library has no
app-specific dependencies.

## Current API

- `runCatchingSuspend` returns Kotlin `Result` for ordinary exceptions, rethrows
  `CancellationException`, and allows unrecoverable errors to propagate.
- `UiText.Resource` represents a string resource with optional formatting arguments;
  `UiText.Plain` represents runtime text. The Composable `UiText.resolve()` extension
  uses Compose Multiplatform resources and the current locale or returns the plain
  value. Consuming apps supply their own `StringResource` values and formatting
  arguments; the library contains no Dozy strings.

- `FoundationTheme` accepts Material 3 color, typography, and shape configuration.
- `UiSpacing` provides an eight-dp spacing scale (`x1` through `x8`).
- `MviViewModel<State, Action, Effect>` provides lifecycle-aware `uiState`, action
  dispatch, and a channel-backed stream for one-off effects.
- `MviScreen` collects that state with lifecycle awareness and connects it to a
  stateless content composable. It also forwards one-off effects to the route.

Use Material 3 components directly unless Foundation adds a distinct reusable
behavior or visual variant; avoid wrappers that only duplicate Material APIs.

Keep app-specific colors, fonts, navigation, and screen behavior in the consuming app.
The module is consumed locally with `implementation(project(":foundation"))`.

## MVI usage

Define screen-specific state, actions, effects, and a ViewModel in the app:

```kotlin
data class CounterUiState(val count: Int = 0)

sealed interface CounterAction {
    data object Increment : CounterAction
}

sealed interface CounterEffect {
    data object ShowLimitMessage : CounterEffect
}

class CounterViewModel : MviViewModel<CounterUiState, CounterAction, CounterEffect>(CounterUiState()) {
    override fun onAction(action: CounterAction) {
        when (action) {
            CounterAction.Increment -> updateState { copy(count = count + 1) }
        }
    }
}
```

Connect the ViewModel to a route and keep the visible screen stateless:

```kotlin
@Composable
fun CounterRoute(
    viewModel: CounterViewModel,
    onShowMessage: () -> Unit,
) {
    MviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                CounterEffect.ShowLimitMessage -> onShowMessage()
            }
        },
    ) { state, dispatch ->
        CounterScreen(state = state, onAction = dispatch)
    }
}

@Composable
fun CounterScreen(state: CounterUiState, onAction: (CounterAction) -> Unit) {
    Button(onClick = { onAction(CounterAction.Increment) }) {
        Text("Count: ${state.count}")
    }
}
```

Obtain/inject the ViewModel and handle navigation in the route. The library does
not depend on a navigation framework. Use one `MviScreen` collector per ViewModel
because one-off effects are delivered through a single-consumer channel.

## Validation

From `mobile/`, run `./gradlew --no-daemon :foundation:testAndroidHostTest` for library unit tests
and `./gradlew --no-daemon :app:assembleDebug` to check integration with the consuming app.

## Optional state restoration

`MviViewModel` accepts an optional constructor-injected `StatePersistence<State>`.
It restores the initial state and automatically persists the committed value after
`updateState`. Existing ViewModels can omit persistence. Dispatch actions and call
`updateState` on the main thread; reducers must be pure because StateFlow may retry
them. Persistence runs outside the reducer.

For a small form, annotate its state with `kotlinx.serialization.Serializable` and
mark errors, loading/saving status, derived data, and other non-restorable fields
with `kotlinx.serialization.Transient`. Transient fields need default values:

```kotlin
@Serializable
data class EditorState(
    val text: String = "",
    @Transient val isLoading: Boolean = false,
    @Transient val error: UiText? = null,
)
```

At the application's DI boundary, supply
`SavedStatePersistence(handle, EditorState.serializer())` to the ViewModel. No
Serializable interface or Parcelable implementation is required. New serializable
fields are included automatically; give new fields defaults so older snapshots
can still be decoded. Domain values need not be serializable when they are excluded
from snapshots; retain only minimal input/IDs needed to rebuild larger data.

The adapter stores one JSON string under `STATE` in `SavedStateHandle` (a custom key
and Json instance can be supplied). Default encoding preserves field defaults and
ignores removed/unknown fields on restore. Invalid or incompatible serialized data
is discarded and the supplied initial state is used. Serialization failures during
save propagate so programming errors are visible. Each adapter owns its key.

This follows SavedStateHandle's lifecycle: it helps restore a system-recreated
screen and is not durable draft storage after intentional dismissal or app data
removal. Large data belongs in a repository. SavedStateHandle is a supported
multiplatform API, so the adapter and its exact
`KSerializer<State>` typing are shared. Android Koin obtains the handle from the
navigation owner and retains existing process-death wiring. On iOS a future host
can inject `SavedStatePersistence(SavedStateHandle(), State.serializer())` and
retain that handle with its owner. This is only an in-memory adapter: no iOS
process-death or cross-relaunch persistence is promised.
