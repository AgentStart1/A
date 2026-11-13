/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.filesystem

import com.storyteller_f.result.getOrThrow
import com.storyteller_f.services.filesystem.api.CopyPack
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocalFileSystemObjectStorageServiceTest {
    @Test
    fun `failed compose retains target and cleans temporary`() =
        runBlocking {
            val root = Files.createDirectories(java.nio.file.Path.of("build/test/filesystem-checksum"))
            val base = Files.createTempDirectory(root, "storage-")
            val source = Files.createTempFile(base, "filesystem-part-", ".txt").toFile()
            source.writeText("original")
            val service = LocalFileSystemObjectStorageService("https://example.invalid", base)
            service.upload("objects", listOf(UploadPack(source, "part"), UploadPack(source, "target"))).getOrThrow()
            assertTrue(service.compose("objects", "target", listOf("part"), "0".repeat(64)).isFailure)
            assertEquals(setOf("part", "target"), service.list("objects", "").getOrThrow().map { it.fullName }.toSet())
            service.getInputStream("objects", "target").getOrThrow().use {
                assertEquals("original", it.readBytes().decodeToString())
            }
        }

    @Test
    fun `upload returns independent storage model`() =
        withTestStorage { service, source ->
            assertEquals(
                "original.txt",
                service.upload("objects", listOf(UploadPack(source, "original.txt")))
                    .getOrThrow().single().fullName,
            )
        }

    @Test
    fun `copy creates a separate object`() =
        withTestStorage { service, source ->
            service.upload("objects", listOf(UploadPack(source, "original.txt"))).getOrThrow()
            val copied = service.copy("objects", listOf(CopyPack("original.txt", "copy.txt"))).getOrThrow().single()
            assertEquals("https://example.invalid/objects/objects/copy.txt", copied.url)
            assertEquals(
                setOf("original.txt", "copy.txt"),
                service.list("objects", "").getOrThrow()
                    .map { it.fullName }.toSet(),
            )
        }

    @Test
    fun `compose concatenates object contents`() =
        withTestStorage { service, source ->
            service.upload("objects", listOf(UploadPack(source, "original.txt"))).getOrThrow()
            service.copy("objects", listOf(CopyPack("original.txt", "copy.txt"))).getOrThrow()
            service.compose(
                "objects",
                "combined.txt",
                listOf("original.txt", "copy.txt"),
                java.security.MessageDigest.getInstance(
                    "SHA-256",
                ).digest("contentcontent".encodeToByteArray()).toHexString(),
            ).getOrThrow()
            service.getInputStream("objects", "combined.txt").getOrThrow().use {
                assertEquals("contentcontent", it.readBytes().decodeToString())
            }
        }

    @Test
    fun `delete removes selected object`() =
        withTestStorage { service, source ->
            service.upload("objects", listOf(UploadPack(source, "original.txt"))).getOrThrow()
            service.copy("objects", listOf(CopyPack("original.txt", "copy.txt"))).getOrThrow()
            service.delete("objects", listOf("copy.txt")).getOrThrow()
            assertTrue(service.get("objects", listOf("copy.txt")).getOrThrow().isEmpty())
            assertEquals(listOf("original.txt"), service.list("objects", "").getOrThrow().map { it.fullName })
        }

    @Test
    fun `clean removes bucket objects`() =
        withTestStorage { service, source ->
            service.upload("objects", listOf(UploadPack(source, "original.txt"))).getOrThrow()
            service.clean("objects").getOrThrow()
            assertTrue(service.list("objects", "").getOrThrow().isEmpty())
        }

    @Test
    fun `storage operations propagate cancellation`() =
        runBlocking<Unit> {
            val service =
                LocalFileSystemObjectStorageService(
                    "https://example.invalid",
                    createTempDirectory("filesystem-"),
                )
            assertFailsWith<CancellationException> {
                service.useFileSystem<Unit> { throw CancellationException("cancelled") }
            }
        }

    @Test
    fun `object paths cannot escape their bucket`() =
        runBlocking {
            val base = createTempDirectory("filesystem-storage-")
            val source = Files.createTempFile("filesystem-upload-", ".txt").toFile()
            source.writeText("private")
            val service = LocalFileSystemObjectStorageService("https://example.invalid", base)
            val escaped = base.resolve("escaped.txt")
            val pack =
                UploadPack(
                    file = source,
                    fullName = "../escaped.txt",
                )

            assertTrue(service.upload("files", listOf(pack)).isFailure)
            assertFalse(escaped.exists())
            assertTrue(service.getInputStream("files", "../escaped.txt").isFailure)
            assertTrue(service.delete("files", listOf("../escaped.txt")).isFailure)
            assertNull(service.getPathResponse(listOf("files", "..", "escaped.txt")))
        }
    private fun withTestStorage(block: suspend (LocalFileSystemObjectStorageService, java.io.File) -> Unit) =
        runBlocking {
            val root = Files.createDirectories(java.nio.file.Path.of("build/test/filesystem-lifecycle"))
            val base = Files.createTempDirectory(root, "storage-")
            val source = Files.createTempFile(base, "source-", ".txt").toFile()
            try {
                source.writeText("content")
                block(LocalFileSystemObjectStorageService("https://example.invalid", base), source)
            } finally {
                base.toFile().deleteRecursively()
            }
        }
}
