/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.common

import com.storyteller_f.alphaheart.api.CustomApi
import com.storyteller_f.alphaheart.app.UIViewModel
import com.storyteller_f.alphaheart.app.error.UploadError
import com.storyteller_f.alphaheart.app.pages.file.getUploadDataFromClipFile
import com.storyteller_f.alphaheart.app.utils.ClientFile
import com.storyteller_f.alphaheart.app.utils.getClientFile
import com.storyteller_f.alphaheart.client.core.SimpleUserSessionManager
import com.storyteller_f.alphaheart.client.core.completeChunkUpload
import com.storyteller_f.alphaheart.client.core.getChunkStatus
import com.storyteller_f.alphaheart.client.core.initChunkUpload
import com.storyteller_f.alphaheart.client.core.upload
import com.storyteller_f.alphaheart.client.core.uploadChunk
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.andThen
import com.storyteller_f.result.captureFailure
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.result.map
import com.storyteller_f.result.mapError
import com.storyteller_f.result.onFailure
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.obj.ob
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.shared.utils.calculateFileChecksums
import com.storyteller_f.shared.utils.md5
import com.storyteller_f.shared.utils.now
import com.storyteller_f.shared.utils.safeMessage
import com.storyteller_f.shared.utils.sha256
import com.storyteller_f.storage.ModelStorage
import com.storyteller_f.storage.UploadCollection
import com.storyteller_f.storage.UploadInfo
import com.storyteller_f.storage.UploadStatus
import io.github.aakira.napier.Napier
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.io.Buffer
import kotlin.time.ExperimentalTime

interface Uploader {
    fun upload(clipData: ImmutableList<ClientFile>)

    fun resume(pathHash: String)
    fun pause(pathHash: String)

    suspend fun forget(pathHash: String)
}

