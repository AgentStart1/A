/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.core.service

import com.perraco.utils.SnowflakeFactory
import com.storyteller_f.alphaheart.api.CommonPath
import com.storyteller_f.alphaheart.api.CustomApi
import com.storyteller_f.alphaheart.api.SearchQuery
import com.storyteller_f.alphaheart.backend.core.Backend
import com.storyteller_f.alphaheart.backend.core.CustomBadRequestException
import com.storyteller_f.alphaheart.backend.core.ForbiddenException
import com.storyteller_f.alphaheart.backend.core.ObjectListFetch
import com.storyteller_f.alphaheart.backend.core.OffsetFetch
import com.storyteller_f.alphaheart.backend.core.PaginationResult
import com.storyteller_f.alphaheart.backend.core.PrimaryKeyFetch
import com.storyteller_f.alphaheart.backend.core.getImageDimension
import com.storyteller_f.alphaheart.backend.core.mapPagingNotNull
import com.storyteller_f.alphaheart.backend.core.mapPagingResultNotNull
import com.storyteller_f.alphaheart.backend.core.service.CopyPack
import com.storyteller_f.alphaheart.backend.core.service.FileDocument
import com.storyteller_f.alphaheart.backend.core.service.FileDocumentSearch
import com.storyteller_f.alphaheart.backend.core.service.PresignContext
import com.storyteller_f.alphaheart.backend.core.service.ProcessedUploadPack
import com.storyteller_f.alphaheart.backend.core.service.UploadPack
import com.storyteller_f.alphaheart.backend.core.types.FileRecord
import com.storyteller_f.alphaheart.backend.core.types.UploadRecord
import com.storyteller_f.alphaheart.backend.core.types.toFileInfo
import com.storyteller_f.alphaheart.backend.core.types.toFileRefInfo
import com.storyteller_f.alphaheart.backend.core.types.toUploadRecordInfo
import com.storyteller_f.alphaheart.cloud.core.service.error.FileError
import com.storyteller_f.alphaheart.cloud.core.utils.cleanImageMeta
import com.storyteller_f.alphaheart.cloud.core.utils.readFlacAlbumFromAudioStream
import com.storyteller_f.alphaheart.cloud.core.utils.readMp3AlbumFromAudioStream
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.andThen
import com.storyteller_f.result.andThenPresent
import com.storyteller_f.result.captureFailure
import com.storyteller_f.result.firstOrNull
import com.storyteller_f.result.fold
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.result.map
import com.storyteller_f.result.mapError
import com.storyteller_f.result.mapPresent
import com.storyteller_f.result.onFailure
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.A_FILE_DEFAULT_BUCKET
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.model.QuotaType
import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.obj.ob
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.shared.type.UploadRecordStatus
import com.storyteller_f.shared.utils.now
import com.storyteller_f.shared.utils.sha256
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.asSource
import kotlinx.io.buffered
import org.apache.tika.mime.MimeTypes
import java.io.BufferedInputStream
import java.io.File
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class FileResponse(val file: File)

suspend fun Backend.getFileList(
    uid: PrimaryKey,
    objectTuple: ObjectTuple,
    primaryKeyFetch: PrimaryKeyFetch,
    clientIp: String? = null,
): CustomResult<PaginationResult<FileInfo>?, FileError> {
    if (objectTuple.objectType == ObjectType.TOPIC) {
        return CustomResult.Failure(
            FileError.from(CustomBadRequestException("can't get topic file record")),
        ).mapError(FileError::fromError)
    }
    val parentType = objectTuple.objectType
    val parentId = objectTuple.objectId
    return checkRootWritePermission(parentType, parentId, uid).andThenPresent(FileError::fromError) {
        getFileInfoPaginationResult(parentId, primaryKeyFetch, clientIp)
    }.mapError(FileError::fromError)
}

suspend fun Backend.getFileInfoByName(
    uid: PrimaryKey,
    objectTuple: ObjectTuple,
    word: String,
    clientIp: String? = null,
): CustomResult<FileInfo?, FileError> {
    if (objectTuple.objectType == ObjectType.TOPIC) {
        return CustomResult.Failure(
            FileError.from(CustomBadRequestException("can't get topic file record")),
        ).mapError(FileError::fromError)
    }
    val parentType = objectTuple.objectType
    val parentId = objectTuple.objectId
    return checkRootWritePermission(
        parentType,
        parentId,
        uid,
    ).andThenPresent(FileError::fromError) { (_, rootId) ->
        database.file.getFileRecord(rootId, word)
    }.andThenPresent(FileError::fromError) { fileRecord ->
        processFileRecordToFileInfo(listOf(fileRecord), uid, clientIp)
    }.mapPresent {
        it.first()
    }.mapError(FileError::fromError)
}

