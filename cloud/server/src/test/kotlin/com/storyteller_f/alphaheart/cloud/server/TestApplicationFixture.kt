/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server

import io.ktor.server.testing.TestApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

// The block-based Ktor helper does not reach stop when its block throws.
internal suspend fun <T> useTestHost(application: TestApplication, block: suspend (TestApplication) -> T): T =
    withCleanupPreservingFailure(
        cleanup = { withContext(NonCancellable + Dispatchers.IO) { application.stop() } },
    ) {
        application.start()
        block(application)
    }

internal inline fun <T> withCleanupPreservingFailure(cleanup: () -> Unit, block: () -> T): T {
    var primary: Throwable? = null
    try {
        return block()
    } catch (failure: Throwable) {
        primary = failure
        throw failure
    } finally {
        val original = primary
        if (original == null) cleanup() else cleanupAfterFailure(original, cleanup)
    }
}

// Like Closeable.use: a failing body owns the failure; cleanup can only add a suppressed cause.
private inline fun cleanupAfterFailure(primary: Throwable, cleanup: () -> Unit) {
    try {
        cleanup()
    } catch (failure: Throwable) {
        if (failure !== primary) primary.addSuppressed(failure)
    }
}
