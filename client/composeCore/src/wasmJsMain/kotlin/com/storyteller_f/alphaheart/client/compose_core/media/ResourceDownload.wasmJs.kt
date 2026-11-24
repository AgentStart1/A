/*
 * This is a private project. All rights reserved.
 */

@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.storyteller_f.alphaheart.client.compose_core.media

import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.await
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.js.JsAny
import kotlin.js.JsException
import kotlin.js.Promise

actual suspend fun saveRemoteResource(
    client: HttpClient,
    resource: ResourceDownloadRequest,
    dialogSettings: FileKitDialogSettings,
): ResourceSaveOutcome = downloadBrowserResource(resource)

/** Browser-native Blob buffering avoids a second complete copy in Kotlin/Wasm memory. */
internal suspend fun downloadBrowserResource(resource: ResourceDownloadRequest): ResourceSaveOutcome {
    currentCoroutineContext().ensureActive()
    val operation =
        try {
            startBrowserResourceDownload(resource.url, resource.fileName, resource.expectedSize?.toDouble() ?: -1.0)
        } catch (_: JsException) {
            throw IllegalStateException("Browser resource download is unavailable")
        }
    try {
        operation.result.await()
        if (operation.failed) throw IllegalStateException(operation.failureMessage)
        return ResourceSaveOutcome.HANDED_OFF
    } catch (cancelled: CancellationException) {
        operation.cancel()
        throw cancelled
    } catch (_: JsException) {
        currentCoroutineContext().ensureActive()
        // Only locally generated status codes cross this boundary; never retain a fetch error or signed URL.
        throw IllegalStateException(operation.failureMessage)
    } finally {
        operation.cancel()
        withContext(NonCancellable) { operation.settled.await() }
    }
}

private external interface BrowserResourceDownload : JsAny {
    val result: Promise<JsAny?>
    val settled: Promise<JsAny?>
    val failureMessage: String
    val failed: Boolean
    fun cancel()
}

@JsFun(
    """(url, fileName, expectedSize) => {
        const controller = new AbortController();
        let anchor = null;
        let objectUrl = null;
        const operation = {
            failed: false,
            failureMessage: 'Resource download failed',
            cancel: () => controller.abort()
        };
        const release = () => {
            if (anchor !== null) { anchor.remove(); anchor = null; }
            if (objectUrl !== null) { URL.revokeObjectURL(objectUrl); objectUrl = null; }
        };
        operation.result = (async () => {
            try {
                const response = await fetch(url, {signal: controller.signal});
                if (response.status !== 200) {
                    operation.failureMessage = 'Resource download HTTP ' + response.status;
                    throw new Error('http');
                }
                const blob = await response.blob();
                if (controller.signal.aborted) throw new DOMException('Cancelled', 'AbortError');
                if (expectedSize >= 0 && blob.size !== expectedSize) {
                    operation.failureMessage = 'Downloaded resource size does not match';
                    throw new Error('size');
                }
                objectUrl = URL.createObjectURL(blob);
                anchor = document.createElement('a');
                anchor.href = objectUrl;
                anchor.download = fileName || 'download';
                anchor.style.display = 'none';
                document.body.append(anchor);
                anchor.click();
                // The navigation task captures the Blob before we release our URL and DOM ownership.
                await new Promise(resolve => setTimeout(resolve, 0));
            } finally {
                release();
            }
        })().then(() => undefined, () => {
            // Promise.await may wrap a JS rejection as a Kotlin Throwable before catch(JsException).
            // Keep raw fetch/handoff errors entirely inside JavaScript, including any signed URL.
            operation.failed = true;
        });
        // Cleanup remains awaitable even when the owning Kotlin coroutine was cancelled.
        operation.settled = operation.result;
        return operation;
    }""",
)
private external fun startBrowserResourceDownload(
    url: String,
    fileName: String,
    expectedSize: Double,
): BrowserResourceDownload