suspend fun Backend.searchFiles(
    uid: PrimaryKey,
    query: SearchQuery,
    objectTuple: ObjectTuple,
    primaryKeyFetch: OffsetFetch,
    clientIp: String? = null,
): CustomResult<PaginationResult<FileInfo>?, FileError> {
    val word = query.word.trim()
    if (word.isBlank()) {
        return CustomResult.success(PaginationResult(emptyList(), 0))
    }
    val objectId = objectTuple.objectId
    val objectType = objectTuple.objectType

    // 如果指定了 objectId 和 objectType，需要检查权限
    return checkRootWritePermission(objectType, objectId, uid).andThenPresent(FileError::fromError) {
        searchFilesByWord(word, objectId, primaryKeyFetch, clientIp)
    }.mapError(FileError::fromError)
}

suspend fun Backend.uncheckedSearchFiles(
    query: SearchQuery,
    primaryKeyFetch: OffsetFetch,
): CustomResult<PaginationResult<FileInfo>?, FileError> {
    val word = query.word
    return (
        if (word.isBlank()) {
            CustomResult.success(PaginationResult(emptyList(), 0))
        } else {
            // 使用 fileSearchService 搜索
            searchFilesByWord(word, null, primaryKeyFetch)
        }
        ).mapError(FileError::fromError)
}

private suspend fun Backend.searchFilesByWord(
    word: String?,
    ownerId: PrimaryKey?,
    primaryKeyFetch: OffsetFetch,
    clientIp: String? = null,
): CustomResult<PaginationResult<FileInfo>?, FileError> =
    (
        if (word.isNullOrBlank()) {
            CustomResult.success(PaginationResult(emptyList(), 0))
        } else {
            // 使用 fileSearchService 搜索
            val searchQuery =
                if (ownerId != null) {
                    FileDocumentSearch.Keyword(word, ownerId, fetch = primaryKeyFetch)
                } else {
                    FileDocumentSearch.Keyword(word, fetch = primaryKeyFetch)
                }
            fileSearchService.searchDocument(searchQuery).andThenPresent(FileError::fromError) { (documents, total) ->
                val fileIds = documents.map { it.id }
                if (fileIds.isEmpty()) {
                    CustomResult.success(PaginationResult(emptyList(), total))
                } else {
                    database.file.getFileRecordByIds(fileIds).andThenPresent(FileError::fromError) { fileRecords ->
                        processFileRecordToFileInfo(fileRecords, ownerId, clientIp)
                    }.mapPresent { fileInfos ->
                        PaginationResult(fileInfos, total)
                    }
                }
            }
        }
        ).mapError(FileError::fromError)

@OptIn(ExperimentalUuidApi::class)
suspend fun Backend.extractAlbum(fileRecordId: PrimaryKey, root: File, uid: PrimaryKey) =
    checkObjectWritable(ObjectType.FILE, fileRecordId).andThenPresent(FileError::fromError) {
        database.file.getFileRecordByIds(listOf(fileRecordId))
    }.andThenPresent(FileError::fromError) { fileRecords ->
        val fileRecord = fileRecords.first()
        if (fileRecord.owner != uid) {
            throw ForbiddenException("no permission")
        }
        if (fileRecord.contentType.startsWith("audio")) {
            CustomResult.success(fileRecord)
        } else {
            CustomResult.Failure(FileError.from(CustomBadRequestException("not an audio file")))
        }
    }.andThenPresent(FileError::fromError) { fileRecord ->
        objectStorageService.getInputStream(A_FILE_DEFAULT_BUCKET, fileRecord.fullName)
            .andThen(FileError::fromError) { input ->
                extractAlbumFromStream(input.buffered(), root, fileRecord)
            }
            .andThenPresent(FileError::fromError) { (file, contentType) ->
                val name = newCoverFileName(fileRecord.name, contentType)
                tryUploadFiles(
                    fileRecord.owner,
                    fileRecord.ownerType,
                    listOf(
                        UploadPack(
                            file,
                            name,
                            fileRecord.size,
                            "${fileRecord.owner}/$name",
                            file.inputStream().buffered().use { input ->
                                sha256(input.asSource().buffered())
                            },
                        ),
                    ),
                )
            }
    }.mapPresent {
        it.firstOrNull()
    }

