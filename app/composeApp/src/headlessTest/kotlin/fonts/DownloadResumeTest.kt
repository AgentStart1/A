/*
 * This is a private project. All rights reserved.
 */

package fonts

import com.storyteller_f.alphaheart.app.common.createFileEngine
import com.storyteller_f.storage.DownloadInfo
import com.storyteller_f.storage.DownloadStatus
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DownloadResumeTest {
    @Test
    fun renewedUrlReusesTheCompletedSource() =
        runTest {
            var requests = 0
            HttpClient(
                MockEngine {
                    requests++
                    respond(CommunityFontTestData.primary)
                },
            ).use { client ->
                val fixture = DownloadTestFixture(this, createFileEngine(), client)
                try {
                    val file = fontFile(CommunityFontTestData.primary)
                    fixture.downloader.download(file)
                    val first = fixture.completed(file.id, 1)
                    val refreshed = file.copy(url = "https://fonts.invalid/renewed")
                    fixture.downloader.resume(refreshed)
                    val second = fixture.completed(file.id, 2)
                    assertEquals(first.path, second.path)
                    assertEquals(refreshed.url, second.fileInfo.url)
                    assertEquals(1, requests)
                } finally {
                    fixture.close()
                }
            }
        }

    @Test
    fun staleRetryCannotReplaceANewerStoredRevision() =
        runTest {
            HttpClient(MockEngine { respond(CommunityFontTestData.primary) }).use { client ->
                val fixture = DownloadTestFixture(this, createFileEngine(), client)
                try {
                    val stale =
                        fontFile(CommunityFontTestData.primary)
                            .copy(lastModified = LocalDateTime(2026, 1, 1, 0, 0))
                    val current =
                        stale.copy(
                            lastModified = LocalDateTime(2026, 2, 3, 0, 0),
                            url = "https://fonts.invalid/current",
                        )
                    fixture.downloader.download(current)
                    val first = fixture.completed(current.id, 1)
                    fixture.downloader.resume(stale.copy(url = "https://fonts.invalid/stale-renewed"))
                    val second = fixture.completed(current.id, 2)
                    assertEquals(current, second.fileInfo)
                    assertEquals(first.path, second.path)
                } finally {
                    fixture.close()
                }
            }
        }

    @Test
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun activeRetryIgnoresEvenARenewedUrl() =
        runTest {
            val entered = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val expired = "https://fonts.invalid/expired"
            val renewed = "https://fonts.invalid/renewed"
            val requests = mutableListOf<String>()
            HttpClient(
                MockEngine { request ->
                    requests += request.url.toString()
                    if (request.url.toString() == expired) {
                        if (requests.size == 2) {
                            entered.complete(Unit)
                            release.await()
                        }
                        respond(byteArrayOf(), HttpStatusCode.Forbidden)
                    } else {
                        respond(CommunityFontTestData.primary)
                    }
                },
            ).use { client ->
                val fixture = DownloadTestFixture(this, createFileEngine(), client)
                val file = fontFile(CommunityFontTestData.primary).copy(url = expired)
                try {
                    fixture.downloader.download(file)
                    fixture.records.observeDocumentByFileId(file.id).first {
                        it?.status == DownloadStatus.DOWNLOAD_FAILED
                    }
                    fixture.downloader.resume(file)
                    entered.await()
                    fixture.downloader.resume(file.copy(url = renewed))
                    release.complete(Unit)
                    fixture.downloader.observeTask(file.id).first { !it }
                    assertEquals(listOf(expired, expired), requests)
                    assertEquals(DownloadStatus.DOWNLOAD_FAILED, fixture.records.getDocumentByFileId(file.id)?.status)
                    fixture.downloader.resume(file.copy(url = renewed))
                    assertEquals(renewed, fixture.completed(file.id, 1).fileInfo.url)
                    assertEquals(listOf(expired, expired, renewed), requests)
                } finally {
                    release.complete(Unit)
                    fixture.close()
                }
            }
        }

    private suspend fun DownloadTestFixture.completed(id: Long, generation: Long): DownloadInfo {
        val completed =
            records.observeDocumentByFileId(id).first {
                it?.status == DownloadStatus.PROCESSED && it.processedGeneration == generation
            }
        downloader.observeTask(id).first { !it }
        return assertNotNull(completed)
    }
}
