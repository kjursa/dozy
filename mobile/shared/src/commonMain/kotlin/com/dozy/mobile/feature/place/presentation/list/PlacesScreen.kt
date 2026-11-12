package com.dozy.mobile.feature.place.presentation.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dozy.mobile.feature.place.domain.model.Place
import com.dozy.mobile.resources.*
import com.kjursa.foundation.compose.mvi.MviScreen
import com.kjursa.foundation.presentation.text.resolve
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun PlacesScreen(
    viewModel: PlacesViewModel,
    onEffect: (PlacesEffect) -> Unit,
) {
    MviScreen(viewModel, onEffect = {
        onEffect(it)
        viewModel.navigationHandled()
    }) { state, dispatch ->
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TextButton(onClick = { dispatch(PlacesAction.Back) }) { Text(stringResource(Res.string.back)) }
            Text(stringResource(Res.string.places_heading), style = MaterialTheme.typography.labelLarge)
            Text(stringResource(Res.string.places_title), style = MaterialTheme.typography.headlineLarge)
            Text(stringResource(Res.string.places_intro))
            OutlinedButton(
                onClick = { dispatch(PlacesAction.Surprise) },
                enabled = !state.navigating,
            ) { Text(stringResource(Res.string.place_surprise)) }
            TextButton(onClick = { dispatch(PlacesAction.Add) }, enabled = !state.navigating) { Text(stringResource(Res.string.place_add)) }
            if (state.loading) CircularProgressIndicator()
            state.error?.let {
                Text(it.resolve(), color = MaterialTheme.colorScheme.error)
                TextButton(
                    onClick = { dispatch(PlacesAction.Retry) },
                    enabled = !state.loading,
                ) { Text(stringResource(Res.string.place_retry)) }
            }
            if (!state.hasCustom) Text(stringResource(Res.string.place_no_custom))
            if (!state.loading && state.places.isEmpty()) {
                Text(stringResource(Res.string.place_empty))
                if (state.error ==
                    null
                ) {
                    TextButton(onClick = { dispatch(PlacesAction.Retry) }) { Text(stringResource(Res.string.place_retry)) }
                }
            }
            state.places.forEach { place ->
                PlaceCard(place, !state.navigating, onSelect = {
                    dispatch(PlacesAction.Select(place.id))
                }, onEdit = { dispatch(PlacesAction.Edit(place.id)) })
            }
        }
    }
}

@Composable
internal fun PlaceCard(
    place: Place,
    enabled: Boolean = true,
    onSelect: () -> Unit,
    onEdit: (() -> Unit)? = null,
) {
    Surface(shape = MaterialTheme.shapes.medium, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onSelect),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    Icons.Default.Landscape,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.secondaryContainer),
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(place.name.value, style = MaterialTheme.typography.titleMedium)
                    Text(place.description.value, style = MaterialTheme.typography.bodyMedium)
                    place.inhabitants?.let {
                        Text(
                            stringResource(Res.string.place_inhabitants_value, it.value),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            if (place.canEdit &&
                onEdit != null
            ) {
                TextButton(onClick = onEdit, enabled = enabled) { Text(stringResource(Res.string.place_edit)) }
            }
        }
    }
}
