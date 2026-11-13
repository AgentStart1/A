/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.filesystem

import com.storyteller_f.result.getOrThrow
import com.storyteller_f.services.filesystem.api.CopyPack
import com.storyteller_f.services.filesystem.api.FilesystemRpc
import com.storyteller_f.services.filesystem.api.ObjectStorageRecord
import com.storyteller_f.services.filesystem.api.ObjectStorageWriteRecord
import com.storyteller_f.services.filesystem.api.RpcUploadPack
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.autohead.AutoHeadResponse
import io.ktor.server.plugins.partialcontent.PartialContent
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.options
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import kotlinx.coroutines.job
import kotlinx.rpc.krpc.ktor.server.Krpc
import kotlinx.rpc.krpc.ktor.server.rpc
import kotlinx.rpc.krpc.serialization.json.json
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.concurrent.ConcurrentHashMap

private const val MAX_CHUNK_SIZE = 256 * 1024

private data class PendingUpload(val bucketName: String, val pack: RpcUploadPack, val path: Path)

internal class FilesystemRpcImpl(
    private val storage: LocalFileSystemObjectStorageService,
    private val uploadDirectory: Path,
) : FilesystemRpc {
    private val logger = LoggerFactory.getLogger(FilesystemRpcImpl::class.java)
    private val uploads = ConcurrentHashMap<String, PendingUpload>()

    fun closeUploads() {
        uploads.values.forEach { pending ->
            try {
                Files.deleteIfExists(pending.path)
            } catch (failure: Exception) {
                logger.error("Remove disconnected filesystem upload failed", failure)
            }
        }
        uploads.clear()
    }

    override suspend fun health() = "ok"

    override suspend fun beginUpload(bucketName: String, uploadPack: RpcUploadPack) {
        require(uploadPack.transferId.isNotBlank()) { "transfer id is empty" }
        val pending =
            PendingUpload(
                bucketName,
                uploadPack,
                Files.createTempFile(uploadDirectory, "a-filesystem-rpc-", ".upload"),
            )
        check(uploads.putIfAbsent(uploadPack.transferId, pending) == null) { "transfer already exists" }
    }

    override suspend fun uploadChunk(transferId: String, content: ByteArray) {
        require(content.size <= MAX_CHUNK_SIZE) { "upload chunk is too large" }
        val pending = uploads[transferId] ?: error("unknown transfer")
        Files.newOutputStream(pending.path, StandardOpenOption.APPEND).buffered().use { it.write(content) }
    }

    override suspend fun finishUpload(transferId: String): ObjectStorageWriteRecord {
        val pending = uploads.remove(transferId) ?: error("unknown transfer")
        return try {
            check(Files.size(pending.path) == pending.pack.size) { "uploaded size does not match metadata" }
            storage.upload(
                pending.bucketName,
                listOf(
                    UploadPack(
                        pending.path.toFile(),
                        pending.pack.fullName,
                    ),
                ),
            ).getOrThrow().single()
        } finally {
            Files.deleteIfExists(pending.path)
        }
    }

    override suspend fun abortUpload(transferId: String) {
        uploads.remove(transferId)?.let { Files.deleteIfExists(it.path) }
    }

    override suspend fun get(bucketName: String, names: List<String>) = storage.get(bucketName, names).getOrThrow()
    override suspend fun cleanObjects(bucketName: String) {
        storage.clean(bucketName).getOrThrow()
    }
    override suspend fun list(bucketName: String, prefix: String) = storage.list(bucketName, prefix).getOrThrow()
    override suspend fun copy(bucketName: String, copyPacks: List<CopyPack>): List<ObjectStorageRecord> =
        storage.copy(bucketName, copyPacks).getOrThrow()
    override suspend fun getChunk(bucketName: String, name: String, offset: Long, size: Int): ByteArray {
        require(offset >= 0) { "offset must not be negative" }
        require(size in 1..MAX_CHUNK_SIZE) { "invalid chunk size" }
        return storage.getInputStream(bucketName, name).getOrThrow().buffered().use { input ->
            var remaining = offset
            while (remaining > 0) {
                val skipped = input.skip(remaining)
                check(skipped > 0) { "offset exceeds object size" }
                remaining -= skipped
            }
            input.readNBytes(size)
        }
    }
    override suspend fun compose(
        bucketName: String,
        targetFullName: String,
        sourceFullNames: List<String>,
        expectedSha256: String,
    ) = storage.compose(bucketName, targetFullName, sourceFullNames, expectedSha256).getOrThrow()
    override suspend fun delete(bucketName: String, names: List<String>) {
        storage.delete(
            bucketName,
            names,
        ).getOrThrow()
    }
}

