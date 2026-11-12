package com.dozy.mobile

import androidx.lifecycle.SavedStateHandle
import com.dozy.mobile.feature.place.domain.model.Place
import com.dozy.mobile.feature.place.domain.usecase.PlaceIdGenerator
import com.dozy.mobile.feature.place.domain.usecase.SavePlace
import com.dozy.mobile.feature.place.domain.usecase.SelectPlace
import com.dozy.mobile.feature.place.presentation.form.PlaceFormUiState
import com.dozy.mobile.feature.place.presentation.form.PlaceFormViewModel
import com.dozy.mobile.resources.*
import com.kjursa.foundation.compose.mvi.SavedStatePersistence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.*
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

@OptIn(ExperimentalCoroutinesApi::class)
internal open class PlaceFormViewModelFixture {
    protected val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest fun teardown() {
        Dispatchers.resetMain()
    }

    protected fun vm(
        repository: FakePlaceRepository,
        handle: SavedStateHandle = SavedStateHandle(),
        id: String? = null,
    ) = PlaceFormViewModel(
        SavedStatePersistence(handle, PlaceFormUiState.serializer()),
        SavePlace(
            repository,
            PlaceIdGenerator {
                Place.Id("new")
            },
        ),
        SelectPlace(repository),
        id,
    )
}
