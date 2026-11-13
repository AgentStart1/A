/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server.route

import com.storyteller_f.alphaheart.api.CustomApi
import com.storyteller_f.alphaheart.api.FileInfoListResponse
import com.storyteller_f.alphaheart.api.FileRefInfoListResponse
import com.storyteller_f.alphaheart.api.NewFavorite
import com.storyteller_f.alphaheart.api.NewSubscription
import com.storyteller_f.alphaheart.backend.core.Backend
import com.storyteller_f.alphaheart.backend.core.CustomBadRequestException
import com.storyteller_f.alphaheart.backend.core.service.UploadPack
import com.storyteller_f.alphaheart.cloud.core.service.RootWritePermission
import com.storyteller_f.alphaheart.cloud.core.service.abortChunkUpload
import com.storyteller_f.alphaheart.cloud.core.service.addFavorite
import com.storyteller_f.alphaheart.cloud.core.service.addSubscription
import com.storyteller_f.alphaheart.cloud.core.service.checkRootWritePermission
import com.storyteller_f.alphaheart.cloud.core.service.completeChunkUpload
import com.storyteller_f.alphaheart.cloud.core.service.deleteFavoriteByObject
import com.storyteller_f.alphaheart.cloud.core.service.extractAlbum
import com.storyteller_f.alphaheart.cloud.core.service.getChunkStatus
import com.storyteller_f.alphaheart.cloud.core.service.getFileInfoById
import com.storyteller_f.alphaheart.cloud.core.service.getFileInfoByName
import com.storyteller_f.alphaheart.cloud.core.service.getFileList
import com.storyteller_f.alphaheart.cloud.core.service.getQuotaDetail
import com.storyteller_f.alphaheart.cloud.core.service.initChunkUpload
import com.storyteller_f.alphaheart.cloud.core.service.newFileName
import com.storyteller_f.alphaheart.cloud.core.service.removeSubscriptionByObject
import com.storyteller_f.alphaheart.cloud.core.service.searchFiles
import com.storyteller_f.alphaheart.cloud.core.service.tryCopyFile
import com.storyteller_f.alphaheart.cloud.core.service.tryUploadFiles
import com.storyteller_f.alphaheart.cloud.core.service.uncheckedGetFileRefsByFileId
import com.storyteller_f.alphaheart.cloud.error.ServerError
import com.storyteller_f.alphaheart.cloud.server.auth.handleResult
import com.storyteller_f.alphaheart.cloud.server.auth.usePrincipal
import com.storyteller_f.alphaheart.cloud.server.common.GeneralOffsetPagingGenerator
import com.storyteller_f.alphaheart.cloud.server.common.IdentifiablePagingGenerator
import com.storyteller_f.alphaheart.cloud.server.common.clientIp
import com.storyteller_f.alphaheart.cloud.server.common.invoke
import com.storyteller_f.alphaheart.cloud.server.common.pagination
import com.storyteller_f.endpoint4k.ktor.server.receiveBody
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.andThenPresent
import com.storyteller_f.result.captureFailure
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.result.map
import com.storyteller_f.result.toUnit
import com.storyteller_f.shared.model.A_FILE_DEFAULT_BUCKET
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.obj.ob
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.shared.utils.sha256
import io.github.aakira.napier.Napier
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.request.receiveChannel
import io.ktor.server.request.receiveMultipart
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.util.cio.readChannel
import io.ktor.util.cio.writeChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.copyAndClose
import io.ktor.utils.io.readBuffer
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.coroutineScope
import java.io.File
import kotlin.uuid.ExperimentalUuidApi

fun Route.bindProtectedFileRoute(backend: Backend) {
    bindUserFileRoute(backend)
    bindUserRoomRoute(backend)
    bindUserCommunityRoute(backend)

    CustomApi.Files.getByName(handleResult(backend)) {
        val ip = call.clientIp()
        usePrincipal { uid ->
            backend.getFileInfoByName(uid, it.objectId ob it.objectType, it.name, ip)
        }
    }

    CustomApi.Files.quota(handleResult(backend)) {
        usePrincipal { _ ->
            backend.getQuotaDetail(it.quotaType, ObjectTuple(it.objectId, it.objectType))
        }
    }

    val userHome = System.getProperty("user.home")
    val root = File(userHome, "a-temp")
    root.mkdirs()
    check(root.isDirectory) { "create a-temp failed" }

    CustomApi.Files.Id.extractAlbum(handleResult(backend)) { p, _ ->
        usePrincipal { uid ->
            backend.extractAlbum(p.id, root, uid)
        }
    }

    CustomApi.Files.upload(handleResult(backend)) { q, _ ->
        usePrincipal { uid ->
            uploadMedia(backend, uid, root, q)
        }
    }

    // Chunked upload endpoints
    bindChunkRoute(backend, root)

    CustomApi.Files.Id.get(handleResult(backend)) { p ->
        val ip = call.clientIp()
        usePrincipal { uid ->
            backend.getFileInfoById(p.id, uid, ip)
        }
    }

    CustomApi.Files.Id.copy(handleResult(backend)) { p, _ ->
        val ip = call.clientIp()
        usePrincipal { uid ->
            backend.tryCopyFile(p, uid, ip)
        }
    }

    CustomApi.Files.Id.Refs.get(handleResult(backend)) { q, p ->
        usePrincipal { _ ->
            q.pagination(IdentifiablePagingGenerator, { l, p ->
                FileRefInfoListResponse(l, p)
            }) { f ->
                backend.uncheckedGetFileRefsByFileId(p.id, f)
            }
        }
    }
    bindFileFavoriteRoute(backend)
    bindFileSubscriptionRoute(backend)
}

