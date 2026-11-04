/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.a.app.common

import com.storyteller_f.a.client.core.UserSessionManager
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.storage.DownloadInfo
import com.storyteller_f.storage.DownloadInfoStorage
import com.storyteller_f.storage.DownloadStatus
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal data class DownloadContext(
    val namespace: String,
    val scope: CoroutineScope,
    val records: DownloadInfoStorage,
    val session: UserSessionManager,
)

/** The account owns transfer jobs; callers await registration only, and observe persisted status afterwards. */
internal class DownloadEngine(
    private val context: DownloadContext,
    private val register: TaskRegister,
    private val files: DownloadFileStore,
) {
    suspend fun start(file: FileInfo, retry: Boolean = false): DownloadInfo =
        try {
            currentCoroutineContext().ensureActive()
            val result = enqueue(file.id, retry, merge = false) { file }.await()
            currentCoroutineContext().ensureActive()
            context.scope.coroutineContext.ensureActive()
            checkNotNull(result.getOrThrow())
        } catch (stopped: TaskStoppedException) {
            // Explicit pause stops the operation; an active observer must still be able to render failure/retry.
            currentCoroutineContext().ensureActive()
            context.scope.coroutineContext.ensureActive()
            throw IllegalStateException("Download request was paused", stopped)
        }

    fun resume(id: PrimaryKey) {
        enqueue(id, retry = true, merge = true) { context.records.getDocumentByFileId(id)?.fileInfo }
    }

    private fun enqueue(
        id: PrimaryKey,
        retry: Boolean,
        merge: Boolean,
        resolveFile: suspend () -> FileInfo?,
    ): Deferred<Result<DownloadInfo?>> {
        val prepared = CompletableDeferred<Result<DownloadInfo?>>()
        val key = downloadTaskKey(context.namespace, id)
        val job =
            context.scope.launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    register.runTask(key, merge) {
                        resolveFile()?.let { executeDownload(it, retry, prepared) }
                    }
                    prepared.complete(Result.success(null))
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (fatal: Error) {
                    throw fatal
                } catch (failure: Throwable) {
                    prepared.complete(Result.failure(failure))
                    Napier.w(failure) { "Download request failed" }
                }
            }
        job.invokeOnCompletion { cause ->
            if (cause != null) prepared.complete(Result.failure(cause))
        }
        return prepared
    }

    private suspend fun executeDownload(
        file: FileInfo,
        retry: Boolean,
        prepared: CompletableDeferred<Result<DownloadInfo?>>,
    ) {
        withDownloadFinalizer({ failure ->
            when (failure) {
                is CancellationException -> {
                    completeDownloadCleanup(failure) {
                        if (context.scope.isActive) {
                            withContext(NonCancellable) { update(file) { it.copy(status = DownloadStatus.PAUSED) } }
                        }
                    }
                    // Keep post-cleanup diagnostics as data before the job canonicalizes its cancellation cause.
                    prepared.complete(Result.failure(failure))
                }

                null, is Error -> Unit

                else ->
                    update(file) {
                        it.copy(status = DownloadStatus.DOWNLOAD_FAILED, message = failure.message.orEmpty())
                    }
            }
        }) {
            files.withLock(downloadTaskKey(context.namespace, file.id)) {
                val record = prepare(file, retry)
                prepared.complete(Result.success(record))
                if (record.status == DownloadStatus.NOT_DOWNLOADED || record.status == DownloadStatus.DOWNLOADED) {
                    process(record)
                }
            }
        }
    }

    fun pause(id: PrimaryKey) {
        register.stop(downloadTaskKey(context.namespace, id)) {
            try {
                val current = context.records.getDocumentByFileId(id)
                if (current != null && current.status != DownloadStatus.PROCESSED) {
                    context.records.save(current.copy(status = DownloadStatus.PAUSED))
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (fatal: Error) {
                throw fatal
            } catch (failure: Throwable) {
                Napier.w(failure) { "Unable to publish paused download state" }
            }
        }
    }

    private suspend fun prepare(file: FileInfo, retry: Boolean): DownloadInfo {
        require(file.size >= 0) { "Invalid download size" }
        val existing = context.records.getDocumentByFileId(file.id)
        val current =
            if (existing == null || getDownloadRevision(existing.fileInfo) != getDownloadRevision(file)) {
                val path = files.location("${context.namespace}/${file.id}/${getDownloadRevision(file)}/source")
                createDownloadRecord(file, path).let { if (existing == null) it else it.copy(id = existing.id) }
            } else {
                existing.copy(fileInfo = file)
            }
        val sourcePresent = files.size(current.path) == file.size
        val archivePresent =
            if (file.isDownloadArchive()) {
                sourcePresent && files.extractedFiles(current.path) != null
            } else {
                true
            }
        val next =
            when {
                current.status == DownloadStatus.PROCESSED && sourcePresent && archivePresent -> current

                current.status == DownloadStatus.PROCESSED ->
                    current.copy(
                        status = if (sourcePresent) DownloadStatus.DOWNLOADED else DownloadStatus.NOT_DOWNLOADED,
                    )

                retry || current.status == DownloadStatus.DOWNLOADED ->
                    current.copy(
                        status = if (sourcePresent) DownloadStatus.DOWNLOADED else DownloadStatus.NOT_DOWNLOADED,
                    )

                current.status == DownloadStatus.DOWNLOADING -> current.copy(status = DownloadStatus.PAUSED)

                else -> current
            }
        context.records.save(next)
        return next
    }

    private suspend fun process(record: DownloadInfo) {
        val file = record.fileInfo
        if (files.size(record.path) != file.size) {
            update(file) { it.copy(status = DownloadStatus.DOWNLOADING) }
            transferDownload(context, files, record.path, file) { count -> update(file) { it.copy(progress = count) } }
        }
        check(files.size(record.path) == file.size) { "Downloaded file size mismatch" }
        update(file) { it.copy(status = DownloadStatus.DOWNLOADED, progress = file.size) }
        try {
            if (file.isDownloadArchive()) files.extractArchive(record.path)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (fatal: Error) {
            throw fatal
        } catch (failure: Throwable) {
            update(file) { it.copy(status = DownloadStatus.PROCESS_FAILED, message = failure.message.orEmpty()) }
            return
        }
        currentCoroutineContext().ensureActive()
        update(file) { it.copy(status = DownloadStatus.PROCESSED, message = "") }
    }

    private suspend fun update(file: FileInfo, change: (DownloadInfo) -> DownloadInfo) {
        val current = context.records.getDocumentByFileId(file.id) ?: return
        if (getDownloadRevision(current.fileInfo) == getDownloadRevision(file)) context.records.save(change(current))
    }
}

internal fun downloadTaskKey(namespace: String, id: PrimaryKey): String = "download:$namespace:$id"
internal fun FileInfo.isDownloadArchive(): Boolean = name.endsWith(".zip", ignoreCase = true)
