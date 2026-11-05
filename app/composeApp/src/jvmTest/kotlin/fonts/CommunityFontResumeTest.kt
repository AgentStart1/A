/*
 * This is a private project. All rights reserved.
 */

package fonts

import com.storyteller_f.a.app.common.createFileEngine
import com.storyteller_f.a.app.pages.community.CommunityFontDownload
import com.storyteller_f.a.app.pages.community.CommunityFontState
import com.storyteller_f.a.app.pages.community.CommunityFontStore
import com.storyteller_f.a.app.pages.community.fontFileKey
import com.storyteller_f.storage.DownloadStatus
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame

class CommunityFontResumeTest {
    @Test
    fun renewedUrlRecoversTheExistingSharedFontFlow() =
        runTest {
            val expired = "https://fonts.invalid/expired"
            val renewed = "https://fonts.invalid/renewed"
            val requests = mutableListOf<String>()
            HttpClient(
                MockEngine { request ->
                    requests += request.url.toString()
                    if (request.url.toString() == expired) {
                        respond(byteArrayOf(), HttpStatusCode.Forbidden)
                    } else {
                        assertEquals(renewed, request.url.toString())
                        respond(CommunityFontTestData.primary)
                    }
                },
            ).use { client ->
                val fixture = DownloadTestFixture(this, createFileEngine(), client)
                val file = fontFile(CommunityFontTestData.primary).copy(url = expired)
                val refreshed = file.copy(url = renewed)
                var builds = 0
                val source =
                    CommunityFontDownload(
                        fixture.records,
                        fixture.downloader,
                        fixture.files,
                        fixture.namespace,
                        onLoaded = { builds++ },
                    )
                val store = CommunityFontStore(backgroundScope, source::observe, fixture.downloader::resume)
                val states = store.observeFont(file)
                val observer = backgroundScope.launch { states.collect {} }
                try {
                    states.first { it is CommunityFontState.Failed }
                    val failed = assertNotNull(fixture.records.getDocumentByFileId(file.id))
                    assertEquals(DownloadStatus.DOWNLOAD_FAILED, failed.status)
                    assertEquals(fontFileKey(file), fontFileKey(refreshed))
                    store.retry(refreshed)
                    val ready = states.first { it is CommunityFontState.Ready } as CommunityFontState.Ready
                    val completed = assertNotNull(fixture.records.getDocumentByFileId(file.id))
                    assertEquals(1L, completed.processedGeneration)
                    assertNotNull(ready.bundle.loaded.family)
                    assertSame(ready, store.observeFont(refreshed).first { it is CommunityFontState.Ready })
                    assertEquals(failed.path, completed.path)
                    assertEquals(renewed, completed.fileInfo.url)
                    assertEquals(listOf(expired, renewed), requests)
                    assertEquals(1, builds)
                } finally {
                    observer.cancelAndJoin()
                    fixture.close()
                }
            }
        }
}
