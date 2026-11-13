/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.core

import com.storyteller_f.alphaheart.backend.core.service.CopyPack
import com.storyteller_f.alphaheart.backend.core.service.ObjectStorageService
import com.storyteller_f.alphaheart.backend.core.service.UploadPack
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.model.A_FILE_DEFAULT_BUCKET
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ObjectStorageServiceTest {
    private fun createTempUploadPack(name: String, content: String): UploadPack {
        val file =
            File.createTempFile("test-upload-", ".txt").apply {
                writeText(content)
                deleteOnExit()
            }
        return UploadPack(
            file = file,
            name = name,
            size = file.length(),
            fullName = "test/$name",
            sha256 = "test-sha256-$name",
        )
    }

    private val uploadAndGet: suspend (ObjectStorageService) -> Unit = { service ->
        val pack = createTempUploadPack("file1.txt", "hello world")
        val writeResult = service.upload(A_FILE_DEFAULT_BUCKET, listOf(pack)).getOrThrow()
        assertEquals(1, writeResult.size)
        assertEquals("test/file1.txt", writeResult[0].fullName)

        val getResult = service.get(A_FILE_DEFAULT_BUCKET, listOf("test/file1.txt")).getOrThrow()
        assertEquals(1, getResult.size)
        assertEquals("test/file1.txt", getResult[0].fullName)
    }

    private val uploadAndList: suspend (ObjectStorageService) -> Unit = { service ->
        val pack1 = createTempUploadPack("list1.txt", "content1")
        val pack2 = createTempUploadPack("list2.txt", "content2")
        service.upload(A_FILE_DEFAULT_BUCKET, listOf(pack1, pack2)).getOrThrow()

        val listResult = service.list(A_FILE_DEFAULT_BUCKET, "test/").getOrThrow()
        assertTrue(listResult.size >= 2)
    }

    private val uploadAndGetInputStream: suspend (ObjectStorageService) -> Unit = { service ->
        val content = "stream content test"
        val pack = createTempUploadPack("stream.txt", content)
        service.upload(A_FILE_DEFAULT_BUCKET, listOf(pack)).getOrThrow()

        val inputStream = service.getInputStream(A_FILE_DEFAULT_BUCKET, "test/stream.txt").getOrThrow()
        val readContent = inputStream.bufferedReader().use { it.readText() }
        assertEquals(content, readContent)
    }

    private val copy: suspend (ObjectStorageService) -> Unit = { service ->
        val pack = createTempUploadPack("origin.txt", "copy me")
        service.upload(A_FILE_DEFAULT_BUCKET, listOf(pack)).getOrThrow()

        val copyResult =
            service.copy(
                A_FILE_DEFAULT_BUCKET,
                listOf(CopyPack("test/origin.txt", "test/copied.txt")),
            ).getOrThrow()
        assertEquals(1, copyResult.size)

        val getResult = service.get(A_FILE_DEFAULT_BUCKET, listOf("test/copied.txt")).getOrThrow()
        assertEquals(1, getResult.size)
    }

    private val delete: suspend (ObjectStorageService) -> Unit = { service ->
        val pack = createTempUploadPack("to-delete.txt", "delete me")
        service.upload(A_FILE_DEFAULT_BUCKET, listOf(pack)).getOrThrow()

        service.delete(A_FILE_DEFAULT_BUCKET, listOf("test/to-delete.txt")).getOrThrow()

        val getResult = service.get(A_FILE_DEFAULT_BUCKET, listOf("test/to-delete.txt")).getOrThrow()
        assertTrue(getResult.isEmpty())
    }

    private val clean: suspend (ObjectStorageService) -> Unit = { service ->
        val pack = createTempUploadPack("clean-me.txt", "to be cleaned")
        service.upload(A_FILE_DEFAULT_BUCKET, listOf(pack)).getOrThrow()

        service.clean(A_FILE_DEFAULT_BUCKET).getOrThrow()

        val getResult = service.get(A_FILE_DEFAULT_BUCKET, listOf("test/clean-me.txt")).getOrThrow()
        assertTrue(getResult.isEmpty())
    }

    private fun getResponseContentType(url: String): String? {
        val request = HttpRequest.newBuilder(URI(url)).GET().build()
        val response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.discarding())
        return response.headers().firstValue("content-type").orElse(null)
    }

    @Test
    fun `test upload and get rpc`() = testOssRpc(uploadAndGet)

    @Test
    fun `test upload and get container`() = testOssContainer(uploadAndGet)

    @Test
    fun `test upload and list rpc`() = testOssRpc(uploadAndList)

    @Test
    fun `test upload and list container`() = testOssContainer(uploadAndList)

    @Test
    fun `test upload and get input stream rpc`() = testOssRpc(uploadAndGetInputStream)

    @Test
    fun `test upload and get input stream container`() = testOssContainer(uploadAndGetInputStream)

    @Test
    fun `test copy rpc`() = testOssRpc(copy)

    @Test
    fun `test copy container`() = testOssContainer(copy)

    @Test
    fun `test delete rpc`() = testOssRpc(delete)

    @Test
    fun `test delete container`() = testOssContainer(delete)

    @Test
    fun `test clean rpc`() = testOssRpc(clean)

    @Test
    fun `test clean container`() = testOssContainer(clean)

    @Test
    internal fun `test response content type override container`() {
        testOssContainer { service ->
            val pack = createTempUploadPack("legacy.bin", "content")
            service.upload(A_FILE_DEFAULT_BUCKET, listOf(pack)).getOrThrow()
            val record =
                service.getWithPresignContext(
                    bucketName = A_FILE_DEFAULT_BUCKET,
                    names = listOf(pack.fullName),
                    presignContext = null,
                    responseContentTypes = mapOf(pack.fullName to "image/avif"),
                ).getOrThrow().single()

            assertEquals("image/avif", getResponseContentType(record.url))
        }
    }
}
