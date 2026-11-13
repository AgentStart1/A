/*
 * This is a private project. All rights reserved.
 */

@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.storyteller_f.alphaheart.app.common

import com.storyteller_f.shared.commonJson
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.await
import kotlinx.coroutines.withContext
import kotlin.js.JsAny
import kotlin.js.JsString
import kotlin.js.Promise
import kotlin.js.toJsString

@JsModule("download-file-worker")
private external fun startExtraction(path: JsString, directory: JsString): DownloadWorkerJob

internal external interface DownloadWorkerJob : JsAny {
    fun result(): Promise<JsString>
    fun close(): Promise<JsAny?>
}

/** Only metadata crosses the boundary; the worker reads and writes OPFS directly. */
internal suspend fun extractArchiveInWorker(path: String, directory: String): List<DownloadFileEntry> =
    startExtraction(path.toJsString(), directory.toJsString()).awaitExtraction()

internal suspend fun DownloadWorkerJob.awaitExtraction(): List<DownloadFileEntry> =
    withDownloadCleanup({ withContext(NonCancellable) { close().await() } }) {
        commonJson.decodeFromString<List<DownloadFileEntry>>(result().await().toString())
    }