fun main() {
    val logger = LoggerFactory.getLogger("filesystem-service")
    val port = System.getenv("FILESYSTEM_RPC_PORT")?.toIntOrNull() ?: 8820
    val base = Paths.get(System.getenv("FILE_SYSTEM_MEDIA_PATH") ?: "/data")
    val publicUrl = System.getenv("FILESYSTEM_PUBLIC_URL") ?: error("FILESYSTEM_PUBLIC_URL is empty")
    val httpPort = System.getenv("FILESYSTEM_HTTP_PORT")?.toIntOrNull() ?: 8822
    logger.info("Filesystem service starting: RPC port={}, HTTP port={}", port, httpPort)
    val storage = LocalFileSystemObjectStorageService(publicUrl, base)
    embeddedServer(CIO, host = "0.0.0.0", port = httpPort) {
        configureObjectDownloads(storage)
    }.start(wait = false)
    embeddedServer(CIO, host = "0.0.0.0", port = port) {
        install(WebSockets) { maxFrameSize = Long.MAX_VALUE }
        install(Krpc)
        routing {
            get("/health") { call.respondText("ok") }
            configureFilesystemRpc(storage)
        }
    }.start(wait = true)
}

internal fun Route.configureFilesystemRpc(
    storage: LocalFileSystemObjectStorageService,
    uploadDirectory: Path = Paths.get(System.getProperty("java.io.tmpdir")),
) {
    rpc("/rpc") {
        val service = FilesystemRpcImpl(storage, uploadDirectory)
        // Session completion runs after in-flight calls, so no upload can register after cleanup.
        coroutineContext.job.invokeOnCompletion { service.closeUploads() }
        rpcConfig { serialization { json() } }
        registerService<FilesystemRpc> { service }
    }
}

internal fun Application.configureObjectDownloads(storage: LocalFileSystemObjectStorageService) {
    install(PartialContent)
    install(AutoHeadResponse)
    routing {
        get("/health") { call.respondText("ok") }
        options("/objects/{path...}") {
            val method = call.request.headers[HttpHeaders.AccessControlRequestMethod]
            if (method != HttpMethod.Get.value && method != HttpMethod.Head.value) {
                call.respond(HttpStatusCode.Forbidden)
            } else {
                call.configureDownloadCors()
                call.response.header(HttpHeaders.AccessControlAllowMethods, "GET, HEAD")
                call.response.header(HttpHeaders.AccessControlAllowHeaders, "*")
                call.respond(HttpStatusCode.NoContent)
            }
        }
        get("/objects/{path...}") {
            call.configureDownloadCors()
            val path = call.parameters.getAll("path").orEmpty()
            val file = storage.getPathResponse(path)
            if (file == null) {
                call.respondText("not found", status = HttpStatusCode.NotFound)
            } else {
                call.respondFile(file.toFile())
            }
        }
    }
}

private fun ApplicationCall.configureDownloadCors() {
    response.header(HttpHeaders.AccessControlAllowOrigin, "*")
    response.header(HttpHeaders.AccessControlExposeHeaders, "Content-Length,Content-Range,Accept-Ranges")
}
