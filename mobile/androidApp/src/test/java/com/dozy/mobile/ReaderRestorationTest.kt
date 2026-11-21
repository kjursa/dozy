package com.dozy.mobile

import androidx.lifecycle.SavedStateHandle
import com.dozy.api.contracts.v1.story.StoryDto
import com.dozy.mobile.feature.story.data.api.GenerationApi
import com.dozy.mobile.feature.story.data.api.StoryNotFoundException
import com.dozy.mobile.feature.story.data.testGenerationRepository
import com.dozy.mobile.feature.story.domain.model.Story
import com.dozy.mobile.feature.story.domain.usecase.ReadStory
import com.dozy.mobile.feature.story.presentation.reader.ReaderAction
import com.dozy.mobile.feature.story.presentation.reader.ReaderDestination
import com.dozy.mobile.feature.story.presentation.reader.ReaderUiState
import com.dozy.mobile.feature.story.presentation.reader.ReaderViewModel
import com.dozy.mobile.navigation.AppNavigator
import com.kjursa.foundation.compose.mvi.SavedStatePersistence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReaderRestorationTest {
    private val id = Story.Id("story")

    @Test fun readerRetriesKeepsContentAcrossOverlaysRestoresSettingsAndEmitsOnlyCast() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val progress = ProgressApi().apply { storyUnavailable = true }
                val api =
                    object : GenerationApi by progress {
                        override suspend fun story(id: String): StoryDto? {
                            val dto = progress.story(id)
                            if (progress.storyUnavailable) throw StoryNotFoundException()
                            return dto
                        }
                    }
                val read = ReadStory(testGenerationRepository(FakeGenerationDao(), api))
                val handle = SavedStateHandle()
                val persistence = SavedStatePersistence(handle, ReaderUiState.serializer())
                val vm = ReaderViewModel(AppNavigator(), id, read, persistence)
                assertTrue(vm.uiState.value.loading)
                vm.dispatch(ReaderAction.Retry)
                advanceUntilIdle()
                assertNull(vm.uiState.value.story)
                assertEquals(1, progress.storyReads)
                progress.storyUnavailable = false
                vm.dispatch(ReaderAction.Retry)
                vm.dispatch(ReaderAction.Retry)
                advanceUntilIdle()
                assertEquals(2, progress.storyReads)
                val story = vm.uiState.value.story
                vm.dispatch(ReaderAction.Settings)
                vm.dispatch(ReaderAction.Size(ReaderUiState.TextSize.LARGE))
                vm.dispatch(ReaderAction.Evening(true))
                vm.dispatch(ReaderAction.Back)
                assertNull(vm.uiState.value.overlay)
                vm.dispatch(ReaderAction.Cover)
                vm.dispatch(ReaderAction.Dismiss)
                assertEquals(story, vm.uiState.value.story)
                val restored = ReaderViewModel(AppNavigator(), id, read, persistence)
                advanceUntilIdle()
                assertTrue(restored.uiState.value.evening)
                assertEquals(ReaderUiState.TextSize.LARGE, restored.uiState.value.textSize)
                assertNull(restored.uiState.value.overlay)
                vm.dispatch(ReaderAction.NewAdventure)
                vm.dispatch(ReaderAction.NewAdventure)
                assertEquals(ReaderDestination.NewAdventure(progress.configuration.cast.map { it.id }), vm.navigator.commands.first())
                vm.resumed()
                vm.dispatch(ReaderAction.Library)
                assertEquals(ReaderDestination.Library, vm.navigator.commands.first())
            } finally {
                Dispatchers.resetMain()
            }
        }
}
