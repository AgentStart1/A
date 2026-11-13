/*
 * This is a private project. All rights reserved.
 */

package fonts

import com.storyteller_f.alphaheart.app.common.SimpleTaskRegister
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FontTaskRegisterTest {
    @Test
    fun duplicatesReturnWithoutPreparingOrQueuing() =
        runTest {
            val register = SimpleTaskRegister(this)
            val entered = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            var executions = 0
            val owner =
                async {
                    register.runTask("font") {
                        executions++
                        entered.complete(Unit)
                        release.await()
                    }
                }
            entered.await()
            assertFalse(
                register.runTask("font", prepare = { error("Duplicate must not capture a client") }) {
                    executions++
                },
            )
            register.lockTask("font") { executions++ }
            release.complete(Unit)
            assertTrue(owner.await())
            runCurrent()
            assertEquals(1, executions)
            assertEquals(0, register.runningTaskCount.value)
        }

    @Test
    fun duplicatesAreIgnoredDuringSessionPreparation() =
        runTest {
            val register = SimpleTaskRegister(this)
            val entered = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val owner =
                async {
                    register.runTask(
                        "font",
                        prepare = {
                            entered.complete(Unit)
                            release.await()
                        },
                    ) {}
                }
            entered.await()
            assertFalse(register.runTask("font") { error("Duplicate must not run") })
            release.complete(Unit)
            assertTrue(owner.await())
        }

    @Test
    fun nestedDuplicateIsIgnoredWithoutWaitingForItself() =
        runTest {
            val register = SimpleTaskRegister(this)
            assertTrue(
                register.runTask("font") {
                    assertFalse(register.runTask("font") { error("Nested duplicate must not run") })
                },
            )
            assertEquals(0, register.runningTaskCount.value)
        }

    @Test
    fun independentFilesExecuteConcurrently() =
        runTest {
            val register = SimpleTaskRegister(this)
            val release = CompletableDeferred<Unit>()
            val first = async { register.runTask("first") { release.await() } }
            val second = async { register.runTask("second") { release.await() } }
            runCurrent()
            assertEquals(2, register.runningTaskCount.value)
            assertFalse(first.isCompleted)
            assertFalse(second.isCompleted)
            release.complete(Unit)
            assertTrue(first.await())
            assertTrue(second.await())
            assertEquals(0, register.runningTaskCount.value)
        }

    @Test
    fun cancelledDownloadReleasesItsRegistration() =
        runTest {
            val register = SimpleTaskRegister(this)
            val started = CompletableDeferred<Unit>()
            val owner =
                async {
                    register.runTask("font") {
                        started.complete(Unit)
                        awaitCancellation()
                    }
                }
            started.await()
            owner.cancelAndJoin()
            assertEquals(0, register.runningTaskCount.value)
            assertTrue(register.runTask("font") {})
        }

    @Test
    fun pauseWaitsForCleanupAndRejectsConcurrentAdmission() =
        runTest {
            val register = SimpleTaskRegister(this)
            val entered = CompletableDeferred<Unit>()
            val cleaning = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val paused = CompletableDeferred<Unit>()
            register.lockTask("download") {
                try {
                    entered.complete(Unit)
                    awaitCancellation()
                } finally {
                    withContext(NonCancellable) {
                        cleaning.complete(Unit)
                        release.await()
                    }
                }
            }
            entered.await()
            register.stop("download") { paused.complete(Unit) }
            cleaning.await()
            assertFalse(paused.isCompleted)
            assertFalse(register.runTask("download") { error("Pause must own the key") })
            release.complete(Unit)
            paused.await()
            runCurrent()
            assertEquals(0, register.runningTaskCount.value)
            assertTrue(register.runTask("download") {})
        }

    @Test
    fun requestsDuringPausePublicationAreNotQueued() =
        runTest {
            val register = SimpleTaskRegister(this)
            val pausing = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            register.stop("download") {
                pausing.complete(Unit)
                release.await()
            }
            pausing.await()
            var executions = 0
            assertFalse(register.runTask("download") { executions++ })
            release.complete(Unit)
            runCurrent()
            assertEquals(0, executions)
            assertTrue(register.runTask("download") { executions++ })
            assertEquals(1, executions)
        }
}
