/*
 * This is a private project. All rights reserved.
 */

package com.storytellerf.alphaheart.e2e

import com.storyteller_f.alphaheart.test.createTestArtifactDirectory
import com.storyteller_f.alphaheart.test.recordTestArtifacts
import com.storyteller_f.alphaheart.test.testArtifactRoot
import com.storyteller_f.alphaheart.test.useBackendFixture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import kotlin.time.Duration.Companion.minutes

/** Mapped host ports of the HTTP and WebSocket services started for a CLI E2E test. */
class E2ePorts(server: Int, ws: Int, internal val artifactDirectory: File? = null) {
    /** HTTP server port on the test host. */
    val server: Int = server

    /** WebSocket server port on the test host. */
    val ws: Int = ws
}

/** Run a suspending CLI E2E test from JUnit's synchronous test boundary. */
fun runE2eBlockingTest(block: suspend CoroutineScope.() -> Unit) {
    runBlocking {
        withTimeout(10.minutes) {
            block()
        }
    }
}

/** Start an isolated full backend and retain service logs under the module build directory. */
suspend fun runE2eTestEnvironment(suite: String, testName: String, block: suspend (E2ePorts) -> Unit) {
    val directory = createTestArtifactDirectory(testArtifactRoot(), suite, testName)
    recordTestArtifacts(directory, suite, testName) {
        useBackendFixture(directory.resolve("logs")) { backend ->
            block(E2ePorts(backend.serverPort, backend.wsPort, directory))
        }
    }
}
