package com.dozy.mobile

import androidx.lifecycle.SavedStateHandle
import com.dozy.mobile.feature.story.data.*
import com.dozy.mobile.feature.story.data.api.*
import com.dozy.mobile.feature.story.domain.model.*
import com.dozy.mobile.feature.story.domain.usecase.ReadStory
import com.dozy.mobile.feature.story.presentation.reader.*
import com.kjursa.foundation.compose.mvi.SavedStatePersistence
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.Assert.*
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
                val read = ReadStory(DefaultGenerationRepository(FakeGenerationDao(), api))
                val handle = SavedStateHandle()
                val persistence = SavedStatePersistence(handle, ReaderUiState.serializer())
                val vm = ReaderViewModel(id, read, persistence)
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
                val restored = ReaderViewModel(id, read, persistence)
                advanceUntilIdle()
                assertTrue(restored.uiState.value.evening)
                assertEquals(ReaderUiState.TextSize.LARGE, restored.uiState.value.textSize)
                assertNull(restored.uiState.value.overlay)
                vm.dispatch(ReaderAction.NewAdventure)
                vm.dispatch(ReaderAction.NewAdventure)
                assertEquals(ReaderEffect.NewAdventure(progress.configuration.cast.map { it.id }), vm.effects.first())
                vm.resumed()
                vm.dispatch(ReaderAction.Library)
                assertEquals(ReaderEffect.Library, vm.effects.first())
            } finally {
                Dispatchers.resetMain()
            }
        }
}
