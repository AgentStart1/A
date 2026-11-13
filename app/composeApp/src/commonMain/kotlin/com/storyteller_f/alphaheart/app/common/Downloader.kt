/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.common

import com.storyteller_f.alphaheart.client.core.UserSessionManager
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.shared.utils.md5
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface Downloader {
    /** True through admission, execution and pause cleanup; terminal controls wait for false. */
    fun observeTask(id: PrimaryKey): Flow<Boolean>

    /** Register once or reuse; return null for an active duplicate. Retry failed/paused tasks through [resume]. */
    suspend fun download(fileInfo: FileInfo): DownloadInfo?
    fun resume(id: PrimaryKey)

    /** Resume with an updated transport URL only when the supplied file matches the stored revision. */
    fun resume(fileInfo: FileInfo)
    fun pause(id: PrimaryKey)
}

internal data class DownloadContext(
    val namespace: String,
    val scope: CoroutineScope,
    val records: DownloadInfoStorage,
    val session: suspend () -> DownloadSession,
)

/** Application-owned resource tasks. Account sessions are captured per admission and never retained between tasks. */
class DownloaderImpl internal constructor(
    private val context: DownloadContext,
    private val register: TaskRegister,
    private val files: FileEngine,
) : Downloader {
    override fun observeTask(id: PrimaryKey): Flow<Boolean> =
        register.observeTask(downloadTaskKey(context.namespace, id))

    override suspend fun download(fileInfo: FileInfo): DownloadInfo? = start(fileInfo)

    internal suspend fun start(file: FileInfo): DownloadInfo? {
        currentCoroutineContext().ensureActive()
        val prepared = admit(file.id, retry = false) { file }
        return try {
            val result = prepared.await()
            currentCoroutineContext().ensureActive()
            context.scope.coroutineContext.ensureActive()
            result.getOrThrow()
        } catch (stopped: TaskStoppedException) {
            currentCoroutineContext().ensureActive()
            context.scope.coroutineContext.ensureActive()
            throw IllegalStateException("Download request was paused", stopped)
        } finally {
            if (!currentCoroutineContext().isActive) prepared.cancel()
        }
    }

    override fun resume(id: PrimaryKey) {
        admit(id, retry = true) { context.records.getDocumentByFileId(id)?.fileInfo }
    }

    override fun resume(fileInfo: FileInfo) {
        admit(fileInfo.id, retry = true) {
            context.records.getDocumentByFileId(fileInfo.id)?.fileInfo?.let { current ->
                if (getDownloadRevision(current) == getDownloadRevision(fileInfo)) {
                    current.copy(url = fileInfo.url)
                } else {
                    current
                }
            }
        }
    }

    private fun admit(
        id: PrimaryKey,
        retry: Boolean,
        resolveFile: suspend () -> FileInfo?,
    ): Deferred<Result<DownloadInfo?>> {
        val prepared = CompletableDeferred<Result<DownloadInfo?>>()
        val key = downloadTaskKey(context.namespace, id)
        val job =
            context.scope.launch(start = CoroutineStart.UNDISPATCHED) {
                try {
                    var request: DownloadSession? = null
                    withDownloadCleanup({ request?.close() }) {
                        register.runTask(key, prepare = { request = context.session() }) {
                            executeDownload(id, retry, prepared, checkNotNull(request).session, resolveFile)
                        }
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
        prepared.invokeOnCompletion { cause ->
            if (prepared.isCancelled) job.cancel(cause as? CancellationException)
        }
        job.invokeOnCompletion { cause ->
            if (cause != null) prepared.complete(Result.failure(cause))
        }
        return prepared
    }

    private suspend fun executeDownload(
        id: PrimaryKey,
        retry: Boolean,
        prepared: CompletableDeferred<Result<DownloadInfo?>>,
        session: UserSessionManager,
        resolveFile: suspend () -> FileInfo?,
    ) {
        files.withLock(downloadTaskKey(context.namespace, id)) {
            val file = resolveFile() ?: return@withLock
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
                val record = prepare(file, retry)
                prepared.complete(Result.success(record))
                if (record.status == DownloadStatus.NOT_DOWNLOADED || record.status == DownloadStatus.DOWNLOADED) {
                    process(record, session)
                }
            }
        }
    }

    override fun pause(id: PrimaryKey) {
        register.stop(downloadTaskKey(context.namespace, id)) {
            try {
                files.withLock(downloadTaskKey(context.namespace, id)) {
                    val current = context.records.getDocumentByFileId(id)
                    if (current != null && current.status != DownloadStatus.PROCESSED) {
                        context.records.save(current.copy(status = DownloadStatus.PAUSED))
                    }
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
                val path =
                    files.location(
                        "public-resources-v1/${context.namespace}/${file.id}/${getDownloadRevision(file)}/source",
                    )
                createDownloadRecord(
                    file,
                    path,
                ).let {
                    if (existing == null) {
                        it
                    } else {
                        it.copy(
                            id = existing.id,
                            processedGeneration = existing.processedGeneration,
                        )
                    }
                }
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
                current.status == DownloadStatus.PROCESSED && sourcePresent && archivePresent && !retry -> current

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

    private suspend fun process(record: DownloadInfo, session: UserSessionManager) {
        val file = record.fileInfo
        if (files.size(record.path) != file.size) {
            val source = session.refreshExpiredDownloadUrl(file)
            update(file) { it.copy(fileInfo = source, status = DownloadStatus.DOWNLOADING) }
            transferDownload(
                session,
                files,
                record.path,
                source,
            ) { count -> update(file) { it.copy(progress = count) } }
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
        update(
            file,
        ) { it.copy(status = DownloadStatus.PROCESSED, message = "", processedGeneration = it.processedGeneration + 1) }
    }

    private suspend fun update(file: FileInfo, change: (DownloadInfo) -> DownloadInfo) {
        val current = context.records.getDocumentByFileId(file.id) ?: return
        if (getDownloadRevision(current.fileInfo) == getDownloadRevision(file)) context.records.save(change(current))
    }
}

internal fun downloadTaskKey(namespace: String, id: PrimaryKey): String = "download:$namespace:$id"
internal fun FileInfo.isDownloadArchive(): Boolean = name.endsWith(".zip", ignoreCase = true)

internal fun getDownloadRevision(file: FileInfo): String =
    md5("${file.fullName}:${file.lastModified}:${file.size}:${file.name}")

internal fun createDownloadRecord(file: FileInfo, path: String): DownloadInfo =
    DownloadInfo(file.id, file, com.storyteller_f.storage.DownloadStatus.NOT_DOWNLOADED, "", path, 0, file.size)