@OptIn(ExperimentalUuidApi::class)
private suspend fun extractAlbumFromStream(input: BufferedInputStream, root: File, fileRecord: FileRecord) =
    captureFailure(FileError::from) {
        withContext(Dispatchers.IO) {
            input.use {
                val saveAlbum = { image: ByteArray, mimeType: String ->
                    val ext = getCoverExtensionFromMimeType(mimeType)
                    val file = File(root, "${Uuid.random()}$ext")
                    file.writeBytes(image)
                    file
                }
                when (fileRecord.contentType) {
                    "audio/mp3" -> it.readMp3AlbumFromAudioStream(saveAlbum)
                    "audio/flac", "audio/x-flac" -> it.readFlacAlbumFromAudioStream(saveAlbum)
                    else -> throw CustomBadRequestException("unsupported audio type: ${fileRecord.contentType}")
                }
            }
        }
    }

@OptIn(ExperimentalUuidApi::class)
fun newFileName(fileName: String): String {
    val extension = fileName.substringAfterLast(".").take(10)
    val originName = fileName.substringBeforeLast(".")
    // length 32
    val uuid = Uuid.random().toHexString()
    val newSavedName =
        if (fileName.length > 60) {
            // 60 - 32 - 1 = 27
            "${originName.take(27 - extension.length)}$uuid.$extension"
        } else {
            fileName
        }
    return newSavedName
}

@OptIn(ExperimentalUuidApi::class)
fun newCopiedFileName(fileName: String): String {
    val extension = fileName.substringAfterLast(".").take(10)
    val originName = fileName.substringBeforeLast(".")
    // length 32
    val uuid = Uuid.random().toHexString()
    return "${originName.take(27 - extension.length)}$uuid.$extension"
}

@OptIn(ExperimentalUuidApi::class)
private fun newCoverFileName(fileName: String, contentType: String): String {
    val extension = getCoverExtensionFromMimeType(contentType)
    val originName = fileName.substringBeforeLast(".") + "_cover"
    // length 32
    val uuid = Uuid.random().toHexString()
    return "${originName.take(27 - extension.length)}$uuid$extension"
}

fun getCoverExtensionFromMimeType(mimeType: String): String =
    MimeTypes.getDefaultMimeTypes().forName(
        mimeType,
    )?.extension
        ?: error("Unsupported mime type: $mimeType")

suspend fun Backend.getFileInfoPaginationResult(
    uid: PrimaryKey,
    primaryKeyFetch: PrimaryKeyFetch,
    clientIp: String? = null,
): CustomResult<PaginationResult<FileInfo>, FileError> =
    database.file.getFileRecordPaginationList(uid, primaryKeyFetch)
        .mapPagingResultNotNull(FileError::fromError) { list ->
            processFileRecordToFileInfo(list, uid, clientIp)
        }.mapError(FileError::fromError)

suspend fun Backend.getAllFileInfos(
    primaryKeyFetch: PrimaryKeyFetch,
): CustomResult<PaginationResult<FileInfo>, FileError> =
    database.file.getAllFileRecordPaginationList(
        primaryKeyFetch,
    ).mapPagingResultNotNull(FileError::fromError) { list ->
        processFileRecordToFileInfo(list)
    }.mapError(FileError::fromError)

suspend fun Backend.getUserUploadRecords(uid: PrimaryKey, primaryKeyFetch: PrimaryKeyFetch) =
    database.file.getUploadRecordPaginationList(uid, primaryKeyFetch).mapPagingNotNull { list ->
        list.map { it.toUploadRecordInfo() }
    }

suspend fun Backend.getFileInfoById(
    id: PrimaryKey,
    uid: PrimaryKey? = null,
    clientIp: String? = null,
): CustomResult<FileInfo?, FileError> =
    database.file.getFileRecordByIds(listOf(id)).andThenPresent(FileError::fromError) { list ->
        processFileRecordToFileInfo(list, uid, clientIp)
    }.mapPresent { it.firstOrNull() }.mapError(FileError::fromError)

