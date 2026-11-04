/*
 * This is a private project. All rights reserved.
 */

@file:OptIn(ExperimentalWasmJsInterop::class)

package fonts

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.await
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.JsNumber
import kotlin.js.JsString
import kotlin.js.Promise
import kotlin.js.toJsString

internal suspend fun waitForPendingDownloadLock(key: String) =
    withContext(Dispatchers.Default) {
        withTimeout(5000) {
            while (lockProbe().pending(key.toJsString()).await().toDouble().toInt() != 1) delay(1)
        }
    }

internal suspend fun verifyDownloadWorkerLock(key: String, release: suspend () -> Unit) =
    withContext(
        Dispatchers.Default,
    ) {
        val worker = lockProbe().worker(key.toJsString())
        try {
            waitForPendingDownloadLock(key)
            release()
            withTimeout(5000) { worker.acquired().await() }
        } finally {
            worker.close()
        }
    }

private external interface DownloadWorkerLock : JsAny {
    fun acquired(): Promise<JsAny?>
    fun close()
}

private external interface DownloadLockProbe : JsAny {
    fun pending(key: JsString): Promise<JsNumber>
    fun worker(key: JsString): DownloadWorkerLock
}

private fun lockProbe(): DownloadLockProbe =
    js(
        """({
        async pending(key) {
            const state = await navigator.locks.query();
            return state.pending.filter(item => item.name === 'a-downloads:' + key).length;
        },
        worker(key) {
            const source = "onmessage = event => navigator.locks.request('a-downloads:' + event.data, async () => {postMessage('acquired'); await new Promise(() => {});});";
            const url = URL.createObjectURL(new Blob([source], {type: 'text/javascript'}));
            const worker = new Worker(url);
            const entered = new Promise((resolve, reject) => {
                worker.onmessage = () => resolve();
                worker.onerror = event => reject(new Error(event.message));
            });
            worker.postMessage(key);
            return {acquired: () => entered, close() {worker.terminate(); URL.revokeObjectURL(url);}};
        }
    })""",
    )
