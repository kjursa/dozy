package com.dozy.mobile

import com.dozy.mobile.core.config.ApiConfig
import com.dozy.mobile.feature.story.data.DefaultGenerationRepository
import com.dozy.mobile.feature.story.data.api.*
import com.dozy.mobile.feature.story.data.toDto
import com.dozy.mobile.feature.story.domain.model.*
import com.dozy.mobile.feature.story.domain.usecase.*
import com.dozy.mobile.feature.story.presentation.generation.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import kotlinx.serialization.json.Json
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class GenerationProgressTest {
    private val key = GenerationJob.RequestKey("request")
    private val id = GenerationJob.Id("job")
    private val keys = GenerationKeyGenerator { GenerationJob.RequestKey("retry") }

    @Test fun stagesResumeOfflineDoesNotFailAndReadyIsAtomicAndIdempotent() =
        runTest {
            val dao = FakeGenerationDao()
            val api = ProgressApi()
            val repository = DefaultGenerationRepository(dao, api)
            repository.start(key, api.configuration)
            api.remote = GenerationStatusDto("job", "CREATING_TEXT")
            assertEquals(GenerationJob.Status.CREATING_TEXT, repository.refresh(id)?.status)
            api.offline = true
            val offline = repository.refresh(id)!!
            assertTrue(offline.statusUnknown)
            assertEquals(GenerationJob.Status.CREATING_TEXT, offline.status)
            val resumed = DefaultGenerationRepository(dao, api)
            assertTrue(
                resumed
                    .observe()
                    .first()
                    .single()
                    .statusUnknown,
            )
            api.offline = false
            api.remote = GenerationStatusDto("job", "CREATING_COVER")
            assertFalse(resumed.refresh(id)!!.statusUnknown)
            api.remote = GenerationStatusDto("job", "READY", "story")
            api.storyUnavailable = true
            assertEquals(GenerationJob.Status.CREATING_COVER, resumed.refresh(id)?.status)
            assertTrue(resumed.stories().first().isEmpty())
            api.storyUnavailable = false
            assertEquals(Story.Id("story"), resumed.refresh(id)?.storyId)
            repeat(3) { resumed.refresh(id) }
            assertEquals(1, api.starts)
            assertEquals(2, api.storyReads)
            assertEquals(1, resumed.stories().first().size)
            assertEquals(
                api.configuration,
                resumed
                    .stories()
                    .first()
                    .single()
                    .configuration,
            )
        }

    @Test fun retryPersistsKeyAcrossUncertaintyAndRecreationAndKeepsConfiguration() =
        runTest {
            val dao = FakeGenerationDao()
            val api = ProgressApi()
            val repository = DefaultGenerationRepository(dao, api)
            repository.start(key, api.configuration)
            api.remote = GenerationStatusDto("job", "FAILED", errorCode = "GENERATION_FAILED")
            repository.refresh(id)
            val snapshot = dao.find(key.value)!!.configuration
            api.loseRetryResponse = true
            assertEquals(RetryGeneration.Result.Unavailable, RetryGeneration(repository, keys)(id))
            assertEquals("retry", dao.find(key.value)?.retryKey)
            val recreated = DefaultGenerationRepository(dao, api)
            api.loseRetryResponse = false
            val retried = RetryGeneration(recreated, GenerationKeyGenerator { GenerationJob.RequestKey("new-key") })(id)
            assertTrue(retried is RetryGeneration.Result.Accepted)
            recreated.retry(id, GenerationJob.RequestKey("duplicate"))
            assertEquals(listOf("retry", "retry"), api.retryKeys)
            assertEquals(snapshot, dao.find(key.value)?.configuration)
            assertEquals(
                GenerationJob.Status.QUEUED,
                recreated
                    .observe()
                    .first()
                    .single()
                    .status,
            )
            assertEquals(1, api.starts)
        }

    @Test fun backgroundReconciliationResumesPendingRetryAndNotificationDenialKeepsHomeResult() =
        runTest {
            val api = ProgressApi()
            val dao = FakeGenerationDao()
            var repository = DefaultGenerationRepository(dao, api)
            repository.start(key, api.configuration)
            api.remote = GenerationStatusDto("job", "FAILED", errorCode = "GENERATION_FAILED")
            repository.refresh(id)
            api.loseRetryResponse = true
            RetryGeneration(repository, keys)(id)
            repository = DefaultGenerationRepository(dao, api)
            api.loseRetryResponse = false
            var permitted = false
            val notifications = mutableListOf<Story.Id>()
            val reconcile =
                ReconcileGenerations(
                    repository,
                    RetryGeneration(repository, keys),
                    StoryCompletionNotifier {
                        if (permitted) notifications += it
                        permitted
                    },
                )
            assertEquals(ReconcileGenerations.Result.Pending, reconcile())
            api.remote = GenerationStatusDto("job", "READY", "story")
            assertEquals(ReconcileGenerations.Result.Settled, reconcile())
            assertEquals(
                Story.Id("story"),
                repository
                    .observe()
                    .first()
                    .single()
                    .storyId,
            )
            assertFalse(
                repository
                    .observe()
                    .first()
                    .single()
                    .notificationDelivered,
            )
            permitted = true
            reconcile()
            reconcile()
            assertEquals(listOf(Story.Id("story")), notifications)
            assertTrue(
                repository
                    .observe()
                    .first()
                    .single()
                    .notificationDelivered,
            )
            assertEquals(1, api.starts)
        }

    @Test fun onlyOneActiveJobAndCompletedJobCannotBeRetried() =
        runTest {
            val api = ProgressApi()
            val repository = DefaultGenerationRepository(FakeGenerationDao(), api)
            repository.start(key, api.configuration)
            try {
                repository.start(GenerationJob.RequestKey("another"), api.configuration)
                fail("Another active job must not be started")
            } catch (_: IllegalStateException) {
            }
            assertEquals(1, api.starts)
            api.remote = GenerationStatusDto("job", "READY", "story")
            repository.refresh(id)
            repository.retry(id, key)
            assertTrue(api.retryKeys.isEmpty())
        }

    @Test fun viewModelBlocksRepeatedRetryAndEmitsSpecificReaderEffect() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val scope = androidx.lifecycle.ViewModelStore()
            try {
                val api = ProgressApi()
                val repository = DefaultGenerationRepository(FakeGenerationDao(), api)
                repository.start(key, api.configuration)
                api.remote = GenerationStatusDto("job", "FAILED", errorCode = "GENERATION_FAILED")
                repository.refresh(id)
                val vm =
                    GenerationViewModel(ObserveGenerations(repository), RefreshGeneration(repository), RetryGeneration(repository, keys))
                scope.put("progress", vm)
                runCurrent()
                api.retryGate = CompletableDeferred()
                vm.dispatch(GenerationAction.Retry(id))
                vm.dispatch(GenerationAction.Retry(id))
                runCurrent()
                assertTrue(vm.uiState.value.busy)
                assertEquals(1, api.retryKeys.size)
                api.retryGate!!.complete(Unit)
                runCurrent()
                assertFalse(vm.uiState.value.busy)
                assertEquals(
                    GenerationJob.Status.QUEUED,
                    vm.uiState.value.jobs
                        .single()
                        .status,
                )
                api.offline = true
                vm.dispatch(GenerationAction.Refresh(id))
                runCurrent()
                assertTrue(
                    vm.uiState.value.jobs
                        .single()
                        .statusUnknown,
                )
                assertEquals(
                    GenerationJob.Status.QUEUED,
                    vm.uiState.value.jobs
                        .single()
                        .status,
                )
                api.offline = false
                api.remote = GenerationStatusDto("job", "READY", "story")
                vm.dispatch(GenerationAction.Refresh(id))
                runCurrent()
                vm.dispatch(GenerationAction.Open(id))
                assertEquals(GenerationEffect.Read(Story.Id("story")), vm.effects.first())
            } finally {
                scope.clear()
                Dispatchers.resetMain()
            }
        }

    @Test fun statusRetryAndResultHttpContractAndDisabledMode() =
        runTest {
            val expected = mutableListOf("GET /api/v1/story-generations/job", "POST /api/v1/story-generations/job/retry", "GET /api/v1/stories/story")
            val dto = StoryDto("story", "Title", "Text", "https://example.test/cover.png", ProgressApi().configuration.toDto())
            val http =
                HttpClient(
                    MockEngine { request ->
                        assertEquals(expected.removeAt(0), "${request.method.value} ${request.url.encodedPath}")
                        when (request.url.encodedPath) {
                            "/api/v1/story-generations/job" ->
                                respond(
                                    """{"jobId":"job","status":"READY","storyId":"story"}""",
                                    HttpStatusCode.OK,
                                    headersOf(HttpHeaders.ContentType, "application/json"),
                                )
                            "/api/v1/story-generations/job/retry" -> {
                                assertEquals("retry-key", request.headers["Idempotency-Key"])
                                respond(
                                    """{"jobId":"job","status":"QUEUED"}""",
                                    HttpStatusCode.Accepted,
                                    headersOf(HttpHeaders.ContentType, "application/json"),
                                )
                            }
                            else ->
                                respond(
                                    Json.encodeToString(dto),
                                    HttpStatusCode.OK,
                                    headersOf(HttpHeaders.ContentType, "application/json"),
                                )
                        }
                    },
                ) { install(ContentNegotiation) { json() } }
            try {
                val api = KtorGenerationApi(http, ApiConfig("https://example.test/"))
                assertEquals("story", api.status("job")?.storyId)
                assertEquals("QUEUED", api.retry("job", "retry-key")?.status)
                assertEquals(dto, api.story("story"))
                val disabled = KtorGenerationApi(http, ApiConfig(null))
                assertNull(disabled.status("job"))
                assertNull(disabled.retry("job", "key"))
                assertNull(disabled.story("story"))
                assertTrue(expected.isEmpty())
            } finally {
                http.close()
            }
        }

    @Test fun malformedOrUnavailableStatusIsUnknownAndCancellationPropagates() =
        runTest {
            for ((code, body) in listOf(
                HttpStatusCode.NotFound to "{}",
                HttpStatusCode.ServiceUnavailable to "{}",
                HttpStatusCode.OK to """{"jobId":"different","status":"QUEUED"}""",
                HttpStatusCode.OK to """{"jobId":"job","status":"READY"}""",
                HttpStatusCode.OK to """{"jobId":"job","status":"FAILED","errorCode":"UNRECOGNIZED"}""",
            )) {
                val http =
                    HttpClient(
                        MockEngine {
                            respond(body, code, headersOf(HttpHeaders.ContentType, "application/json"))
                        },
                    ) { install(ContentNegotiation) { json() } }
                try {
                    val dao = FakeGenerationDao()
                    val api = ProgressApi()
                    DefaultGenerationRepository(dao, api).start(key, api.configuration)
                    val repository = DefaultGenerationRepository(dao, KtorGenerationApi(http, ApiConfig("https://example.test")))
                    val job = repository.refresh(id)!!
                    assertTrue(job.statusUnknown)
                    assertEquals(GenerationJob.Status.QUEUED, job.status)
                } finally {
                    http.close()
                }
            }
            val http = HttpClient(MockEngine { throw CancellationException("cancelled") })
            try {
                val dao = FakeGenerationDao()
                val api = ProgressApi()
                DefaultGenerationRepository(dao, api).start(key, api.configuration)
                try {
                    DefaultGenerationRepository(dao, KtorGenerationApi(http, ApiConfig("https://example.test"))).refresh(id)
                    fail("Cancellation must propagate")
                } catch (_: CancellationException) {
                }
                assertFalse(dao.find(key.value)!!.statusUnknown)
            } finally {
                http.close()
            }
        }

    @Test fun homeKeepsAnOlderRetriedActiveJobVisibleAboveANewerReadyJob() {
        val active = GenerationJob(id, GenerationJob.Status.QUEUED)
        val ready = GenerationJob(GenerationJob.Id("newer"), GenerationJob.Status.READY, Story.Id("story"))
        assertEquals(active, GenerationUiState(jobs = listOf(active, ready)).homeJob)
        assertEquals(ready, GenerationUiState(jobs = listOf(active.copy(status = GenerationJob.Status.FAILED), ready)).homeJob)
    }

    @Test fun notificationRoutesOnlyExactSpecificStoryIdentifiers() {
        assertEquals("story-42", notificationStoryId("dozy://stories/story-42"))
        listOf(null, "dozy://stories/", "dozy://stories/../other", "https://stories/story-42", "dozy://stories/story?other=1").forEach {
            assertNull(notificationStoryId(it))
        }
    }
}