suspend fun Backend.tryCopyFile(p: CommonPath, uid: PrimaryKey, clientIp: String? = null) =
    checkObjectWritable(ObjectType.FILE, p.id).andThenPresent(FileError::fromError) {
        database.file.getFileRecordByIds(
            listOf(p.id),
        ).firstOrNull().andThenPresent(FileError::fromError) { fileRecord ->
            checkRootReadPermission(
                fileRecord.ownerType,
                fileRecord.owner,
                uid,
            ).andThenPresent(FileError::fromError) { permission ->
                if (permission.hasRead) {
                    CustomResult.success(fileRecord)
                } else {
                    CustomResult.Failure(FileError.from(ForbiddenException()))
                }
            }
        }
    }.andThenPresent(FileError::fromError) { fileRecord ->
        lockQuotaInfo(
            ObjectTuple(uid, ObjectType.USER),
            QuotaType.FILE,
            fileRecord.size,
        ) {
            copyFile(fileRecord, uid)
        }
    }.andThenPresent(FileError::fromError) {
        processFileRecordToFileInfo(it, uid, clientIp)
    }.mapPresent {
        it.firstOrNull()
    }

private suspend fun Backend.copyFile(fileRecord: FileRecord, newOwner: PrimaryKey) =
    database.file.getFileRecord(newOwner, fileRecord.name).map {
        if (it == null) {
            fileRecord.name
        } else {
            newCopiedFileName(fileRecord.name)
        }
    }.andThen(FileError::fromError) { newName ->
        val id = SnowflakeFactory.nextId()
        val newFileRecord = fileRecord.copy(id = id, owner = newOwner, name = newName, fullName = "$newOwner/$newName")
        objectStorageService.copy(
            A_FILE_DEFAULT_BUCKET,
            listOf(CopyPack(fileRecord.fullName, newFileRecord.fullName)),
        ).map {
            listOf(newFileRecord)
        }
    }

@OptIn(ExperimentalUuidApi::class)
suspend fun Backend.tryUploadFiles(
    ownerId: PrimaryKey,
    ownerType: ObjectType,
    files: List<UploadPack>,
    clientIp: String? = null,
) = lockQuotaInfo(
    ownerId ob ownerType,
    QuotaType.FILE,
    files.sumOf {
        it.size
    },
) {
    val f = mutableListOf<File>()
    try {
        val uploadPacks = processContentTypeAndDimension(files)
        val packs = removeExifIfImage(uploadPacks, f)
        uploadFilesToStorage(ownerId, ownerType, packs)
    } finally {
        f.forEach {
            it.delete()
        }
    }
}.andThen(FileError::fromError) {
    processFileRecordToFileInfo(it, ownerId, clientIp)
}

private suspend fun processContentTypeAndDimension(files: List<UploadPack>): List<ProcessedUploadPack> =
    files.map { uploadPack ->
        val detectedType = Backend.tika.detect(uploadPack.file) ?: error("delete content type failed")
        val dimension =
            if (detectedType.startsWith("image")) {
                getImageDimension(uploadPack.file.absolutePath, detectedType) {
                    uploadPack.file.inputStream().buffered()
                }
            } else {
                null
            }
        ProcessedUploadPack(uploadPack, detectedType, dimension)
    }

@OptIn(ExperimentalUuidApi::class)
internal suspend fun removeExifIfImage(
    uploadPacks: List<ProcessedUploadPack>,
    f: MutableList<File>,
): List<ProcessedUploadPack> =
    uploadPacks.map {
        if (it.contentType.startsWith("image")) {
            val target = File(System.getProperty("java.io.tmpdir"), Uuid.random().toHexString() + it.pack.name)
            target.outputStream().buffered().use { output ->
                cleanImageMeta(it.pack.file, output, it.contentType)
            }
            f.add(target)
            val newSha256 =
                target.inputStream().buffered().use { input ->
                    sha256(input.asSource().buffered())
                }
            it.copy(
                pack =
                it.pack.copy(
                    file = target,
                    size = target.length(),
                    sha256 = newSha256,
                ),
            )
        } else {
            it
        }
    }