private fun Route.bindFileSubscriptionRoute(backend: Backend) {
    CustomApi.Files.Id.Subscription.add(handleResult(backend)) { p, _ ->
        usePrincipal { uid ->
            backend.addSubscription(uid, NewSubscription(p.id, ObjectType.FILE)).map { }
        }
    }

    CustomApi.Files.Id.Subscription.delete(handleResult(backend)) { p, _ ->
        usePrincipal { uid ->
            backend.removeSubscriptionByObject(uid, p.id).map { }
        }
    }
}

private fun Route.bindFileFavoriteRoute(backend: Backend) {
    CustomApi.Files.Id.Favorite.add(handleResult(backend)) { p, _ ->
        usePrincipal { uid ->
            backend.addFavorite(uid, NewFavorite(ObjectType.FILE, p.id)).map { }
        }
    }

    CustomApi.Files.Id.Favorite.delete(handleResult(backend)) { p, _ ->
        usePrincipal { uid ->
            backend.deleteFavoriteByObject(uid, p.id).map { }
        }
    }
}

private fun Route.bindUserCommunityRoute(backend: Backend) {
    CustomApi.Communities.Id.Files.get(handleResult(backend)) { query, path ->
        val ip = call.clientIp()
        usePrincipal { uid ->
            query.pagination(IdentifiablePagingGenerator, { l, p ->
                FileInfoListResponse(l, p)
            }) { pagingFetch ->
                backend.getFileList(uid, path.id ob ObjectType.COMMUNITY, pagingFetch, ip)
            }
        }
    }

    CustomApi.Communities.Id.Files.search(handleResult(backend)) { query, path ->
        val ip = call.clientIp()
        usePrincipal { uid ->
            query.pagination(GeneralOffsetPagingGenerator, { l, p ->
                FileInfoListResponse(l, p)
            }) { pagingFetch ->
                backend.searchFiles(uid, query, path.id ob ObjectType.COMMUNITY, pagingFetch, ip)
            }
        }
    }
}

private fun Route.bindUserRoomRoute(backend: Backend) {
    CustomApi.Rooms.Id.Files.get(handleResult(backend)) { query, path ->
        val ip = call.clientIp()
        usePrincipal { uid ->
            query.pagination(IdentifiablePagingGenerator, { l, p ->
                FileInfoListResponse(l, p)
            }) { pagingFetch ->
                backend.getFileList(uid, path.id ob ObjectType.ROOM, pagingFetch, ip)
            }
        }
    }

    CustomApi.Rooms.Id.Files.search(handleResult(backend)) { query, path ->
        val ip = call.clientIp()
        usePrincipal { uid ->
            query.pagination(GeneralOffsetPagingGenerator, { l, p ->
                FileInfoListResponse(l, p)
            }) { pagingFetch ->
                backend.searchFiles(uid, query, path.id ob ObjectType.ROOM, pagingFetch, ip)
            }
        }
    }
}

private fun Route.bindUserFileRoute(backend: Backend) {
    CustomApi.Users.Id.Files.get(handleResult(backend)) { query, path ->
        val ip = call.clientIp()
        usePrincipal { uid ->
            query.pagination(IdentifiablePagingGenerator, { l, p ->
                FileInfoListResponse(l, p)
            }) { pagingFetch ->
                backend.getFileList(uid, path.id ob ObjectType.USER, pagingFetch, ip)
            }
        }
    }

    CustomApi.Users.Id.Files.search(handleResult(backend)) { query, path ->
        val ip = call.clientIp()
        usePrincipal { uid ->
            query.pagination(GeneralOffsetPagingGenerator, { l, p ->
                FileInfoListResponse(l, p)
            }) { pagingFetch ->
                backend.searchFiles(uid, query, path.id ob ObjectType.USER, pagingFetch, ip)
            }
        }
    }
}

