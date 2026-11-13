/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.filesystem

import com.storyteller_f.alphaheart.backend.core.ManagedRpcService
import com.storyteller_f.alphaheart.backend.core.RestartableRpcServer
import com.storyteller_f.alphaheart.backend.core.service.UploadPack
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.services.filesystem.api.CopyPack
import com.storyteller_f.services.filesystem.api.FilesystemRpc
import com.storyteller_f.services.filesystem.api.ObjectStorageRecord
import com.storyteller_f.services.filesystem.api.ObjectStorageWriteRecord
import com.storyteller_f.services.filesystem.api.RpcUploadPack
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.rpc.withService
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RpcRecoveryTest {
    @Test
    fun `failed establishment does not poison later calls`() =
        runTest {
            withAdapter { adapter, server, _, _ ->
                adapter.list("bucket", "").getOrThrow()
                server.stop()
                assertFailsWith<Exception> { adapter.list("bucket", "").getOrThrow() }
                server.start()
                assertTrue(adapter.list("bucket", "").getOrThrow().isEmpty())
                assertEquals(2, server.connections.value)
            }
        }

    @Test
    fun `same adapter recovers after server restart`() =
        runTest {
            withAdapter { adapter, server, _, pack ->
                adapter.upload("bucket", listOf(pack)).getOrThrow()
                server.stop()
                server.start()
                assertEquals(listOf("file"), adapter.list("bucket", "").getOrThrow().map { it.fullName })
                assertEquals(2, server.connections.value)
            }
        }

    @Test
    fun `concurrent calls share one reconnected transport`() =
        runTest {
            withAdapter { adapter, server, _, _ ->
                adapter.list("bucket", "").getOrThrow()
                server.stop()
                server.start()
                coroutineScope { List(16) { async { adapter.list("bucket", "").getOrThrow() } }.awaitAll() }
                assertEquals(2, server.connections.value)
            }
        }

    @Test
    fun `interrupted upload is not replayed or resumed`() =
        runTest {
            withAdapter { adapter, server, state, pack ->
                state.pauseChunks.value = true
                coroutineScope {
                    val pending = async { adapter.upload("bucket", listOf(pack)) }
                    state.entered.await()
                    server.stop()
                    assertFailsWith<Exception> { pending.await().getOrThrow() }
                }
                state.pauseChunks.value = false
                server.start()
                assertTrue(adapter.list("bucket", "").getOrThrow().isEmpty())
                assertEquals(1, state.begins.value)
                assertEquals(0, state.finishes.value)
                adapter.upload("bucket", listOf(pack)).getOrThrow()
                assertEquals(2, state.begins.value)
                assertEquals(1, state.finishes.value)
            }
        }

    @Test
    fun `caller cancellation aborts on pinned connection`() =
        runTest {
            withAdapter { adapter, server, state, pack ->
                state.pauseChunks.value = true
                coroutineScope {
                    val pending = async { adapter.upload("bucket", listOf(pack)) }
                    state.entered.await()
                    pending.cancel(CancellationException("caller cancelled"))
                    assertFailsWith<CancellationException> { pending.await() }
                    pending.join()
                }
                state.pauseChunks.value = false
                assertTrue(adapter.list("bucket", "").getOrThrow().isEmpty())
                assertEquals(1, state.aborts.value)
                assertEquals(1, server.connections.value)
            }
        }

    @Test
    fun `cancelled establishment leaves no cached client`() =
        runTest {
            withAdapter { adapter, server, state, _ ->
                state.pauseHealth.value = true
                coroutineScope {
                    val pending = async { adapter.list("bucket", "") }
                    state.entered.await()
                    pending.cancel(CancellationException("cancel establishment"))
                    assertFailsWith<CancellationException> { pending.await() }
                    pending.join()
                }
                state.pauseHealth.value = false
                assertTrue(adapter.list("bucket", "").getOrThrow().isEmpty())
                assertEquals(2, server.connections.value)
            }
        }

    private suspend fun withAdapter(
        block: suspend (
            FileSystemObjectStorageService,
            RestartableRpcServer<FilesystemRpc>,
            UploadState,
            UploadPack,
        ) -> Unit,
    ) {
        val state = UploadState()
        val server = RestartableRpcServer(FilesystemRpc::class) { UploadConnection(state) }
        server.start()
        val connection = ManagedRpcService(server.url, { it.withService<FilesystemRpc>() }, { it.health() })
        val file =
            Files.createTempFile(
                Files.createDirectories(Path.of("build/test/recovery")),
                "upload-",
                ".bin",
            ).toFile()
        val content = ByteArray(262_145) { it.toByte() }
        file.writeBytes(content)
        val pack =
            UploadPack(
                file,
                "file",
                content.size.toLong(),
                "file",
                MessageDigest.getInstance("SHA-256").digest(content).toHexString(),
            )
        try {
            block(FileSystemObjectStorageService(connection), server, state, pack)
        } finally {
            connection.close()
            server.stop()
            Files.deleteIfExists(file.toPath())
        }
    }
}

private class UploadState {
    val pauseChunks = MutableStateFlow(false)
    val pauseHealth = MutableStateFlow(false)
    val entered = CompletableDeferred<Unit>()
    val begins = MutableStateFlow(0)
    val finishes = MutableStateFlow(0)
    val aborts = MutableStateFlow(0)
    val files = MutableStateFlow<List<ObjectStorageRecord>>(emptyList())
}

private class UploadConnection(private val state: UploadState) : FilesystemRpc {
    private val pending = MutableStateFlow<Set<String>>(emptySet())
    override suspend fun health(): String {
        if (state.pauseHealth.value) {
            state.entered.complete(Unit)
            awaitCancellation()
        }
        return "ok"
    }
    override suspend fun beginUpload(bucketName: String, uploadPack: RpcUploadPack) {
        pending.update { it + uploadPack.transferId }
        state.begins.update { it + 1 }
    }
    override suspend fun uploadChunk(transferId: String, content: ByteArray) {
        check(transferId in pending.value)
        if (state.pauseChunks.value) {
            state.entered.complete(Unit)
            awaitCancellation()
        }
    }
    override suspend fun finishUpload(transferId: String): ObjectStorageWriteRecord {
        check(transferId in pending.value)
        pending.update { it - transferId }
        state.finishes.update { it + 1 }
        state.files.value =
            listOf(
                ObjectStorageRecord("http://localhost/objects/bucket/file", LocalDateTime(2026, 1, 1, 0, 0), "file"),
            )
        return ObjectStorageWriteRecord("file")
    }
    override suspend fun abortUpload(transferId: String) {
        pending.update { it - transferId }
        state.aborts.update { it + 1 }
    }
    override suspend fun list(bucketName: String, prefix: String) = state.files.value
    override suspend fun get(bucketName: String, names: List<String>) = state.files.value
    override suspend fun cleanObjects(bucketName: String) {
        state.files.value = emptyList()
    }
    override suspend fun copy(bucketName: String, copyPacks: List<CopyPack>): List<ObjectStorageRecord> =
        error(
            "Unused",
        )
    override suspend fun getChunk(bucketName: String, name: String, offset: Long, size: Int): ByteArray =
        error(
            "Unused",
        )
    override suspend fun compose(
        bucketName: String,
        targetFullName: String,
        sourceFullNames: List<String>,
        expectedSha256: String,
    ): ObjectStorageWriteRecord =
        error(
            "Unused",
        )
    override suspend fun delete(bucketName: String, names: List<String>): Unit = error("Unused")
}
