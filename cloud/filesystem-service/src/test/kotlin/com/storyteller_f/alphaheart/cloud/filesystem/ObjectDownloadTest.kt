/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.filesystem

import com.storyteller_f.result.getOrThrow
import io.ktor.client.request.get
import io.ktor.client.request.head
import io.ktor.client.request.header
import io.ktor.client.request.options
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ObjectDownloadTest {
    @Test
    fun `direct downloads support byte ranges`() =
        withObjectDownloads { storage, directory ->
            val source = Files.writeString(directory.resolve("upload.txt"), "abcdef").toFile()
            storage.upload("custom", listOf(UploadPack(source, "test.txt"))).getOrThrow()
            val url = storage.get("custom", listOf("test.txt")).getOrThrow().single().url
            assertEquals("http://localhost/objects/custom/test.txt", url)
            assertEquals("abcdef", client.get(url).bodyAsText())
            val head = client.head(url)
            assertEquals(HttpStatusCode.OK, head.status)
            assertEquals("6", head.headers[HttpHeaders.ContentLength])
            assertEquals("", head.bodyAsText())
            val partial = client.get(url) { header(HttpHeaders.Range, "bytes=1-3") }
            assertEquals(HttpStatusCode.PartialContent, partial.status)
            assertEquals("bcd", partial.bodyAsText())
            assertEquals("*", partial.headers[HttpHeaders.AccessControlAllowOrigin])
            assertEquals(HttpStatusCode.NotFound, client.get("/rpc").status)
            assertEquals(HttpStatusCode.NotFound, client.get("/objects/custom/missing.txt").status)
        }

    @Test
    fun `object reads accept browser preflight`() =
        withObjectDownloads { _, _ ->
            val response =
                client.options("/objects/custom/test.txt") {
                    header(HttpHeaders.Origin, "http://reader.invalid")
                    header(HttpHeaders.AccessControlRequestMethod, "GET")
                    header(HttpHeaders.AccessControlRequestHeaders, "content-type,cache-control,range")
                }
            assertEquals(HttpStatusCode.NoContent, response.status)
            assertEquals("*", response.headers[HttpHeaders.AccessControlAllowOrigin])
            assertEquals("GET, HEAD", response.headers[HttpHeaders.AccessControlAllowMethods])
            assertEquals("*", response.headers[HttpHeaders.AccessControlAllowHeaders])
            assertNull(response.headers[HttpHeaders.AccessControlAllowCredentials])
        }

    @Test
    fun `object head requests accept preflight`() =
        withObjectDownloads { _, _ ->
            val response =
                client.options("/objects/custom/test.txt") {
                    header(HttpHeaders.Origin, "http://reader.invalid")
                    header(HttpHeaders.AccessControlRequestMethod, "HEAD")
                }
            assertEquals(HttpStatusCode.NoContent, response.status)
            assertEquals("GET, HEAD", response.headers[HttpHeaders.AccessControlAllowMethods])
        }

    @Test
    fun `object preflight refuses mutations`() =
        withObjectDownloads { _, _ ->
            val response =
                client.options("/objects/custom/test.txt") {
                    header(HttpHeaders.Origin, "http://reader.invalid")
                    header(HttpHeaders.AccessControlRequestMethod, "POST")
                }
            assertEquals(HttpStatusCode.Forbidden, response.status)
            assertNull(response.headers[HttpHeaders.AccessControlAllowOrigin])
        }

    @Test
    fun `object preflight does not expose rpc`() =
        withObjectDownloads { _, _ ->
            val response =
                client.options("/rpc") {
                    header(HttpHeaders.Origin, "http://reader.invalid")
                    header(HttpHeaders.AccessControlRequestMethod, "POST")
                }
            assertEquals(HttpStatusCode.NotFound, response.status)
            assertNull(response.headers[HttpHeaders.AccessControlAllowOrigin])
        }

    @Test
    fun `missing objects include cors response headers`() =
        withObjectDownloads { _, _ ->
            val response =
                client.get("/objects/custom/missing.txt") {
                    header(HttpHeaders.Origin, "http://reader.invalid")
                }
            assertEquals(HttpStatusCode.NotFound, response.status)
            assertEquals("*", response.headers[HttpHeaders.AccessControlAllowOrigin])
        }
}

private fun withObjectDownloads(
    block: suspend ApplicationTestBuilder.(LocalFileSystemObjectStorageService, Path) -> Unit,
) = testApplication {
    val parent = Path.of("build", "test", "object-download").toAbsolutePath().normalize().createDirectories()
    val directory = Files.createTempDirectory(parent, "case-")
    AutoCloseable {
        check(directory.startsWith(parent))
        check(directory.toFile().deleteRecursively())
    }.use {
        val storage = LocalFileSystemObjectStorageService("http://localhost", directory.resolve("objects"))
        application { configureObjectDownloads(storage) }
        block(storage, directory)
    }
}