private suspend fun Backend.uploadFilesToStorage(
    ownerId: PrimaryKey,
    ownerType: ObjectType,
    uploadPacks: List<ProcessedUploadPack>,
): CustomResult<List<FileRecord>, FileError> {
    val data =
        uploadPacks.map { p ->
            val uploadPack = p.pack
            val nextId = SnowflakeFactory.nextId()
            val dimension = p.dimension
            val savedSha256 = p.pack.sha256
            FileRecord(
                nextId,
                now(),
                uploadPack.name,
                0,
                dimension?.width ?: 0,
                dimension?.height ?: 0,
                ownerId,
                ownerType,
                p.contentType,
                uploadPack.size,
                uploadPack.fullName,
                savedSha256,
            )
        }
    return objectStorageService.upload(
        A_FILE_DEFAULT_BUCKET,
        uploadPacks.map { it.pack },
    ).andThen(FileError::fromError) { uploaded ->
        val map = uploaded.associateBy { it.fullName }
        data.forEach { record ->
            if (map[record.fullName] == null) {
                throw CustomBadRequestException("file upload failed")
            }
        }
        CustomResult.success(data)
    }.mapError(FileError::fromError)
}

suspend fun Backend.getFileInfoList(names: List<String>): CustomResult<List<FileInfo?>?, FileError> =
    database.file.getFileRecordByNames(
        names,
    ).andThen(FileError::fromError) { fileRecords ->
        processFileRecordToFileInfo(fileRecords)
    }.mapError(FileError::fromError)

suspend fun Backend.processFileRecordToFileInfo(
    fileRecords: List<FileRecord>,
    uid: PrimaryKey? = null,
    clientIp: String? = null,
): CustomResult<List<FileInfo>, FileError> {
    val fileRecordIds = fileRecords.map { it.id }
    val favoriteMap =
        if (uid != null && fileRecordIds.isNotEmpty()) {
            database.favorite.getHasFavorite(ObjectListFetch.IdListFetch(fileRecordIds), uid)
                .getOrNull()?.associateBy { it.objectId }.orEmpty()
        } else {
            emptyMap()
        }
    val subscriptionMap =
        if (uid != null && fileRecordIds.isNotEmpty()) {
            database.subscription.getHasSubscription(ObjectListFetch.IdListFetch(fileRecordIds), uid)
                .getOrNull()?.associateBy { it.objectId }.orEmpty()
        } else {
            emptyMap()
        }
    return objectStorageService.getWithPresignContext(
        bucketName = A_FILE_DEFAULT_BUCKET,
        names = fileRecords.map { it.fullName },
        presignContext = PresignContext(uid?.toString(), clientIp),
        responseContentTypes = fileRecords.associate { it.fullName to it.contentType },
    ).map { mediaList ->
        val mediaRecordMap = mediaList.associateBy { it.fullName }
        fileRecords.mapNotNull { media ->
            mediaRecordMap[media.fullName]?.let {
                media.toFileInfo(it.url, it.lastModified, favoriteMap[media.id]?.id, subscriptionMap[media.id]?.id)
            }
        }
    }.mapError(FileError::fromError)
}

suspend fun initChunkUpload(backend: Backend, id: PrimaryKey, body: CustomApi.Files.Chunks.InitBody) =
    backend.checkRootWritePermission(body.objectType, body.objectId, id)
        .andThenPresent(FileError::fromError) {
            backend.checkQuotaStatus(body.objectId ob body.objectType, body.size, QuotaType.FILE)
        }
        .andThenPresent(FileError::fromError) {
            backend.database.file.insertUploadRecord(
                UploadRecord(
                    SnowflakeFactory.nextId(),
                    now(),
                    body.objectId,
                    body.objectType,
                    UploadRecordStatus.PENDING,
                    body.size,
                    0,
                    body.name,
                    body.chunkSize,
                    body.sha256,
                    body.crc32c,
                ),
            )
        }
        .mapPresent {
            CustomApi.Files.Chunks.InitResponse(it.id, body.chunkSize)
        }

