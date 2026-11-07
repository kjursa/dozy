# Compose UI Kit

A reusable Android library module for Jetpack Compose UI foundations.

## Current API

- `ComposeUiKitTheme` accepts Material 3 color, typography, and shape configuration.
- `UiSpacing` provides an eight-dp spacing scale (`x1` through `x8`).
- `MviViewModel<State, Action, Effect>` provides lifecycle-aware `uiState`, action
  dispatch, and a channel-backed stream for one-off effects.
- `MviScreen` collects that state with lifecycle awareness and connects it to a
  stateless content composable. It also forwards one-off effects to the route.

Use Material 3 components directly unless Compose UI Kit adds a distinct reusable
behavior or visual variant; avoid wrappers that only duplicate Material APIs.

Keep app-specific colors, fonts, navigation, and screen behavior in the consuming app.
The module is consumed locally with `implementation(project(":compose-ui-kit"))`.

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
