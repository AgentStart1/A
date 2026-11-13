/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.minio

import com.storyteller_f.alphaheart.backend.core.ContainerImages
import com.storyteller_f.alphaheart.backend.core.MinIoConnection
import com.storyteller_f.alphaheart.backend.core.service.UploadPack
import com.storyteller_f.result.exceptionOrNull
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.model.FileChecksums
import kotlinx.coroutines.test.runTest
import org.testcontainers.containers.MinIOContainer
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ComposeChecksumTest {
    @Test
    fun `multipart compose validates CRC32C not SHA256`() =
        runTest {
            withStorage { service, expected ->
                assertEquals(
                    "combined",
                    service.compose(
                        "checksums",
                        "combined",
                        listOf("first", "last"),
                        "0".repeat(64),
                        expected.crc32c,
                    ).getOrThrow().fullName,
                )
                val actual =
                    service.getInputStream("checksums", "combined").getOrThrow().buffered().use {
                        computeChecksums(it.readBytes())
                    }
                assertEquals(expected, actual)
            }
        }

    @Test
    fun `CRC32C mismatch retains parts without a target`() =
        runTest {
            withStorage { service, expected ->
                val result =
                    service.compose(
                        "checksums",
                        "combined",
                        listOf("first", "last"),
                        expected.sha256,
                        "AAAAAA==",
                    )
                assertTrue(
                    generateSequence(result.exceptionOrNull()) { it.cause }
                        .any { it.message == "file crc32c mismatch" },
                )
                val objects = service.list("checksums", "").getOrThrow().map { it.fullName }
                assertFalse("combined" in objects)
                assertEquals(setOf("first", "last"), objects.toSet())
            }
        }

    @Test
    fun `single source copy also verifies CRC32C`() =
        runTest {
            withStorage(singleSource = true) { service, _ ->
                val result =
                    service.compose(
                        "checksums",
                        "combined",
                        listOf("last"),
                        "0".repeat(64),
                        "AAAAAA==",
                    )
                assertTrue(
                    generateSequence(result.exceptionOrNull()) { it.cause }
                        .any { it.message == "file crc32c mismatch" },
                )
                assertFalse(service.list("checksums", "").getOrThrow().any { it.fullName == "combined" })
            }
        }

    private fun computeChecksums(bytes: ByteArray): FileChecksums {
        val crc = java.util.zip.CRC32C().apply { update(bytes) }.value
        val encoded =
            java.util.Base64.getEncoder().encodeToString(
                java.nio.ByteBuffer.allocate(4).putInt(crc.toInt()).array(),
            )
        val sha = java.security.MessageDigest.getInstance("SHA-256").digest(bytes).toHexString()
        return FileChecksums(sha, encoded)
    }

    private suspend fun withStorage(
        singleSource: Boolean = false,
        block: suspend (MinIoObjectStorageService, FileChecksums) -> Unit,
    ) {
        MinIOContainer(ContainerImages.MINIO).use { container ->
            container.start()
            val service =
                MinIoObjectStorageService(
                    MinIoConnection(container.s3URL, container.userName, container.password),
                    null,
                )
            val last = "last source part".encodeToByteArray()
            val first = if (singleSource) byteArrayOf() else ByteArray(10 * 1024 * 1024) { it.toByte() }
            val expected = computeChecksums(first + last)
            val directory = Files.createTempDirectory(Files.createDirectories(Path.of("build/test/minio")), "compose-")
            try {
                val sources = if (singleSource) listOf("last" to last) else listOf("first" to first, "last" to last)
                val packs =
                    sources.map { (name, bytes) ->
                        val file = directory.resolve(name).toFile().apply { writeBytes(bytes) }
                        val checksums = computeChecksums(bytes)
                        UploadPack(file, name, bytes.size.toLong(), name, checksums.sha256)
                    }
                service.upload("checksums", packs).getOrThrow()
                block(service, expected)
            } finally {
                Files.deleteIfExists(directory.resolve("first"))
                Files.deleteIfExists(directory.resolve("last"))
                Files.deleteIfExists(directory)
            }
        }
    }
}
