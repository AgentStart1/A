/*
 * This is a private project. All rights reserved.
 */

@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package fonts

import com.storyteller_f.a.app.common.DownloadFileStore
import com.storyteller_f.a.app.common.DownloadWorkerJob
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.js.JsAny
import kotlin.js.JsNumber
import kotlin.js.JsString
import kotlin.js.Promise
import kotlin.test.assertTrue

@JsModule("download-file-worker/test/write-probe.mjs")
internal external fun startPausedExtraction(path: JsString, directory: JsString): WritingDownloadWorker

@JsModule("download-file-worker/test/inflight-probe.mjs")
internal external fun startInflightExtraction(path: JsString, directory: JsString): InflightDownloadWorker

internal external interface InflightDownloadWorker : WritingDownloadWorker {
    fun pendingAtCancellation(): Boolean
    fun nativeSettledAfterCancellation(): Boolean
}

internal external interface WritingDownloadWorker : DownloadWorkerJob {
    fun writeObserved(): Promise<WorkerWrite>
    fun closed(): Promise<JsString>
    fun isClosed(): Boolean
}

internal external interface WorkerWrite : JsAny {
    val bytes: JsNumber
    val name: JsString
}

/** Real browser time, independent of runTest's virtual-time scheduler. */
internal suspend fun <T : JsAny?> awaitWorkerProbe(signal: Promise<T>): T =
    withContext(Dispatchers.Default) {
        withTimeout(5000) { signal.await() }
    }

/** Observe ordering without replacing any actual OPFS operation. */
internal class WorkerCleanupProbe(
    private val files: DownloadFileStore,
    private val started: Deferred<WritingDownloadWorker>,
) : DownloadFileStore by files {
    var cleaned = false
        private set

    override suspend fun delete(path: String) {
        if (started.isCompleted) {
            assertTrue(started.await().isClosed(), "Real OPFS writer must close before extraction cleanup")
            files.delete(path)
            cleaned = true
        } else {
            files.delete(path)
        }
    }
}
