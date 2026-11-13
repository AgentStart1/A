/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.filesystem

import com.storyteller_f.alphaheart.backend.core.service.UploadPack
import com.storyteller_f.services.filesystem.api.CopyPack
import com.storyteller_f.services.filesystem.api.FilesystemRpc
import com.storyteller_f.services.filesystem.api.ObjectStorageRecord
import com.storyteller_f.services.filesystem.api.ObjectStorageWriteRecord
import com.storyteller_f.services.filesystem.api.RpcUploadPack
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class UploadCancellationTest {
    @Test
    fun `cancellation aborts the remote upload`() = runTest { verifyCancellation(null) }

    @Test
    fun `abort failure preserves the original cancellation`() =
        runTest { verifyCancellation(IllegalStateException("abort failed")) }

    @Test
    fun `begin cancellation aborts registered upload`() = runTest { verifyCancellation(null, cancelDuringBegin = true) }

    private suspend fun verifyCancellation(abortFailure: Exception?, cancelDuringBegin: Boolean = false) =
        coroutineScope {
            val rpc = UploadRpc(abortFailure, cancelDuringBegin)
            val directory = Files.createDirectories(Path.of("build/test/filesystem"))
            val file = Files.createTempFile(directory, "cancel-", ".bin").toFile()
            file.writeBytes(byteArrayOf(1))
            try {
                val service = FileSystemObjectStorageService(rpc)
                val upload = async { service.upload("bucket", listOf(UploadPack(file, "file", 1, "file", "digest"))) }
                if (cancelDuringBegin) rpc.transferId.await() else rpc.chunkStarted.await()
                val cancellation = CancellationException("cancel upload")
                upload.cancel(cancellation)
                val failure = assertFailsWith<CancellationException> { upload.await() }
                upload.join()
                val causes = generateSequence<Throwable>(failure) { it.cause }.toList()
                assertTrue(causes.any { it === cancellation })
                assertEquals(listOf(rpc.transferId.await()), rpc.aborted)
                assertTrue(rpc.registered.isEmpty())
                if (cancelDuringBegin) assertTrue(!rpc.chunkStarted.isCompleted)
            } finally {
                Files.deleteIfExists(file.toPath())
            }
        }
}

private class UploadRpc(private val abortFailure: Exception?, private val pauseDuringBegin: Boolean) : FilesystemRpc {
    val transferId = CompletableDeferred<String>()
    val chunkStarted = CompletableDeferred<Unit>()
    val aborted = mutableListOf<String>()
    val registered = mutableSetOf<String>()

    override suspend fun beginUpload(bucketName: String, uploadPack: RpcUploadPack) {
        registered.add(uploadPack.transferId)
        transferId.complete(uploadPack.transferId)
        if (pauseDuringBegin) awaitCancellation()
    }
    override suspend fun uploadChunk(transferId: String, content: ByteArray) {
        chunkStarted.complete(Unit)
        awaitCancellation()
    }
    override suspend fun abortUpload(transferId: String) {
        currentCoroutineContext().ensureActive()
        aborted.add(transferId)
        registered.remove(transferId)
        if (abortFailure != null) throw abortFailure
    }
    override suspend fun finishUpload(transferId: String): ObjectStorageWriteRecord = error("Unexpected finish")
    override suspend fun health(): String = error("Unused")
    override suspend fun get(bucketName: String, names: List<String>): List<ObjectStorageRecord> = error("Unused")
    override suspend fun cleanObjects(bucketName: String): Unit = error("Unused")
    override suspend fun list(bucketName: String, prefix: String): List<ObjectStorageRecord> = error("Unused")
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
    ): ObjectStorageWriteRecord = error("Unused")
    override suspend fun delete(bucketName: String, names: List<String>): Unit = error("Unused")
}