@Suppress("LongMethod")
suspend fun completeChunkUpload(
    backend: Backend,
    id: PrimaryKey,
    path: CommonPath,
    clientIp: String? = null,
): CustomResult<FileInfo?, FileError> {
    val recordId = path.id
    // 读取上传记录以获取所有者信息
    val record =
        backend.database.file.getUploadRecord(recordId).getOrThrow()
            ?: throw CustomBadRequestException("upload record not found")
    val expectedSha256 = record.sha256
    val tuple = ObjectTuple(record.objectId, record.objectType)
    return backend.checkRootWritePermission(tuple.objectType, tuple.objectId, id)
        .andThenPresent(FileError::fromError) {
            val name = record.name
            val totalSize = record.total
            val expectedChunkCount = ((totalSize + record.chunkSize - 1) / record.chunkSize).toInt()
            val newSavedName = newFileName(name)
            val targetFullName = "${tuple.objectId}/$newSavedName"
            captureFailure(FileError::from) {
                // 使用对象存储 compose 在默认桶直接合并到最终路径
                val chunkRecords =
                    backend.objectStorageService.list(
                        A_FILE_DEFAULT_BUCKET,
                        "chunks/$recordId/",
                    ).getOrThrow()
                val chunkMap =
                    chunkRecords.mapNotNull { r ->
                        val index = r.fullName.substringAfter("chunk_").toIntOrNull()
                        if (index == null) {
                            null
                        } else {
                            index to r.fullName
                        }
                    }.toMap()
                if (chunkMap.size != expectedChunkCount) {
                    throw CustomBadRequestException("chunk missing")
                }
                val sortedSources =
                    (0 until expectedChunkCount).map { index ->
                        chunkMap[index] ?: throw CustomBadRequestException("chunk missing")
                    }
                // 合并到最终对象
                backend.objectStorageService.compose(
                    A_FILE_DEFAULT_BUCKET,
                    targetFullName,
                    sortedSources,
                    expectedSha256,
                    record.crc32c,
                ).getOrThrow()
                val fileRecord =
                    backend.buildFileRecordFromComposedObject(
                        targetFullName,
                        newSavedName,
                        tuple,
                        totalSize,
                        expectedSha256,
                    )
                // 完成后释放上传锁并更新配额使用量
                val quotaInfo = backend.getQuotaInfo(QuotaType.FILE, tuple).getOrThrow()
                backend.database.file.updateUploadRecordStatus(
                    quotaInfo,
                    record.copy(status = UploadRecordStatus.SUCCESS),
                    listOf(fileRecord),
                ).onSuccess {
                    // 保存文件文档到搜索服务
                    backend.fileSearchService.saveDocument(
                        listOf(FileDocument.fromFileRecord(fileRecord)),
                    ).onFailure { error ->
                        Napier.e(error.cause) {
                            "save file document failed"
                        }
                    }
                }.getOrThrow()
                // 清理分片与元数据
                cleanChunk(backend, sortedSources, recordId)
                listOf(fileRecord)
            }.fold(
                onSuccess = { fileRecords ->
                    CustomResult.success(fileRecords)
                },
                onFailure = { error ->
                    captureFailure(FileError::from) {
                        val quotaInfo = backend.getQuotaInfo(QuotaType.FILE, tuple).getOrThrow()
                        backend.database.file.updateUploadRecordStatus(
                            quotaInfo,
                            record.copy(status = UploadRecordStatus.FAILED),
                            emptyList(),
                        ).getOrThrow()
                    }
                    captureFailure(FileError::from) {
                        backend.objectStorageService.delete(
                            A_FILE_DEFAULT_BUCKET,
                            listOf(
                                targetFullName,
                            ),
                        ).getOrThrow()
                    }
                    captureFailure(FileError::from) {
                        val sessionObjects =
                            backend.objectStorageService.list(
                                A_FILE_DEFAULT_BUCKET,
                                "chunks/$recordId/",
                            ).getOrThrow().map { it.fullName }
                        if (sessionObjects.isNotEmpty()) {
                            backend.objectStorageService.delete(A_FILE_DEFAULT_BUCKET, sessionObjects).getOrThrow()
                        }
                    }
                    CustomResult.Failure(FileError.fromError(error))
                },
            )
        }.andThenPresent(FileError::fromError) {
            backend.processFileRecordToFileInfo(it, id, clientIp)
        }.firstOrNull().mapError(FileError::fromError)
}

