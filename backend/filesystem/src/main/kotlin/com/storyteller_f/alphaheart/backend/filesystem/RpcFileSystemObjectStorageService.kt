/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.filesystem

import com.storyteller_f.alphaheart.backend.core.ManagedRpcService
import com.storyteller_f.alphaheart.backend.core.MergedEnv
import com.storyteller_f.alphaheart.backend.core.RpcServiceProvider
import com.storyteller_f.alphaheart.backend.core.error.ObjectStorageError
import com.storyteller_f.alphaheart.backend.core.service.CopyPack
import com.storyteller_f.alphaheart.backend.core.service.ObjectStorageRecord
import com.storyteller_f.alphaheart.backend.core.service.ObjectStorageService
import com.storyteller_f.alphaheart.backend.core.service.ObjectStorageServiceFactory
import com.storyteller_f.alphaheart.backend.core.service.ObjectStorageWriteRecord
import com.storyteller_f.alphaheart.backend.core.service.UploadPack
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.captureFailure
import com.storyteller_f.services.filesystem.api.FilesystemRpc
import com.storyteller_f.services.filesystem.api.RpcUploadPack
import io.github.aakira.napier.Napier
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.http.encodedPath
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.rpc.withService
import java.io.FilterInputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import com.storyteller_f.services.filesystem.api.CopyPack as RpcCopyPack
import com.storyteller_f.services.filesystem.api.ObjectStorageRecord as RpcObjectStorageRecord

private const val TRANSFER_CHUNK_SIZE = 256 * 1024

class FileSystemObjectStorageService(
    private val connection: RpcServiceProvider<FilesystemRpc>,
    private val publicUrl: String? = null,
) : ObjectStorageService {
    constructor(rpc: FilesystemRpc, publicUrl: String? = null) : this(RpcServiceProvider { rpc }, publicUrl)
    override suspend fun upload(bucketName: String, uploadPacks: List<UploadPack>) =
        rpcResult {
            val rpc = connection.acquire()
            uploadPacks.map { pack -> uploadOne(rpc, bucketName, pack) }
        }

    private suspend fun uploadOne(rpc: FilesystemRpc, bucketName: String, pack: UploadPack): ObjectStorageWriteRecord {
        val transferId = UUID.randomUUID().toString()
        var completed = false
        var failure: Exception? = null
        return try {
            rpc.beginUpload(
                bucketName,
                RpcUploadPack(transferId, pack.name, pack.size, pack.fullName, pack.sha256),
            )
            pack.file.inputStream().buffered().use { input ->
                val buffer = ByteArray(TRANSFER_CHUNK_SIZE)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    rpc.uploadChunk(transferId, if (count == buffer.size) buffer else buffer.copyOf(count))
                }
            }
            rpc.finishUpload(transferId).let {
                completed = true
                ObjectStorageWriteRecord(it.fullName)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            failure = exception
            throw exception
        } finally {
            if (!completed) {
                withContext(NonCancellable) { abortUpload(rpc, transferId, failure) }
            }
        }
    }

    private suspend fun abortUpload(rpc: FilesystemRpc, transferId: String, cause: Throwable?) {
        try {
            rpc.abortUpload(transferId)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (abortException: Exception) {
            if (cause != null) {
                cause.addSuppressed(abortException)
            } else {
                Napier.e(abortException) { "abort cancelled filesystem upload failed" }
            }
        }
    }

    override suspend fun get(bucketName: String, names: List<String>) =
        rpcResult {
            publicRecords(
                connection.acquire().get(bucketName, names),
            )
        }
    override suspend fun clean(bucketName: String) = rpcResult { connection.acquire().cleanObjects(bucketName) }
    override suspend fun list(bucketName: String, prefix: String) =
        rpcResult {
            publicRecords(
                connection.acquire().list(bucketName, prefix),
            )
        }
    override suspend fun copy(bucketName: String, copyPacks: List<CopyPack>) =
        rpcResult {
            publicRecords(
                connection.acquire().copy(
                    bucketName,
                    copyPacks.map { RpcCopyPack(it.originFullName, it.newFullName) },
                ),
            )
        }

    private fun publicRecords(records: List<RpcObjectStorageRecord>): List<ObjectStorageRecord> =
        records.map { rpcRecord ->
            val record = ObjectStorageRecord(rpcRecord.url, rpcRecord.lastModified, rpcRecord.fullName)
            if (publicUrl == null) {
                record
            } else {
                record.copy(
                    url =
                    URLBuilder(publicUrl).apply {
                        encodedPath = Url(record.url).encodedPath
                    }.buildString(),
                )
            }
        }

    override suspend fun getInputStream(
        bucketName: String,
        name: String,
    ): CustomResult<InputStream, ObjectStorageError> =
        rpcResult {
            val rpc = connection.acquire()
            val temporary = Files.createTempFile("a-filesystem-rpc-", ".download")
            try {
                Files.newOutputStream(temporary).buffered().use { output ->
                    var offset = 0L
                    do {
                        val chunk = rpc.getChunk(bucketName, name, offset, TRANSFER_CHUNK_SIZE)
                        output.write(chunk)
                        offset += chunk.size
                    } while (chunk.size == TRANSFER_CHUNK_SIZE)
                }
                DeletingInputStream(temporary)
            } catch (throwable: Throwable) {
                Files.deleteIfExists(temporary)
                throw throwable
            }
        }

    override suspend fun compose(
        bucketName: String,
        targetFullName: String,
        sourceFullNames: List<String>,
        expectedSha256: String,
        expectedCrc32c: String,
    ) = rpcResult {
        connection.acquire().compose(bucketName, targetFullName, sourceFullNames, expectedSha256).let {
            ObjectStorageWriteRecord(it.fullName)
        }
    }

    override suspend fun delete(bucketName: String, names: List<String>) =
        rpcResult {
            connection.acquire().delete(
                bucketName,
                names,
            )
        }
}

private class DeletingInputStream(private val path: Path) :
    FilterInputStream(Files.newInputStream(path).buffered()) {
    override fun close() {
        try {
            super.close()
        } finally {
            Files.deleteIfExists(path)
        }
    }
}

private inline fun <T> rpcResult(block: () -> T): CustomResult<T, ObjectStorageError> =
    captureFailure(
        ObjectStorageError::from,
        block,
    )

class FileSystemObjectStorageServiceFactory : ObjectStorageServiceFactory {
    override fun match(env: MergedEnv) = env["MEDIA_SERVICE"] == "filesystem"

    override fun build(env: MergedEnv): ObjectStorageService {
        val url = env["FILESYSTEM_RPC_URL"] ?: error("FILESYSTEM_RPC_URL is empty")
        val connection = ManagedRpcService(url, { it.withService<FilesystemRpc>() }, { it.health() })
        return FileSystemObjectStorageService(connection, env["FILESYSTEM_PUBLIC_URL"])
    }
}
