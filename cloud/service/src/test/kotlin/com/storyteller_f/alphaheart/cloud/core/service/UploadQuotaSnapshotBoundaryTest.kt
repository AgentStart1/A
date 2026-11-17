/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.core.service

import com.storyteller_f.alphaheart.backend.core.Backend
import com.storyteller_f.alphaheart.backend.core.error.ObjectStorageError
import com.storyteller_f.alphaheart.backend.core.service.ObjectStorageRecord
import com.storyteller_f.alphaheart.backend.core.service.ObjectStorageService
import com.storyteller_f.alphaheart.backend.core.types.UploadRecord
import com.storyteller_f.alphaheart.cloud.core.service.error.ContainerError
import com.storyteller_f.result.CustomResult
import com.storyteller_f.shared.model.A_FILE_DEFAULT_BUCKET
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.UploadRecordStatus
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import java.lang.reflect.Proxy
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame

class UploadQuotaSnapshotBoundaryTest {
    @Test
    fun pendingSnapshotListsOnlyItsChunkPrefixOnce() =
        runBlocking {
            val calls = mutableListOf<Pair<String, String>>()
            val backend =
                backendWithStorage { bucket, prefix ->
                    calls.add(bucket to prefix)
                    CustomResult.success(
                        listOf(
                            chunkRecord("chunks/120/chunk_0"),
                            chunkRecord("chunks/120/chunk_1"),
                            chunkRecord("chunks/120/chunk_6"),
                        ),
                    )
                }

            val result = backend.loadUploadQuotaSnapshot(uploadRecord())

            assertEquals(listOf(A_FILE_DEFAULT_BUCKET to "chunks/120/"), calls)
            assertEquals(24L * 1024 * 1024, result.getOrNull()?.progress)
        }

    @Test
    fun storageFailureMapsToContainerErrorAndRetainsCause() =
        runBlocking {
            val cause = IllegalStateException("chunk listing failed")
            val backend =
                backendWithStorage { _, _ ->
                    CustomResult.failure(ObjectStorageError.Unexpected(cause))
                }

            val result = backend.loadUploadQuotaSnapshot(uploadRecord())

            val failure = assertIs<CustomResult.Failure<ContainerError>>(result)
            val error = assertIs<ContainerError.Unexpected>(failure.error)
            assertSame(cause, error.cause)
        }

    @Test
    fun terminalAndMissingRecordsDoNotListChunks() =
        runBlocking {
            val calls = mutableListOf<Pair<String, String>>()
            val backend =
                backendWithStorage { bucket, prefix ->
                    calls.add(bucket to prefix)
                    CustomResult.success(emptyList())
                }
            val record = uploadRecord()

            val success = backend.loadUploadQuotaSnapshot(record.copy(status = UploadRecordStatus.SUCCESS))
            val aborted = backend.loadUploadQuotaSnapshot(record.copy(status = UploadRecordStatus.ABORTED))
            val failed = backend.loadUploadQuotaSnapshot(record.copy(status = UploadRecordStatus.FAILED))
            val missing = backend.loadUploadQuotaSnapshot(null)

            assertEquals(record.total, success.getOrNull()?.progress)
            assertEquals(record.progress, aborted.getOrNull()?.progress)
            assertEquals(record.progress, failed.getOrNull()?.progress)
            assertEquals(null, missing.getOrNull())
            assertEquals(emptyList(), calls)
        }

    @Test
    fun storageCancellationPropagatesAsTheSameInstance() {
        val cancellation = CancellationException("chunk listing cancelled")
        val backend = backendWithStorage { _, _ -> throw cancellation }

        val thrown =
            assertFailsWith<CancellationException> {
                runBlocking { backend.loadUploadQuotaSnapshot(uploadRecord()) }
            }

        assertSame(cancellation, thrown)
    }
}

private fun backendWithStorage(
    onList: (String, String) -> CustomResult<List<ObjectStorageRecord>, ObjectStorageError>,
): Backend {
    val storage =
        Proxy.newProxyInstance(
            ObjectStorageService::class.java.classLoader,
            arrayOf(ObjectStorageService::class.java),
        ) { _, method, arguments ->
            check(method.name == "list") { "Unexpected object storage call: ${method.name}" }
            val callArguments = requireNotNull(arguments)
            onList(callArguments[0] as String, callArguments[1] as String)
        } as ObjectStorageService
    return Proxy.newProxyInstance(
        Backend::class.java.classLoader,
        arrayOf(Backend::class.java),
    ) { _, method, _ ->
        check(method.name == "getObjectStorageService") { "Unexpected backend call: ${method.name}" }
        storage
    } as Backend
}

private fun chunkRecord(fullName: String) =
    ObjectStorageRecord(
        url = "https://fixture.invalid/$fullName",
        lastModified = LocalDateTime(2026, 11, 17, 0, 0),
        fullName = fullName,
    )

private fun uploadRecord() =
    UploadRecord(
        id = 120L,
        createdTime = LocalDateTime(2026, 11, 17, 0, 0),
        objectId = 1L,
        objectType = ObjectType.USER,
        status = UploadRecordStatus.PENDING,
        total = 64L * 1024 * 1024,
        progress = 0L,
        name = "synthetic.bin",
        chunkSize = 10L * 1024 * 1024,
        sha256 = "281e519df3077b557c6b03f5da83c4e8d397219259615dd7c3308f89cae8f2a6",
        crc32c = "hkV4iw==",
    )