class UploaderImpl(
    val uiViewModel: UIViewModel,
    val taskRegister: TaskRegister,
    private val retainSelectedFiles: Boolean = false,
    private val persistTransportProgress: Boolean = true,
) : Uploader {
    private val selectedFiles = mutableMapOf<String, ClientFile>()
    private val selectedFilesMutex = Mutex()

    override suspend fun forget(pathHash: String) {
        selectedFilesMutex.withLock { selectedFiles.remove(pathHash) }
    }

    private suspend fun releaseCompletedFile(
        modelStorage: ModelStorage,
        collection: UploadCollection,
        pathHash: String,
    ) {
        if (!retainSelectedFiles) return
        val record = modelStorage.upload.getDocument(collection, pathHash)
        if (record == null || record.status == UploadStatus.SUCCESS) forget(pathHash)
    }

    override fun upload(clipData: ImmutableList<ClientFile>) {
        if (clipData.isEmpty()) {
            return
        }
        Napier.i {
            "upload ${
                clipData.joinToString {
                    it.name
                }
            }"
        }
        val instance = uiViewModel.instance.value
        val myUid = instance.sessionManager.model.uid ?: return
        val modelStorage = instance.database
        val userSession = instance.sessionManager
        clipData.forEach {
            taskRegister.lockTask(md5(it.path)) {
                val pathHash = md5(it.path)
                if (retainSelectedFiles) selectedFilesMutex.withLock { selectedFiles[pathHash] = it }
                try {
                    uploadIfNeed(userSession, myUid, it, modelStorage, UploadCollection(myUid))
                    releaseCompletedFile(modelStorage, UploadCollection(myUid), pathHash)
                } catch (cancellation: kotlin.coroutines.cancellation.CancellationException) {
                    throw cancellation
                } catch (e: Exception) {
                    Napier.e(e) {
                        "upload failed ${it.path}"
                    }
                }
            }
        }
    }

    override fun resume(pathHash: String) {
        Napier.i {
            "resume upload $pathHash"
        }
        val instance = uiViewModel.instance.value
        val myUid = instance.sessionManager.model.uid ?: return
        val modelStorage = instance.database
        val userSession = instance.sessionManager
        taskRegister.lockTask(pathHash) {
            try {
                resumeIfNeed(modelStorage, myUid, pathHash, userSession, UploadCollection(myUid))
                releaseCompletedFile(modelStorage, UploadCollection(myUid), pathHash)
            } catch (cancellation: kotlin.coroutines.cancellation.CancellationException) {
                throw cancellation
            } catch (e: Exception) {
                Napier.e(e) {
                    "resume upload $pathHash"
                }
            }
        }
    }

    override fun pause(pathHash: String) {
        Napier.i {
            "pause upload $pathHash"
        }
        val instance = uiViewModel.instance.value
        val myUid = instance.sessionManager.model.uid ?: return
        val modelStorage = instance.database
        taskRegister.stop(pathHash) {
            updateUploadInfo(modelStorage, UploadCollection(myUid), pathHash) {
                it.copy(status = UploadStatus.PAUSED)
            }
        }
    }

    private suspend fun resumeIfNeed(
        modelStorage: ModelStorage,
        myUid: PrimaryKey,
        pathHash: String,
        userSession: SimpleUserSessionManager,
        collection: UploadCollection,
    ) {
        val uploadInfo = modelStorage.upload.getDocument(collection, pathHash)
        if (uploadInfo == null) {
            Napier.i {
                "resume upload $pathHash not found"
            }
            return
        }
        val clientFile =
            selectedFilesMutex.withLock { selectedFiles[pathHash] } ?: getClientFile(uploadInfo.path) ?: return
        upload(userSession, myUid, clientFile, UploadContext(modelStorage, collection, pathHash, uploadInfo))
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun uploadIfNeed(
        userSession: SimpleUserSessionManager,
        myUid: PrimaryKey,
        clipFile: ClientFile,
        modelStorage: ModelStorage,
        collection: UploadCollection,
    ) {
        val pathHash = md5(clipFile.path)
        val existing = modelStorage.upload.getDocument(collection, pathHash)
        if (existing != null) {
            Napier.i {
                "upload $pathHash exists"
            }
            return
        }
        val checksums =
            withContext(Dispatchers.Default) {
                clipFile.source().use { calculateFileChecksums(it) }
            }
        val id =
            now().toInstant(
                TimeZone.UTC,
            ).toEpochMilliseconds()
        val uploadInfo =
            UploadInfo.EMPTY.copy(
                id = id,
                objectId = myUid,
                pathHash = pathHash,
                path = clipFile.path,
                sha256 = checksums.sha256,
                crc32c = checksums.crc32c,
                total = clipFile.size,
                status = UploadStatus.UPLOADING,
                name = clipFile.name,
                contentType = clipFile.contentType.contentType,
                chunkSize = 10L * 1024 * 1024, // 10MB
            )
        modelStorage.upload.saveLast(UploadCollection.fromInfo(uploadInfo), uploadInfo)
        upload(userSession, myUid, clipFile, UploadContext(modelStorage, collection, pathHash, uploadInfo))
    }

    private data class UploadContext(
        val modelStorage: ModelStorage,
        val collection: UploadCollection,
        val pathHash: String,
        val uploadInfo: UploadInfo,
    )

    private suspend fun upload(
        userSession: SimpleUserSessionManager,
        myUid: PrimaryKey,
        clientFile: ClientFile,
        context: UploadContext,
    ) {
        val modelStorage = context.modelStorage
        val collection = context.collection
        val pathHash = context.pathHash
        val uploadInfo = context.uploadInfo
        if (uploadInfo.status != UploadStatus.UPLOADING) {
            updateUploadInfo(modelStorage, collection, pathHash) {
                it.copy(status = UploadStatus.UPLOADING)
            }
        }
        val objectTuple = myUid ob ObjectType.USER
        val chunkThreshold = uploadInfo.chunkSize
        val fileSha256 = uploadInfo.sha256
        if (clientFile.size > chunkThreshold) {
            uploadChunked(userSession, objectTuple, clientFile, modelStorage, uploadInfo, fileSha256)
            return
        }
        val progress =
            UploadProgressWriter(clientFile.size) { sent ->
                updateUploadInfo(modelStorage, collection, pathHash) { it.copy(progress = sent) }
            }
        userSession.upload(
            objectTuple,
            clientFile.getUploadDataFromClipFile(fileSha256),
        ) { sent, _ ->
            if (persistTransportProgress) progress.update(sent)
        }.onSuccess {
            updateUploadInfo(modelStorage, collection, pathHash) {
                it.copy(status = UploadStatus.SUCCESS, progress = it.total)
            }
        }.onFailure { throwable ->
            updateUploadInfo(modelStorage, collection, pathHash) {
                it.copy(status = UploadStatus.FAILED, message = throwable.cause.safeMessage())
            }
        }
    }

    private suspend fun uploadChunked(
        userSession: SimpleUserSessionManager,
        objectTuple: ObjectTuple,
        clipFile: ClientFile,
        modelStorage: ModelStorage,
        uploadInfo: UploadInfo,
        fileSha256: String,
    ) {
        val collection = UploadCollection.fromInfo(uploadInfo)
        val pathHash = uploadInfo.pathHash
        getUploadId(
            modelStorage,
            userSession,
            objectTuple,
            clipFile,
            uploadInfo,
            fileSha256,
        ).andThen(UploadError::fromError) {
            userSession.getChunkStatus(it)
        }.andThen(UploadError::fromError) {
            uploadChunkedFiles(userSession, it.id, clipFile, modelStorage, uploadInfo, it)
        }.andThen(UploadError::fromError) {
            userSession.completeChunkUpload(it)
        }.onSuccess {
            updateUploadInfo(modelStorage, collection, pathHash) {
                it.copy(status = UploadStatus.SUCCESS, progress = it.total)
            }
        }.onFailure { throwable ->
            updateUploadInfo(modelStorage, collection, pathHash) {
                it.copy(status = UploadStatus.FAILED, message = throwable.cause.safeMessage())
            }
        }
    }

    private suspend fun getUploadId(
        modelStorage: ModelStorage,
        userSession: SimpleUserSessionManager,
        objectTuple: ObjectTuple,
        clipFile: ClientFile,
        uploadInfo: UploadInfo,
        fileSha256: String,
    ): CustomResult<PrimaryKey, UploadError> {
        val collection = UploadCollection.fromInfo(uploadInfo)
        val pathHash = uploadInfo.pathHash
        val existingRecordId = modelStorage.upload.getDocument(collection, pathHash)?.recordId
        return CustomResult.success(existingRecordId).andThen(UploadError::fromError) {
            if (it == null) {
                getUploadRecordId(userSession, objectTuple, clipFile, modelStorage, uploadInfo, fileSha256)
            } else {
                CustomResult.success(it)
            }
        }
    }

    private suspend fun uploadChunkedFiles(
        userSession: SimpleUserSessionManager,
        uploadRecordId: PrimaryKey,
        clipFile: ClientFile,
        modelStorage: ModelStorage,
        uploadInfo: UploadInfo,
        status: CustomApi.Files.Chunks.StatusResponse,
    ): CustomResult<PrimaryKey, UploadError> =
        captureFailure(UploadError::from) {
            val collection = UploadCollection.fromInfo(uploadInfo)
            val pathHash = uploadInfo.pathHash
            val chunkSize = uploadInfo.chunkSize
            val uploadedIndices = status.uploaded.toSet()
            val totalSent =
                uploadedIndices.sumOf { idx ->
                    val chunkCount = ((clipFile.size + chunkSize - 1) / chunkSize).toInt()
                    val isLast = idx == chunkCount - 1
                    if (isLast) {
                        clipFile.size - chunkSize * (chunkCount - 1)
                    } else {
                        chunkSize
                    }
                }
            updateUploadInfo(modelStorage, collection, pathHash) {
                it.copy(progress = totalSent)
            }

            var index = 0
            var currentSent = totalSent
            clipFile.source().use { raw ->
                while (true) {
                    val buf = Buffer()
                    var count = 0L
                    while (count < chunkSize) {
                        val read = raw.readAtMostTo(buf, minOf(1024, chunkSize - count))
                        if (read == -1L) break
                        count += read
                    }
                    Napier.i(tag = "upload") {
                        "upload chunk $index, size $count"
                    }
                    if (count == 0L) break
                    // skip already uploaded chunks by index
                    if (!uploadedIndices.contains(index)) {
                        val hash = sha256(buf.peek())
                        val progress =
                            UploadProgressWriter(count) { sent ->
                                updateUploadInfo(modelStorage, collection, pathHash) {
                                    it.copy(progress = currentSent + sent)
                                }
                            }
                        userSession.uploadChunk(uploadRecordId, index, buf, hash) { sent, _ ->
                            if (persistTransportProgress) progress.update(sent)
                        }.getOrThrow()
                        currentSent += count
                        updateUploadInfo(modelStorage, collection, pathHash) {
                            it.copy(progress = currentSent)
                        }
                    }
                    index++
                }
            }
            uploadRecordId
        }.mapError(UploadError::fromError)

    private suspend fun getUploadRecordId(
        userSession: SimpleUserSessionManager,
        objectTuple: ObjectTuple,
        clipFile: ClientFile,
        modelStorage: ModelStorage,
        uploadInfo: UploadInfo,
        fileSha256: String,
    ): CustomResult<PrimaryKey, UploadError> {
        val chunkSize = uploadInfo.chunkSize
        val pathHash = uploadInfo.pathHash
        val collection = UploadCollection.fromInfo(uploadInfo)
        return userSession.initChunkUpload(
            objectTuple,
            clipFile.name,
            clipFile.size,
            clipFile.contentType,
            chunkSize,
            fileSha256,
            uploadInfo.crc32c,
        ).onSuccess { initResponse ->
            updateUploadInfo(modelStorage, collection, pathHash) {
                it.copy(recordId = initResponse.recordId, chunkSize = initResponse.chunkSize)
            }
        }.map {
            it.recordId
        }.mapError(UploadError::fromError)
    }

    suspend fun updateUploadInfo(
        modelStorage: ModelStorage,
        collection: UploadCollection,
        pathHash: String,
        block: (UploadInfo) -> UploadInfo,
    ) {
        val uploadInfo = modelStorage.upload.getDocument(collection, pathHash) ?: return
        modelStorage.upload.saveLast(collection, block(uploadInfo))
    }
}
