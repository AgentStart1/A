/*
 * This is a private project. All rights reserved.
 */

package fonts

import com.storyteller_f.alphaheart.app.common.createFileEngine
import com.storyteller_f.alphaheart.app.common.write
import com.storyteller_f.alphaheart.app.pages.community.CommunityFontDownload
import com.storyteller_f.alphaheart.app.pages.community.CommunityFontState
import com.storyteller_f.alphaheart.app.pages.community.CommunityFontStore
import com.storyteller_f.storage.DownloadStatus
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertSame

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PublicFontGenerationTest {
    @Test
    fun skippedIntermediateSnapshotsStillRebuildTheNewGenerationOnce() =
        runTest {
            HttpClient(MockEngine { respond(CommunityFontTestData.primary) }).use { client ->
                val fixture = DownloadTestFixture(this, createFileEngine(), client)
                try {
                    val file = fontFile(CommunityFontTestData.primary)
                    var builds = 0
                    val source =
                        CommunityFontDownload(
                            fixture.records,
                            fixture.downloader,
                            fixture.files,
                            fixture.namespace,
                            onLoaded = { builds++ },
                        )
                    val store =
                        CommunityFontStore(backgroundScope, source::observe) { fixture.downloader.resume(it) }
                    backgroundScope.launch { store.observeFont(file).collect {} }
                    val first = assertNotNull(store.load(file))
                    val record = assertNotNull(fixture.records.getDocumentByFileId(file.id))
                    // A conflated database observer is allowed to see only the new successful snapshot.
                    fixture.records.save(record.copy(processedGeneration = record.processedGeneration + 1))
                    val replacement =
                        store.observeFont(file).first {
                            it is CommunityFontState.Ready && it.bundle !== first
                        } as CommunityFontState.Ready
                    assertNotSame(first.loaded.family, replacement.bundle.loaded.family)
                    assertSame(replacement.bundle, store.load(file))
                    assertEquals(2, builds)
                } finally {
                    fixture.close()
                }
            }
        }

    @Test
    fun ordinaryCacheRegistrationDoesNotReconstructAnActiveFont() =
        runTest {
            HttpClient(MockEngine { respond(CommunityFontTestData.primary) }).use { client ->
                val fixture = DownloadTestFixture(this, createFileEngine(), client)
                try {
                    val file = fontFile(CommunityFontTestData.primary)
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
                    backgroundScope.launch { store.observeFont(file).collect {} }
                    val first = assertNotNull(store.load(file))
                    fixture.downloader.download(file)
                    fixture.downloader.observeTask(file.id).first { !it }
                    runCurrent()
                    assertSame(first, store.load(file))
                    assertEquals(1, builds)
                } finally {
                    fixture.close()
                }
            }
        }

    @Test
    fun unrelatedRowsAndProgressDoNotReconstructTheSuccessfulGeneration() =
        runTest {
            HttpClient(MockEngine { respond(CommunityFontTestData.primary) }).use { client ->
                val fixture = DownloadTestFixture(this, createFileEngine(), client)
                try {
                    val file = fontFile(CommunityFontTestData.primary)
                    var builds = 0
                    val source =
                        CommunityFontDownload(
                            fixture.records,
                            fixture.downloader,
                            fixture.files,
                            fixture.namespace,
                            onLoaded = { builds++ },
                        )
                    val store =
                        CommunityFontStore(backgroundScope, source::observe) { fixture.downloader.resume(it) }
                    backgroundScope.launch { store.observeFont(file).collect {} }
                    val first = assertNotNull(store.load(file))
                    val record = assertNotNull(fixture.records.getDocumentByFileId(file.id))
                    fixture.records.save(record.copy(progress = 0))
                    fixture.records.save(record.copy(id = 2, fileInfo = file.copy(id = 2)))
                    runCurrent()
                    assertSame(first, store.load(file))
                    assertEquals(1, builds)
                } finally {
                    fixture.close()
                }
            }
        }

    @Test
    fun aFailedConstructionRecoversOnSameVersionResume() =
        runTest {
            HttpClient(MockEngine { respond(CommunityFontTestData.primary) }).use { client ->
                val fixture = DownloadTestFixture(this, createFileEngine(), client)
                try {
                    val file = fontFile(CommunityFontTestData.primary)
                    fixture.downloader.download(file)
                    val original =
                        assertNotNull(
                            fixture.records.observeDocumentByFileId(file.id).first {
                                it?.status == DownloadStatus.PROCESSED
                            },
                        )
                    fixture.files.write(original.path, ByteArray(file.size.toInt()))
                    val source =
                        CommunityFontDownload(
                            fixture.records,
                            fixture.downloader,
                            fixture.files,
                            fixture.namespace,
                        )
                    val store =
                        CommunityFontStore(backgroundScope, source::observe) { fixture.downloader.resume(it) }
                    backgroundScope.launch { store.observeFont(file).collect {} }
                    store.observeFont(file).first { it is CommunityFontState.Failed }
                    fixture.files.write(original.path, CommunityFontTestData.primary)
                    store.retry(file)
                    val recovered =
                        store.observeFont(
                            file,
                        ).first { it is CommunityFontState.Ready } as CommunityFontState.Ready
                    assertNotNull(recovered.bundle.loaded.family)
                    assertEquals(
                        original.processedGeneration + 1,
                        fixture.records.getDocumentByFileId(file.id)?.processedGeneration,
                    )
                } finally {
                    fixture.close()
                }
            }
        }

    @Test
    fun sharedFileLockSerializesGenerationsAcrossDownloaderInstances() =
        runTest {
            HttpClient(MockEngine { respond(CommunityFontTestData.primary) }).use { client ->
                val fixture = DownloadTestFixture(this, createFileEngine(), client)
                val other = DownloadTestFixture(this, createFileEngine(), client, fixture.namespace, fixture.records)
                try {
                    val file = fontFile(CommunityFontTestData.primary)
                    fixture.downloader.download(file)
                    fixture.records.observeDocumentByFileId(file.id).first { it?.status == DownloadStatus.PROCESSED }
                    fixture.downloader.resume(file.id)
                    other.downloader.resume(file.id)
                    val completed =
                        fixture.records.observeDocumentByFileId(file.id).first {
                            it?.processedGeneration ==
                                3L
                        }
                    assertEquals(DownloadStatus.PROCESSED, completed?.status)
                } finally {
                    fixture.close()
                }
            }
        }
}
