package com.kjursa.foundation.coroutines

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.fail

@OptIn(ExperimentalCoroutinesApi::class)
class RunCatchingSuspendTest {
    @Test fun returnsValueAfterSuspending() =
        runTest {
            val result =
                runCatchingSuspend {
                    delay(1)
                    "saved"
                }
            assertEquals("saved", result.getOrThrow())
        }

    @Test fun retainsOriginalException() =
        runTest {
            val failure = IllegalStateException("disk full")
            val result = runCatchingSuspend { throw failure }
            assertSame(failure, result.exceptionOrNull())
        }

    @Test fun rethrowsCancellationInsteadOfReturningFailure() =
        runTest {
            val cancelled = CancellationException("cancelled")
            try {
                runCatchingSuspend { throw cancelled }
                fail("Expected cancellation to propagate")
            } catch (actual: CancellationException) {
                assertSame(cancelled, actual)
            }
        }

    @Test fun cancellingSuspendedWorkDoesNotContinueWithAFailureResult() =
        runTest {
            var returned = false
            val job =
                launch {
                    runCatchingSuspend { awaitCancellation() }
                    returned = true
                }
            runCurrent()
            job.cancelAndJoin()
            assertFalse(returned)
        }

    @Test fun doesNotConvertUnrecoverableErrorsToFailures() =
        runTest {
            val error = AssertionError("invariant broken")
            try {
                runCatchingSuspend { throw error }
                fail("Expected error to propagate")
            } catch (actual: AssertionError) {
                assertSame(error, actual)
            }
        }
}
