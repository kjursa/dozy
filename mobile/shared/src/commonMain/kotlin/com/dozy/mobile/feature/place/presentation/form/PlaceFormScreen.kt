package com.dozy.mobile.feature.place.presentation.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dozy.mobile.resources.*
import com.kjursa.foundation.compose.mvi.MviScreen
import com.kjursa.foundation.presentation.text.UiText
import com.kjursa.foundation.presentation.text.resolve
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PlaceFormScreen(
    viewModel: PlaceFormViewModel,
    onEffect: (PlaceFormEffect) -> Unit,
) {
    MviScreen(viewModel, onEffect = { onEffect(it) }) { state, dispatch ->
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(
                    rememberScrollState(),
                ).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TextButton(onClick = {
                dispatch(PlaceFormAction.Back)
            }, enabled = !state.saving && !state.navigating) { Text(stringResource(Res.string.back)) }
            Text(
                stringResource(
                    if (state.id ==
                        null
                    ) {
                        Res.string.place_add
                    } else {
                        Res.string.place_edit
                    },
                ),
                style = MaterialTheme.typography.headlineLarge,
            )
            if (state.loading) CircularProgressIndicator()
            PlaceField(
                state.name,
                Res.string.place_name,
                state.nameError,
                !state.saving && state.initialized,
            ) { dispatch(PlaceFormAction.Name(it)) }
            PlaceField(state.description, Res.string.place_description, state.descriptionError, !state.saving && state.initialized) {
                dispatch(PlaceFormAction.Description(it))
            }
            PlaceField(state.inhabitants, Res.string.place_inhabitants, state.inhabitantsError, !state.saving && state.initialized) {
                dispatch(PlaceFormAction.Inhabitants(it))
            }
            Text(stringResource(Res.string.place_inhabitants_hint))
            state.error?.let { Text(it.resolve(), color = MaterialTheme.colorScheme.error) }
            if (!state.initialized &&
                !state.loading
            ) {
                TextButton(onClick = { dispatch(PlaceFormAction.Retry) }) { Text(stringResource(Res.string.place_retry)) }
            }
            Button(onClick = {
                dispatch(PlaceFormAction.Save)
            }, enabled = state.initialized && !state.saving && !state.navigating) { Text(stringResource(Res.string.place_save)) }
            if (state.saving) CircularProgressIndicator()
        }
    }
}

@Composable
private fun PlaceField(
    value: String,
    label: org.jetbrains.compose.resources.StringResource,
    error: UiText?,
    enabled: Boolean,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value,
        onChange,
        label = { Text(stringResource(label)) },
        isError = error != null,
        enabled = enabled,
        supportingText = { error?.let { Text(it.resolve()) } },
        modifier = Modifier.fillMaxWidth(),
    )
}
