/*
 * This is a private project. All rights reserved.
 */

package fonts

import com.storyteller_f.alphaheart.app.common.DownloadFileLease
import com.storyteller_f.alphaheart.app.common.FileEngine
import com.storyteller_f.alphaheart.app.common.createFileEngine
import com.storyteller_f.alphaheart.app.pages.community.CommunityFontDownload
import com.storyteller_f.alphaheart.app.pages.community.CommunityFontState
import com.storyteller_f.alphaheart.app.pages.community.CommunityFontStore
import com.storyteller_f.storage.DownloadStatus
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DownloadRetryStateTest {
    @Test
    fun retryPublishesLoadingAndIgnoresDuplicateCommands() =
        runTest {
            val entered = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            var requests = 0
            HttpClient(
                MockEngine {
                    if (++requests == 2) {
                        entered.complete(Unit)
                        release.await()
                    }
                    respond(byteArrayOf(), HttpStatusCode.Forbidden)
                },
            ).use { client ->
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
                    val file = fontFile(CommunityFontTestData.primary)
                    val states = mutableListOf<CommunityFontState>()
                    backgroundScope.launch { store.observeFont(file).collect { states += it } }
                    store.observeFont(file).first { it is CommunityFontState.Failed }
                    store.retry(file)
                    entered.await()
                    runCurrent()
                    assertIs<CommunityFontState.Loading>(states.last())
                    store.retry(file)
                    fixture.downloader.resume(file.id)
                    fixture.downloader.resume(file.copy(url = "https://fonts.invalid/updated"))
                    assertNull(fixture.downloader.download(file))
                    release.complete(Unit)
                    store.observeFont(file).first { it is CommunityFontState.Failed }
                    runCurrent()
                    assertEquals(2, requests)
                    store.retry(file)
                    fixture.downloader.observeTask(file.id).first { !it }
                    runCurrent()
                    assertEquals(3, requests)
                    assertIs<CommunityFontState.Failed>(states.last())
                } finally {
                    release.complete(Unit)
                    fixture.close()
                }
            }
        }

    @Test
    fun failureControlsWaitForFileLeaseCleanup() =
        runTest {
            val cleaning = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val disk = createFileEngine()
            val files =
                object : FileEngine by disk {
                    override suspend fun acquire(key: String): DownloadFileLease {
                        val lease = disk.acquire(key)
                        return DownloadFileLease {
                            cleaning.complete(Unit)
                            release.await()
                            lease.close()
                        }
                    }
                }
            HttpClient(MockEngine { respond(byteArrayOf(), HttpStatusCode.Forbidden) }).use { client ->
                val fixture = DownloadTestFixture(this, files, client)
                try {
                    val source =
                        CommunityFontDownload(
                            fixture.records,
                            fixture.downloader,
                            fixture.files,
                            fixture.namespace,
                        )
                    val store = CommunityFontStore(backgroundScope, source::observe, fixture.downloader::resume)
                    val file = fontFile(CommunityFontTestData.primary)
                    val states = mutableListOf<CommunityFontState>()
                    backgroundScope.launch { store.observeFont(file).collect { states += it } }
                    fixture.records.observeDocumentByFileId(file.id).first {
                        it?.status == DownloadStatus.DOWNLOAD_FAILED
                    }
                    cleaning.await()
                    runCurrent()
                    assertIs<CommunityFontState.Loading>(states.last())
                    release.complete(Unit)
                    store.observeFont(file).first { it is CommunityFontState.Failed }
                    assertEquals(false, fixture.downloader.observeTask(file.id).first())
                } finally {
                    release.complete(Unit)
                    fixture.close()
                }
            }
        }
}