private suspend fun Backend.buildFileRecordFromComposedObject(
    targetFullName: String,
    newSavedName: String,
    tuple: ObjectTuple,
    totalSize: Long,
    sha256: String,
): FileRecord {
    // 内容类型与尺寸检测
    val contentType =
        objectStorageService.getInputStream(
            A_FILE_DEFAULT_BUCKET,
            targetFullName,
        ).getOrThrow().buffered().use {
            Backend.tika.detect(it)
        }
    val dimension =
        if (contentType.startsWith("image")) {
            getImageDimension(targetFullName, contentType) {
                objectStorageService.getInputStream(A_FILE_DEFAULT_BUCKET, targetFullName).getOrThrow().buffered()
            }
        } else {
            null
        }

    // 写入数据库记录
    return FileRecord(
        id = SnowflakeFactory.nextId(),
        createdTime = now(),
        name = newSavedName,
        0,
        width = dimension?.width ?: 0,
        height = dimension?.height ?: 0,
        owner = tuple.objectId,
        ownerType = tuple.objectType,
        contentType = contentType,
        size = totalSize,
        fullName = targetFullName,
        sha256 = sha256,
    )
}

private suspend fun cleanChunk(backend: Backend, sortedSources: List<String>, recordId: PrimaryKey) {
    captureFailure(FileError::from) {
        backend.objectStorageService.delete(A_FILE_DEFAULT_BUCKET, sortedSources).getOrThrow()
        // 删除整个分片目录下可能残留的对象
        val sessionObjects =
            backend.objectStorageService.list(
                A_FILE_DEFAULT_BUCKET,
                "chunks/$recordId/",
            ).getOrThrow().map { it.fullName }
        if (sessionObjects.isNotEmpty()) {
            backend.objectStorageService.delete(A_FILE_DEFAULT_BUCKET, sessionObjects)
                .getOrThrow()
        }
    }.onFailure {
        Napier.e(it.cause) { "clean chunk failed $recordId $sortedSources" }
    }
}

suspend fun abortChunkUpload(backend: Backend, path: CommonPath) =
    backend.database.file.getUploadRecord(path.id).andThenPresent(FileError::fromError) { uploadRecord ->
        captureFailure(FileError::from) {
            // 删除默认桶下对应会话的所有对象，并释放上传锁
            val recordId = path.id
            val tuple = ObjectTuple(uploadRecord.objectId, uploadRecord.objectType)
            val quotaInfo = backend.getQuotaInfo(QuotaType.FILE, tuple).getOrThrow()
            backend.database.file.updateUploadRecordStatus(
                quotaInfo,
                uploadRecord.copy(status = UploadRecordStatus.ABORTED),
                emptyList(),
            ).getOrThrow()
            val records = backend.objectStorageService.list(A_FILE_DEFAULT_BUCKET, "chunks/$recordId/").getOrThrow()
            val names = records.map { it.fullName }
            backend.objectStorageService.delete(A_FILE_DEFAULT_BUCKET, names).getOrThrow()
        }
    }

suspend fun getChunkStatus(backend: Backend, path: CommonPath) =
    backend.database.file.getUploadRecord(path.id).andThenPresent(FileError::fromError) { record ->
        captureFailure(FileError::from) {
            // 从对象存储临时桶读取已上传分片索引
            val records =
                backend.objectStorageService.list(
                    A_FILE_DEFAULT_BUCKET,
                    "chunks/${path.id}/",
                ).getOrThrow()
            val uploadedIndices =
                records.mapNotNull { r ->
                    r.fullName.substringAfter("chunk_").toIntOrNull()
                }.sorted()
            // 从数据库中的 UploadRecord 获取 chunkSize/size
            CustomApi.Files.Chunks.StatusResponse(
                uploadedIndices,
                record.chunkSize,
                record.total,
                record.id,
                record.status,
            )
        }
    }

suspend fun Backend.uncheckedGetFileRefsByFileId(fileId: PrimaryKey, primaryKeyFetch: PrimaryKeyFetch) =
    database.file.getFileRefsByFileId(fileId, primaryKeyFetch).mapPagingNotNull { list ->
        list.map { ref ->
            ref.toFileRefInfo()
        }
    }

suspend fun Backend.getFileRefsByFileId(uid: PrimaryKey, fileId: PrimaryKey, primaryKeyFetch: PrimaryKeyFetch) =
    checkRootReadPermission(ObjectType.FILE, fileId, uid).andThenPresent(FileError::fromError) {
        database.file.getFileRefsByFileId(fileId, primaryKeyFetch).mapPagingNotNull { list ->
            list.map { ref ->
                ref.toFileRefInfo()
            }
        }
    }
