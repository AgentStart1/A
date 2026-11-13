/*
 * This is a private project. All rights reserved.
 */

@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package fonts

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.storyteller_f.alphaheart.app.common.OpfsFileEngine
import com.storyteller_f.alphaheart.app.common.awaitExtraction
import com.storyteller_f.alphaheart.app.common.extractArchiveInWorker
import com.storyteller_f.alphaheart.app.common.extractDownloadArchive
import com.storyteller_f.alphaheart.app.common.extractedFiles
import com.storyteller_f.alphaheart.app.common.getDownloadRevision
import com.storyteller_f.alphaheart.app.common.withLock
import com.storyteller_f.alphaheart.app.common.write
import com.storyteller_f.alphaheart.app.pages.community.CommunityFontDownload
import com.storyteller_f.alphaheart.app.pages.community.CommunityFontStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.runTest
import org.jetbrains.skiko.wasm.onWasmReady
import kotlin.coroutines.resume
import kotlin.js.toJsString
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class CommunityFontBrowserTest {
    private val fontFixture = CommunityFontFixture()

    @AfterTest
    fun cleanFontFiles() = runTest { fontFixture.close() }

    @Test
    fun persistedZipLoadsIntoTheRealWasmResolver() =
        runTest {
            suspendCancellableCoroutine { continuation -> onWasmReady { continuation.resume(Unit) } }
            var requests = 0
            HttpClient(
                MockEngine {
                    requests++
                    respond(CommunityFontTestData.primaryZip)
                },
            ).use { client ->
                val fixture = DownloadTestFixture(this, OpfsFileEngine(), client)
                try {
                    val file = fontFile(CommunityFontTestData.primaryZip, "family.zip")
                    val source =
                        CommunityFontDownload(
                            fixture.records,
                            fixture.downloader,
                            fixture.files,
                            fixture.namespace,
                        )
                    val store = CommunityFontStore(this, source::observe) { fixture.downloader.resume(it.id) }
                    val first = assertNotNull(store.load(file))
                    assertSame(first.loaded.family, assertNotNull(store.load(file)).loaded.family)
                    assertSame(first.loaded.resolver, assertNotNull(store.load(file)).loaded.resolver)
                    val original = assertNotNull(fixture.records.getDocumentByFileId(file.id))
                    val recreated =
                        DownloadTestFixture(
                            this,
                            OpfsFileEngine(),
                            client,
                            fixture.namespace,
                            fixture.records,
                        )
                    val bundle =
                        CommunityFontDownload(
                            recreated.records,
                            recreated.downloader,
                            recreated.files,
                            recreated.namespace,
                        ).load(file)
                    assertEquals(
                        fixture.files.extractedFiles(original.path),
                        recreated.files.extractedFiles(original.path),
                    )
                    assertNotNull(first.loaded.family)
                    assertNotNull(bundle.loaded.family)
                    assertEquals(30, measureFont(bundle.loaded))
                    assertEquals(1, requests)
                } finally {
                    fixture.close()
                }
            }
        }

    @Test
    fun independentlyLoadedFamiliesKeepTheirGlyphMetrics() =
        runTest {
            suspendCancellableCoroutine { continuation -> onWasmReady { continuation.resume(Unit) } }
            val body = fontFixture.testFontBundle("body", "Regular.ttf", CommunityFontTestData.primary)
            val code = fontFixture.testFontBundle("code", "Regular.ttf", CommunityFontTestData.code)
            val fonts = buildTestCommunityFonts(body, code)
            val measurer = TextMeasurer(fonts.resolver ?: createFontFamilyResolver(), Density(1f), LayoutDirection.Ltr)
            val text = AnnotatedString("A")
            assertEquals(
                30,
                measurer.measure(text, TextStyle(fontFamily = fonts.fonts.content, fontSize = 100.sp)).size.width,
            )
            assertEquals(
                60,
                measurer.measure(text, TextStyle(fontFamily = fonts.fonts.code, fontSize = 100.sp)).size.width,
            )
        }

    @Test
    fun serverAndRevisionLocationsNeverShareBytes() =
        runTest {
            val files = OpfsFileEngine()
            val server = "test-${Uuid.random().toHexString()}"
            val other = "$server-other"
            val file = fontFile(CommunityFontTestData.primary)
            val path = files.location("$server/${file.id}/${getDownloadRevision(file)}/source")
            try {
                files.write(path, CommunityFontTestData.primary)
                assertContentEquals(CommunityFontTestData.primary, OpfsFileEngine().read(path, file.size))
                assertNull(files.size(files.location("$other/${file.id}/${getDownloadRevision(file)}/source")))
                assertNull(files.size(files.location("$server/${file.id}/other-version/source")))
            } finally {
                files.delete(files.location(server))
                files.delete(files.location(other))
            }
        }

    @Test
    fun cancelledLockWaitDoesNotBlockLaterReaders() =
        runTest {
            val files = OpfsFileEngine()
            val key = "test-lock-${Uuid.random().toHexString()}"
            val first = files.acquire(key)
            val waiting = async(start = CoroutineStart.UNDISPATCHED) { OpfsFileEngine().withLock(key) {} }
            waitForPendingDownloadLock(key)
            assertFalse(waiting.isCompleted)
            waiting.cancelAndJoin()
            first.close()
            OpfsFileEngine().withLock(key) { currentCoroutineContext().ensureActive() }
        }

    @Test
    fun independentStoresSerializeTheSameFile() =
        runTest {
            val files = OpfsFileEngine()
            val key = "test-lock-${Uuid.random().toHexString()}"
            val first = files.acquire(key)
            val acquired = CompletableDeferred<Unit>()
            val next =
                async(start = CoroutineStart.UNDISPATCHED) {
                    OpfsFileEngine().withLock(key) { acquired.complete(Unit) }
                }
            waitForPendingDownloadLock(key)
            assertFalse(acquired.isCompleted)
            first.close()
            next.await()
            assertEquals(true, acquired.isCompleted)
        }

    @Test
    fun fileLocksAlsoExcludeOtherWorkerContexts() =
        runTest {
            val files = OpfsFileEngine()
            val key = "test-worker-${Uuid.random().toHexString()}"
            val lease = files.acquire(key)
            var released = false
            try {
                verifyDownloadWorkerLock(key) {
                    lease.close()
                    released = true
                }
            } finally {
                if (!released) lease.close()
            }
        }

    @Test
    fun cancelledExtractionLeavesNoPartialDirectory() =
        runTest {
            val files = OpfsFileEngine()
            val root = files.location("test-cancel-${Uuid.random().toHexString()}")
            val entered = CompletableDeferred<Unit>()
            val pending =
                async {
                    files.extractDownloadArchive("$root/source") { directory ->
                        files.write("$directory/Regular.ttf", CommunityFontTestData.primary)
                        entered.complete(Unit)
                        kotlinx.coroutines.awaitCancellation()
                    }
                }
            try {
                entered.await()
                pending.cancelAndJoin()
                assertEquals(emptyList(), files.listFiles(root))
            } finally {
                files.delete(root)
            }
        }

    @Test
    fun workerReadsNestedDownloadPaths() =
        runTest {
            val files = OpfsFileEngine()
            val root = files.location("test-worker-nested-${Uuid.random().toHexString()}")
            val source = "$root/1/revision/source"
            try {
                files.write(source, CommunityFontTestData.primaryZip)
                files.extractArchive(source)
                val extracted = assertNotNull(files.extractedFiles(source))
                assertContentEquals(
                    CommunityFontTestData.primary,
                    files.read(extracted.single().path, extracted.single().size),
                )
            } finally {
                files.delete(root)
            }
        }

    @Test
    fun cancelledWorkerReleasesFilesBeforeGenerationCleanup() =
        runTest {
            val files = OpfsFileEngine()
            val root = files.location("test-worker-cancel-${Uuid.random().toHexString()}")
            val source = "$root/source"
            val entered = CompletableDeferred<Unit>()
            try {
                files.write(source, CommunityFontTestData.primaryZip)
                val pending =
                    async {
                        files.extractDownloadArchive(source) { directory ->
                            entered.complete(Unit)
                            extractArchiveInWorker(source, directory)
                        }
                    }
                entered.await()
                pending.cancelAndJoin()
                assertNull(files.extractedFiles(source))
                assertEquals(listOf(source), files.listFiles(root).map { it.path })
                files.extractArchive(source)
                val extracted = assertNotNull(files.extractedFiles(source))
                assertContentEquals(
                    CommunityFontTestData.primary,
                    files.read(extracted.single().path, extracted.single().size),
                )
            } finally {
                files.delete(root)
            }
        }

    @Test
    fun cancellingDuringRealWorkerWriteClosesWriterBeforeCleanup() =
        runTest {
            val files = OpfsFileEngine()
            val root = files.location("test-worker-write-${Uuid.random().toHexString()}")
            val source = "$root/source"
            val started = CompletableDeferred<WritingDownloadWorker>()
            val cleanup = WorkerCleanupProbe(files, started)
            try {
                files.write(source, CommunityFontTestData.primaryZip)
                val pending =
                    async {
                        cleanup.extractDownloadArchive(source) { directory ->
                            val job = startPausedExtraction(source.toJsString(), directory.toJsString())
                            started.complete(job)
                            job.awaitExtraction()
                        }
                    }
                try {
                    val job = started.await()
                    val written = awaitWorkerProbe(job.writeObserved())
                    assertTrue(written.bytes.toDouble() > 0)
                    assertFalse(pending.isCompleted)
                    assertTrue(files.listFiles(root).any { it.path.endsWith("/${written.name}") })
                    pending.cancelAndJoin()
                    assertEquals("abort", awaitWorkerProbe(job.closed()).toString())
                    assertTrue(cleanup.cleaned)
                    assertNull(files.extractedFiles(source))
                    assertEquals(listOf(source), files.listFiles(root).map { it.path })
                    assertContentEquals(CommunityFontTestData.primaryZip, files.read(source, 64 * 1024))
                } finally {
                    pending.cancelAndJoin()
                }
            } finally {
                files.delete(root)
            }
        }

    @Test
    fun workerCancellationBeforeNativeWriteSettlesClosesBeforeCleanup() =
        runTest {
            val files = OpfsFileEngine()
            val root = files.location("test-native-write-${Uuid.random().toHexString()}")
            val source = "$root/source"
            val started = CompletableDeferred<InflightDownloadWorker>()
            val cleanup = WorkerCleanupProbe(files, started)
            try {
                files.write(source, CommunityFontTestData.primaryZip)
                val pending =
                    async {
                        runCatching {
                            cleanup.extractDownloadArchive(source) { directory ->
                                val job = startInflightExtraction(source.toJsString(), directory.toJsString())
                                started.complete(job)
                                job.awaitExtraction()
                            }
                        }.onFailure { if (it is CancellationException) throw it }
                    }
                try {
                    val job = started.await()
                    assertTrue(awaitWorkerProbe(job.writeObserved()).bytes.toDouble() > 0)
                    assertTrue(job.pendingAtCancellation())
                    assertTrue(pending.await().isFailure)
                    assertTrue(job.nativeSettledAfterCancellation())
                    assertEquals("abort", awaitWorkerProbe(job.closed()).toString())
                    assertTrue(cleanup.cleaned)
                    assertNull(files.extractedFiles(source))
                    assertEquals(listOf(source), files.listFiles(root).map { it.path })
                    assertContentEquals(CommunityFontTestData.primaryZip, files.read(source, 64 * 1024))
                } finally {
                    pending.cancelAndJoin()
                }
            } finally {
                files.delete(root)
            }
        }

    @Test
    fun malformedZipDoesNotPublishAnExtraction() =
        runTest {
            val bytes = CommunityFontTestData.primaryZip.copyOf().also { it[48] = 0xff.toByte() }
            HttpClient(MockEngine { respond(bytes) }).use { client ->
                val fixture = DownloadTestFixture(this, OpfsFileEngine(), client)
                try {
                    val file = fontFile(bytes, "family.zip")
                    assertFailsWith<IllegalStateException> {
                        CommunityFontDownload(
                            fixture.records,
                            fixture.downloader,
                            fixture.files,
                            fixture.namespace,
                        ).load(file)
                    }
                    val record = assertNotNull(fixture.records.getDocumentByFileId(file.id))
                    assertEquals(com.storyteller_f.storage.DownloadStatus.PROCESS_FAILED, record.status)
                    assertNull(fixture.files.extractedFiles(record.path))
                    currentCoroutineContext().ensureActive()
                } finally {
                    fixture.close()
                }
            }
        }

    @Test
    fun persistentFontCacheEvictsAtTheRealByteLimit() =
        runTest {
            val files = OpfsFileEngine()
            val server = "public-resources-v1/test-limit-${Uuid.random().toHexString()}"
            val old = files.location("$server/1/revision/source")
            val fresh = files.location("$server/2/revision/source")
            try {
                files.write(old, ByteArray(700))
                files.retainFont(old, maxBytes = 1024)
                files.write(fresh, ByteArray(700))
                files.retainFont(fresh, maxBytes = 1024)
                assertNull(files.size(old))
                assertEquals(700L, files.size(fresh))
            } finally {
                files.delete(files.location(server))
            }
        }

    @Test
    fun persistentFontCacheEvictsAtTheFileLimit() =
        runTest {
            val files = OpfsFileEngine()
            val server = "public-resources-v1/test-count-${Uuid.random().toHexString()}"
            try {
                for (id in 1..3) {
                    val path = files.location("$server/$id/revision/source")
                    files.write(path, byteArrayOf(id.toByte()))
                    files.retainFont(path, maxFiles = 2)
                }
                assertNull(files.size(files.location("$server/1/revision/source")))
                assertEquals(1L, files.size(files.location("$server/3/revision/source")))
            } finally {
                files.delete(files.location(server))
            }
        }

    @Test
    fun opfsWriteRoundTripsBinaryBytesWithoutBase64() =
        runTest {
            val files = OpfsFileEngine()
            val root = files.location("test-binary-${Uuid.random().toHexString()}")
            val bytes = ByteArray(8192) { it.toByte() }
            try {
                files.write("$root/source", bytes)
                assertContentEquals(bytes, files.read("$root/source", bytes.size.toLong()))
            } finally {
                files.delete(root)
            }
        }

    @Test
    fun publicRetentionNeverReadsOrDeletesLegacyEntries() =
        runTest {
            val files = OpfsFileEngine()
            val server = "test-legacy-${Uuid.random().toHexString()}"
            val root = "public-resources-v1/$server"
            val legacy = files.location("$server/1/revision/source")
            val legacyIndex = files.location("$server/font-cache.json")
            val indexBytes = """[{"path":"$legacy","sourceBytes":1,"decodedBytes":0}]""".encodeToByteArray()
            val first = files.location("$root/1/revision/source")
            val second = files.location("$root/2/revision/source")
            try {
                files.write(legacy, byteArrayOf(9))
                files.write(legacyIndex, indexBytes)
                files.write(first, byteArrayOf(1))
                files.retainFont(first, maxFiles = 1)
                files.write(second, byteArrayOf(2))
                files.retainFont(second, maxFiles = 1)
                assertNull(files.size(first))
                assertContentEquals(byteArrayOf(9), files.read(legacy, 1))
                assertContentEquals(indexBytes, files.read(legacyIndex, indexBytes.size.toLong()))
            } finally {
                files.delete(files.location(server))
                files.delete(files.location(root))
            }
        }

    @Test
    fun publicRetentionBudgetsRemainIndependentAcrossServers() =
        runTest {
            val files = OpfsFileEngine()
            val root = "public-resources-v1/test-servers-${Uuid.random().toHexString()}"
            val otherRoot = "$root-other"
            val first = files.location("$root/1/revision/source")
            val second = files.location("$root/2/revision/source")
            val other = files.location("$otherRoot/1/revision/source")
            try {
                files.write(first, byteArrayOf(1))
                files.retainFont(first, maxFiles = 1)
                files.write(other, byteArrayOf(3))
                files.retainFont(other, maxFiles = 1)
                files.write(second, byteArrayOf(2))
                files.retainFont(second, maxFiles = 1)
                assertNull(files.size(first))
                assertEquals(1L, files.size(second))
                assertContentEquals(byteArrayOf(3), files.read(other, 1))
            } finally {
                files.delete(files.location(root))
                files.delete(files.location(otherRoot))
            }
        }

    @Test
    fun publicRetentionWaitsForTheVictimDownloadLease() =
        runTest {
            val files = OpfsFileEngine()
            val server = "test-retain-lock-${Uuid.random().toHexString()}"
            val root = "public-resources-v1/$server"
            val first = files.location("$root/1/revision/source")
            val second = files.location("$root/2/revision/source")
            val key = com.storyteller_f.alphaheart.app.common.downloadTaskKey(server, 1)
            try {
                files.write(first, byteArrayOf(1))
                files.retainFont(first, maxFiles = 1)
                files.write(second, byteArrayOf(2))
                val victim = files.acquire(key)
                try {
                    val retaining = async { files.retainFont(second, maxFiles = 1) }
                    waitForPendingDownloadLock(key)
                    assertFalse(retaining.isCompleted)
                    assertEquals(1L, files.size(first))
                    victim.close()
                    retaining.await()
                    assertNull(files.size(first))
                } finally {
                    victim.close()
                }
            } finally {
                files.delete(files.location(root))
            }
        }

    private fun measureFont(font: com.storyteller_f.alphaheart.app.pages.community.LoadedCommunityFont): Int =
        TextMeasurer(font.resolver ?: createFontFamilyResolver(), Density(1f), LayoutDirection.Ltr).measure(
            AnnotatedString("A"),
            TextStyle(fontFamily = font.family, fontSize = 100.sp),
            softWrap = false,
        ).size.width
}
