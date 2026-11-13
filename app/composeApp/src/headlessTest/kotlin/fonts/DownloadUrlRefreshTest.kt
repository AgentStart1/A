/*
 * This is a private project. All rights reserved.
 */

package fonts

import com.storyteller_f.alphaheart.app.common.createDownloadRecord
import com.storyteller_f.alphaheart.app.common.createFileEngine
import com.storyteller_f.alphaheart.app.common.write
import com.storyteller_f.alphaheart.app.pages.community.CommunityFontDownload
import com.storyteller_f.alphaheart.app.pages.community.CommunityFontState
import com.storyteller_f.alphaheart.app.pages.community.CommunityFontStore
import com.storyteller_f.alphaheart.app.pages.community.observeCommunityFont
import com.storyteller_f.shared.commonJson
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.storage.DownloadInfo
import com.storyteller_f.storage.DownloadStatus
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DownloadUrlRefreshTest {
    private val expired = "https://fonts.invalid/old?X-Amz-Date=20000101T000000Z&X-Amz-Expires=60"
    private val valid = "https://fonts.invalid/current"

    @Test
    fun expiredUrlIsRefreshedBeforeAnySourceRequest() =
        runTest {
            val file = fontFile(CommunityFontTestData.primary).copy(url = expired)
            val http = RefreshClient(file.copy(url = valid))
            http.client.use { client ->
                val fixture = DownloadTestFixture(this, createFileEngine(), client)
                try {
                    fixture.downloader.download(file)
                    val record = fixture.terminal()
                    assertEquals(DownloadStatus.PROCESSED, record.status, record.message)
                    assertEquals(valid, record.fileInfo.url)
                    assertEquals(1, http.metadataRequests)
                    assertEquals(listOf(valid), http.downloads)
                } finally {
                    fixture.close()
                }
            }
        }

    @Test
    fun unauthorizedRefreshKeepsTheGuestOnApplicationFonts() =
        runTest {
            val file = fontFile(CommunityFontTestData.primary).copy(url = expired)
            val http = RefreshClient(file.copy(url = valid), HttpStatusCode.Unauthorized)
            http.client.use { client ->
                val fixture = DownloadTestFixture(this, createFileEngine(), client)
                try {
                    val source =
                        CommunityFontDownload(
                            fixture.records,
                            fixture.downloader,
                            fixture.files,
                            fixture.namespace,
                        )
                    val store = CommunityFontStore(backgroundScope, source::observe, fixture.downloader::resume)
                    store.observeFont(file).first { it is CommunityFontState.Failed }
                    assertNull(observeCommunityFont(file, store).first().family)
                    assertEquals(DownloadStatus.DOWNLOAD_FAILED, fixture.terminal().status)
                    assertEquals(1, http.metadataRequests)
                    assertTrue(http.downloads.isEmpty())
                } finally {
                    fixture.close()
                }
            }
        }

    @Test
    fun completeCachedSourcesDoNotNeedAuthenticatedRefresh() =
        runTest {
            val file = fontFile(CommunityFontTestData.primary).copy(url = expired)
            val http = RefreshClient(file.copy(url = valid), HttpStatusCode.Unauthorized)
            http.client.use { client ->
                val fixture = DownloadTestFixture(this, createFileEngine(), client)
                val path = fixture.files.location("public-resources-v1/${fixture.namespace}/cached/source")
                try {
                    fixture.files.write(path, CommunityFontTestData.primary)
                    val cached =
                        createDownloadRecord(file, path).copy(
                            status = DownloadStatus.PROCESSED,
                            processedGeneration = 1,
                        )
                    fixture.records.save(cached)
                    fixture.downloader.resume(file.id)
                    assertEquals(DownloadStatus.PROCESSED, fixture.terminal(2).status)
                    assertEquals(0, http.metadataRequests)
                    assertTrue(http.downloads.isEmpty())
                } finally {
                    fixture.close()
                }
            }
        }

    @Test
    fun refreshPreservesThePartialSourceAndRangeOffset() =
        runTest {
            val bytes = CommunityFontTestData.primary
            val file = fontFile(bytes).copy(url = expired)
            val offset = bytes.size / 2
            val http = RefreshClient(file.copy(url = valid))
            http.client.use { client ->
                val fixture = DownloadTestFixture(this, createFileEngine(), client)
                val path = fixture.files.location("public-resources-v1/${fixture.namespace}/partial/source")
                try {
                    fixture.files.write(path, bytes.copyOfRange(0, offset))
                    fixture.records.save(createDownloadRecord(file, path).copy(status = DownloadStatus.PAUSED))
                    fixture.downloader.resume(file.id)
                    val record = fixture.terminal()
                    assertEquals(DownloadStatus.PROCESSED, record.status, record.message)
                    assertEquals("bytes=$offset-", http.range)
                    assertEquals(path, record.path)
                    assertContentEquals(bytes, fixture.files.read(path, file.size))
                } finally {
                    fixture.close()
                }
            }
        }

    @Test
    fun changedMetadataIsRejectedBeforeDownloadingAnotherVersion() =
        runTest {
            val file = fontFile(CommunityFontTestData.primary).copy(url = expired)
            val changed = file.copy(url = valid, lastModified = LocalDateTime(2026, 1, 2, 0, 0))
            val http = RefreshClient(changed)
            http.client.use { client ->
                val fixture = DownloadTestFixture(this, createFileEngine(), client)
                try {
                    fixture.downloader.download(file)
                    val record = fixture.terminal()
                    assertEquals(DownloadStatus.DOWNLOAD_FAILED, record.status)
                    assertTrue(record.message.contains("changed"), record.message)
                    assertTrue(http.downloads.isEmpty())
                } finally {
                    fixture.close()
                }
            }
        }

    @Test
    fun stillExpiredRefreshFailsWithoutRepeatingTheRequest() =
        runTest {
            val file = fontFile(CommunityFontTestData.primary).copy(url = expired)
            val http = RefreshClient(file)
            http.client.use { client ->
                val fixture = DownloadTestFixture(this, createFileEngine(), client)
                try {
                    fixture.downloader.download(file)
                    val record = fixture.terminal()
                    assertEquals(DownloadStatus.DOWNLOAD_FAILED, record.status)
                    assertEquals(1, http.metadataRequests)
                    assertTrue(http.downloads.isEmpty())
                } finally {
                    fixture.close()
                }
            }
        }

    private suspend fun DownloadTestFixture.terminal(generation: Long = 1): DownloadInfo {
        val record =
            records.observeDocumentByFileId(1).first {
                when (it?.status) {
                    DownloadStatus.DOWNLOAD_FAILED -> true
                    DownloadStatus.PROCESSED -> it.processedGeneration >= generation
                    else -> false
                }
            }
        return checkNotNull(record)
    }
}

private class RefreshClient(latest: FileInfo, metadataStatus: HttpStatusCode = HttpStatusCode.OK) {
    var metadataRequests = 0
        private set
    val downloads = mutableListOf<String>()
    var range: String? = null
        private set
    val client =
        HttpClient(
            MockEngine { request ->
                if (request.url.encodedPath.endsWith("/files/${latest.id}")) {
                    metadataRequests++
                    respond(
                        commonJson.encodeToString(latest),
                        metadataStatus,
                        headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                } else {
                    downloads += request.url.toString()
                    check(request.url.toString() == latest.url) { "Unexpected source request" }
                    range = request.headers[HttpHeaders.Range]
                    val offset = range?.removePrefix("bytes=")?.removeSuffix("-")?.toInt() ?: 0
                    respond(
                        CommunityFontTestData.primary.copyOfRange(offset, CommunityFontTestData.primary.size),
                        if (offset == 0) HttpStatusCode.OK else HttpStatusCode.PartialContent,
                    )
                }
            },
        ) {
            expectSuccess = true
            install(ContentNegotiation) { json(commonJson) }
        }
}