private fun Route.bindChunkRoute(backend: Backend, root: File) {
    CustomApi.Files.Chunks.init(handleResult(backend)) { api ->
        usePrincipal { uid ->
            initChunkUpload(backend, uid, api.receiveBody())
        }
    }

    CustomApi.Files.Chunks.upload(handleResult(backend)) { q, p, _ ->
        usePrincipal { _ ->
            uploadChunkFromChannel(backend, root, p, q) { call.receiveChannel() }
        }
    }

    CustomApi.Files.Chunks.complete(handleResult(backend)) { path, _ ->
        val ip = call.clientIp()
        usePrincipal { uid ->
            completeChunkUpload(backend, uid, path, ip)
        }
    }

    CustomApi.Files.Chunks.abort(handleResult(backend)) { path, _ ->
        usePrincipal { _ ->
            abortChunkUpload(backend, path)
        }
    }

    CustomApi.Files.Chunks.status(handleResult(backend)) { path ->
        usePrincipal { _ ->
            getChunkStatus(backend, path)
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
suspend fun RoutingContext.uploadMedia(
    backend: Backend,
    id: PrimaryKey,
    root: File,
    query: CustomApi.Files.UploadQuery,
): CustomResult<FileInfoListResponse?, ServerError> {
    val ip = call.clientIp()
    val objectTuple = ObjectTuple(query.objectId, query.objectType)
    if (objectTuple.objectType == ObjectType.TOPIC) {
        return CustomResult.Failure(ServerError.from(CustomBadRequestException("can't upload to topic")))
    }
    val parentType = objectTuple.objectType
    val parentId = objectTuple.objectId
    return backend.checkRootWritePermission(parentType, parentId, id).andThenPresent(ServerError::fromError) {
        processFormData { part ->
            val fileInfos =
                when (part) {
                    is PartData.FileItem -> {
                        val fileName = checkNotNull(part.originalFileName) { "Uploaded file name is missing" }
                        backend.uploadFilesFromChannel(root, it, fileName, query.sha256, ip) {
                            part.provider()
                        }
                    }

                    else -> {
                        emptyList()
                    }
                }
            part.dispose()
            fileInfos
        }
    }
}

private suspend fun RoutingContext.processFormData(block: suspend (PartData) -> List<FileInfo>) =
    try {
        val result = mutableListOf<FileInfo>()
        coroutineScope {
            call.receiveMultipart(1024 * 1024 * 100).forEachPart { part ->
                result.addAll(block(part))
            }
        }
        CustomResult.success(FileInfoListResponse(result.toImmutableList()))
    } catch (cancellation: kotlin.coroutines.cancellation.CancellationException) {
        throw cancellation
    } catch (e: Exception) {
        CustomResult.Failure(ServerError.from(e))
    }

private suspend fun Backend.uploadFilesFromChannel(
    root: File,
    permission: RootWritePermission,
    fileName: String,
    expectedSha256: String,
    clientIp: String?,
    provider: () -> ByteReadChannel,
): List<FileInfo> {
    val newSavedName = newFileName(fileName)
    val file = File.createTempFile("upload-", ".tmp", root)
    return try {
        provider().copyAndClose(file.writeChannel())
        val actual = sha256(file.readChannel().readBuffer())
        if (actual != expectedSha256) {
            throw CustomBadRequestException("file hash mismatch")
        }
        tryUploadFiles(
            permission.rootId,
            permission.rootType,
            listOf(
                UploadPack(
                    file,
                    newSavedName,
                    file.length(),
                    "${permission.rootId}/$newSavedName",
                    expectedSha256,
                ),
            ),
            clientIp,
        ).getOrThrow()
    } finally {
        file.delete()
    }
}

suspend fun uploadChunkFromChannel(
    backend: Backend,
    root: File,
    path: CustomApi.Files.Chunks.UploadPath,
    query: CustomApi.Files.Chunks.UploadQuery,
    provider: suspend () -> ByteReadChannel,
) = captureFailure(ServerError::from) {
    // 写入本地临时文件以校验哈希并上传到对象存储临时桶
    val chunkTmp = File.createTempFile("upload-chunk-", ".tmp", root)
    try {
        provider().copyAndClose(chunkTmp.writeChannel())
        val actual = sha256(chunkTmp.readChannel().readBuffer())
        val expected = query.hash
        if (actual != expected) {
            Napier.i(tag = "file") {
                "chunk hash mismatch, expected: $expected, actual: $actual"
            }
            throw CustomBadRequestException("chunk hash mismatch")
        } else {
            backend.objectStorageService.upload(
                A_FILE_DEFAULT_BUCKET,
                listOf(
                    UploadPack(
                        chunkTmp,
                        "chunk_${path.index}",
                        chunkTmp.length(),
                        "chunks/${path.id}/chunk_${path.index}",
                        expected,
                    ),
                ),
            ).getOrThrow()
        }
    } finally {
        chunkTmp.delete()
    }
}.toUnit()
