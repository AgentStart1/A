/*
 * This is a private project. All rights reserved.
 */

package fonts

import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.storyteller_f.a.app.common.DownloadFileStore
import com.storyteller_f.a.app.common.createDownloadFileStore
import com.storyteller_f.a.app.common.createDownloadRecord
import com.storyteller_f.a.app.common.extractDownloadArchive
import com.storyteller_f.a.app.common.extractedFiles
import com.storyteller_f.a.app.common.getDownloadRevision
import com.storyteller_f.a.app.common.write
import com.storyteller_f.a.app.pages.community.CommunityFontBundle
import com.storyteller_f.a.app.pages.community.CommunityFontDownload
import com.storyteller_f.storage.DownloadStatus
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DownloadEngineTest {
    @Test
    fun databaseCompletionPublishesReadableExtractedFonts() =
        runTest {
            val bytes = CommunityFontTestData.primaryZip
            val release = CompletableDeferred<Unit>()
            var requests = 0
            HttpClient(
                MockEngine {
                    requests++
                    release.await()
                    respond(bytes)
                },
            ).use { client ->
                val files = createDownloadFileStore()
                val records =
                    DownloadTestRecords { saved ->
                        if (saved.status == DownloadStatus.PROCESSED) assertNotNull(files.extractedFiles(saved.path))
                    }
                val fixture = DownloadTestFixture(this, files, client, records = records)
                try {
                    val file = fontFile(bytes, "family.zip")
                    val loaded = async { load(fixture, file) }
                    fixture.records.observeDocumentByFileId(file.id).first { it?.status == DownloadStatus.DOWNLOADING }
                    assertFalse(loaded.isCompleted)
                    release.complete(Unit)
                    assertNotNull(loaded.await().loaded.family)
                    val record = assertNotNull(fixture.records.getDocumentByFileId(file.id))
                    assertEquals(DownloadStatus.PROCESSED, record.status)
                    assertNotNull(fixture.files.extractedFiles(record.path))
                    assertEquals(1, requests)
                } finally {
                    fixture.close()
                }
            }
        }

    @Test
    fun recreatedDownloaderReusesTheExtractedDiskCache() =
        runTest {
            val bytes = CommunityFontTestData.primaryZip
            var requests = 0
            HttpClient(
                MockEngine {
                    requests++
                    respond(bytes)
                },
            ).use { client ->
                val fixture = DownloadTestFixture(this, createDownloadFileStore(), client)
                try {
                    val file = fontFile(bytes, "family.zip")
                    load(fixture, file)
                    val first = assertNotNull(fixture.records.getDocumentByFileId(file.id))
                    val recreated =
                        DownloadTestFixture(
                            this,
                            createDownloadFileStore(),
                            client,
                            fixture.namespace,
                            fixture.records,
                        )
                    load(recreated, file.copy(url = "https://fonts.invalid/renewed"))
                    val second = assertNotNull(fixture.records.getDocumentByFileId(file.id))
                    assertEquals(first.path, second.path)
                    assertEquals(fixture.files.extractedFiles(first.path), fixture.files.extractedFiles(second.path))
                    assertEquals("https://fonts.invalid/renewed", second.fileInfo.url)
                    assertEquals(1, requests)
                } finally {
                    fixture.close()
                }
            }
        }

    @Test
    fun missingExtractionIsRebuiltWithoutDownloadingAgain() =
        runTest {
            val bytes = CommunityFontTestData.primaryZip
            var requests = 0
            HttpClient(
                MockEngine {
                    requests++
                    respond(bytes)
                },
            ).use { client ->
                val fixture = DownloadTestFixture(this, createDownloadFileStore(), client)
                try {
                    val file = fontFile(bytes, "family.zip")
                    load(fixture, file)
                    val path = assertNotNull(fixture.records.getDocumentByFileId(file.id)).path
                    val face = assertNotNull(fixture.files.extractedFiles(path)).single()
                    fixture.files.delete(face.path)
                    assertNotNull(load(fixture, file))
                    assertEquals(1, requests)
                    assertContentEquals(
                        CommunityFontTestData.primary,
                        fixture.files.read(face.path, CommunityFontTestData.primary.size.toLong()),
                    )
                } finally {
                    fixture.close()
                }
            }
        }

    @Test
    fun missingSourceIsDownloadedAgain() =
        runTest {
            val bytes = CommunityFontTestData.primary
            var requests = 0
            HttpClient(
                MockEngine {
                    requests++
                    respond(bytes)
                },
            ).use { client ->
                val fixture = DownloadTestFixture(this, createDownloadFileStore(), client)
                try {
                    val file = fontFile(bytes)
                    load(fixture, file)
                    fixture.files.delete(assertNotNull(fixture.records.getDocumentByFileId(file.id)).path)
                    assertNotNull(load(fixture, file))
                    assertEquals(2, requests)
                } finally {
                    fixture.close()
                }
            }
        }

    @Test
    fun failedDownloadsRequireAnExplicitRetry() =
        runTest {
            val bytes = CommunityFontTestData.primary
            var requests = 0
            HttpClient(
                MockEngine {
                    if (++requests == 1) respond("missing", HttpStatusCode.NotFound) else respond(bytes)
                },
            ).use { client ->
                val fixture = DownloadTestFixture(this, createDownloadFileStore(), client)
                try {
                    val file = fontFile(bytes)
                    assertFailsWith<IllegalStateException> { load(fixture, file) }
                    assertFailsWith<IllegalStateException> { load(fixture, file) }
                    assertEquals(1, requests)
                    assertNotNull(load(fixture, file, retry = true))
                    assertEquals(2, requests)
                } finally {
                    fixture.close()
                }
            }
        }

    @Test
    fun extractionRetryReusesTheDownloadedArchive() =
        runTest {
            val bytes = CommunityFontTestData.primaryZip
            val disk = createDownloadFileStore()
            var extractions = 0
            val files =
                object : DownloadFileStore by disk {
                    override suspend fun extractArchive(path: String) {
                        if (++extractions == 1) error("Synthetic extraction failure")
                        disk.extractArchive(path)
                    }
                }
            var requests = 0
            HttpClient(
                MockEngine {
                    requests++
                    respond(bytes)
                },
            ).use { client ->
                val fixture = DownloadTestFixture(this, files, client)
                try {
                    val file = fontFile(bytes, "family.zip")
                    assertFailsWith<IllegalStateException> { load(fixture, file) }
                    assertEquals(DownloadStatus.PROCESS_FAILED, fixture.records.getDocumentByFileId(file.id)?.status)
                    assertNotNull(load(fixture, file, retry = true))
                    assertEquals(1, requests)
                    assertEquals(2, extractions)
                } finally {
                    fixture.close()
                }
            }
        }

    @Test
    fun fullResponseReplacesAnExistingPartialFile() =
        runTest {
            val bytes = CommunityFontTestData.primary
            HttpClient(
                MockEngine { request ->
                    assertEquals("bytes=2-", request.headers[HttpHeaders.Range])
                    respond(bytes)
                },
            ).use { client ->
                val fixture = DownloadTestFixture(this, createDownloadFileStore(), client)
                try {
                    val file = fontFile(bytes)
                    val partialPath =
                        fixture.files.location(
                            "${fixture.namespace}/${file.id}/${getDownloadRevision(file)}/source",
                        )
                    fixture.files.write(partialPath, bytes.copyOf(2))
                    fixture.records.save(
                        createDownloadRecord(file, partialPath).copy(status = DownloadStatus.PAUSED, progress = 2),
                    )
                    assertNotNull(load(fixture, file, retry = true))
                    val path = assertNotNull(fixture.records.getDocumentByFileId(file.id)).path
                    assertContentEquals(bytes, fixture.files.read(path, bytes.size.toLong()))
                } finally {
                    fixture.close()
                }
            }
        }

    @Test
    fun revisionsUseIndependentSourceLocations() =
        runTest {
            val bytes = CommunityFontTestData.primary
            var requests = 0
            HttpClient(
                MockEngine {
                    requests++
                    respond(bytes)
                },
            ).use { client ->
                val fixture = DownloadTestFixture(this, createDownloadFileStore(), client)
                try {
                    val file = fontFile(bytes)
                    load(fixture, file)
                    val first = assertNotNull(fixture.records.getDocumentByFileId(file.id)).path
                    load(fixture, file.copy(lastModified = LocalDateTime(2026, 1, 2, 0, 0)))
                    assertNotEquals(first, assertNotNull(fixture.records.getDocumentByFileId(file.id)).path)
                    assertEquals(2, requests)
                } finally {
                    fixture.close()
                }
            }
        }

    @Test
    fun cancelledExtractionDoesNotPublishPartialFiles() =
        runTest {
            val files = createDownloadFileStore()
            val root = files.location("test-cancel-${kotlin.uuid.Uuid.random().toHexString()}")
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
    fun extractionCleanupPreservesTheOriginalFailure() =
        runTest {
            val disk = createDownloadFileStore()
            val root = disk.location("test-cleanup-${kotlin.uuid.Uuid.random().toHexString()}")
            val files =
                object : DownloadFileStore by disk {
                    private var deletes = 0
                    override suspend fun delete(path: String) {
                        if (++deletes > 1) error("cleanup")
                        disk.delete(path)
                    }
                }
            try {
                val failure =
                    assertFailsWith<IllegalStateException> {
                        files.extractDownloadArchive("$root/source") { error("primary") }
                    }
                val chain = generateSequence<Throwable>(failure) { it.cause }.toList()
                assertEquals("primary", chain.last().message)
                kotlin.test.assertTrue(chain.flatMap { it.suppressedExceptions }.any { it.message == "cleanup" })
            } finally {
                disk.delete(root)
            }
        }

    @Test
    fun downloadedArchivePreservesAllFourWeightAndStylePairs() =
        runTest {
            val bytes =
                fontTestZip(
                    "family/Regular.ttf",
                    "family/Bold.ttf",
                    "family/Italic.ttf",
                    "family/BoldItalic.ttf",
                )
            val bundle = downloadFont(bytes, "family.zip")
            assertEquals(
                mapOf(
                    "Regular.ttf" to (FontWeight.Normal to FontStyle.Normal),
                    "Bold.ttf" to (FontWeight.Bold to FontStyle.Normal),
                    "Italic.ttf" to (FontWeight.Normal to FontStyle.Italic),
                    "BoldItalic.ttf" to (FontWeight.Bold to FontStyle.Italic),
                ),
                bundle.faces.associate { face ->
                    val name = face.name.replace('\\', '/').substringAfterLast('/')
                    name to (face.weight to face.style)
                },
            )
        }

    @Test
    fun downloadedUnsupportedFontExtensionIsRejected() =
        runTest {
            assertFailsWith<IllegalArgumentException> { downloadFont(CommunityFontTestData.primary, "font.txt") }
        }

    @Test
    fun emptyFontSourceIsRejected() =
        runTest {
            assertFailsWith<IllegalArgumentException> { downloadFont(byteArrayOf(), "Regular.ttf") }
        }

    @Test
    fun downloadedArchiveWithoutFontEntriesIsRejected() =
        runTest {
            assertFailsWith<IllegalArgumentException> { downloadFont(fontTestZip("README.txt"), "family.zip") }
        }

    @Test
    fun nativeFontLoadingNeverReadsTheSourceIntoBytes() =
        runTest {
            val disk = createDownloadFileStore()
            val files =
                object : DownloadFileStore by disk {
                    override suspend fun read(path: String, limit: Long): ByteArray =
                        error("Unexpected byte-array read")
                }
            HttpClient(MockEngine { respond(CommunityFontTestData.primary) }).use { client ->
                val fixture = DownloadTestFixture(this, files, client)
                try {
                    assertNotNull(load(fixture, fontFile(CommunityFontTestData.primary)).loaded.family)
                } finally {
                    fixture.close()
                }
            }
        }

    @Test
    fun retryLeavesCompletedStateBeforeReplacingPartialExtraction() =
        runTest {
            val bytes = fontTestZip("Regular.ttf", "Bold.ttf")
            val disk = createDownloadFileStore()
            val records = DownloadTestRecords()
            var extractions = 0
            val files =
                object : DownloadFileStore by disk {
                    override suspend fun extractArchive(path: String) {
                        assertEquals(DownloadStatus.DOWNLOADED, records.getDocumentByFileId(1)?.status)
                        extractions++
                        disk.extractArchive(path)
                    }
                }
            HttpClient(MockEngine { respond(bytes) }).use { client ->
                val fixture = DownloadTestFixture(this, files, client, records = records)
                try {
                    val file = fontFile(bytes, "family.zip")
                    load(fixture, file)
                    val path = assertNotNull(records.getDocumentByFileId(file.id)).path
                    files.delete("$path.extracted/Bold.ttf")
                    files.write("$path.extracted/stale.ttf", byteArrayOf(1, 2, 3))
                    val repaired = load(fixture, file, retry = true)
                    assertEquals(2, repaired.faces.size)
                    assertEquals(2, extractions)
                    assertNull(files.size("$path.extracted/stale.ttf"))
                    assertEquals(DownloadStatus.PROCESSED, records.getDocumentByFileId(file.id)?.status)
                } finally {
                    fixture.close()
                }
            }
        }

    @Test
    fun unfinishedDirectoryIsDiscardedBeforeRestartedExtraction() =
        runTest {
            val bytes = CommunityFontTestData.primaryZip
            HttpClient(MockEngine { error("The complete source must be reused") }).use { client ->
                val fixture = DownloadTestFixture(this, createDownloadFileStore(), client)
                try {
                    val file = fontFile(bytes, "family.zip")
                    val path = fixture.files.location("${fixture.namespace}/1/${getDownloadRevision(file)}/source")
                    fixture.files.write(path, bytes)
                    fixture.files.write("$path.extracted/partial.ttf", byteArrayOf(1, 2, 3))
                    fixture.records.save(createDownloadRecord(file, path).copy(status = DownloadStatus.DOWNLOADED))
                    assertNotNull(load(fixture, file).loaded.family)
                    assertNull(fixture.files.size("$path.extracted/partial.ttf"))
                    assertEquals(1, assertNotNull(fixture.files.extractedFiles(path)).size)
                } finally {
                    fixture.close()
                }
            }
        }

    @Test
    fun readerRechecksCompletionAfterEnteringTheFileLock() =
        runTest {
            val records = DownloadTestRecords()
            val file = fontFile(CommunityFontTestData.primary)
            val record = createDownloadRecord(file, "synthetic/source").copy(status = DownloadStatus.PROCESSED)
            records.save(record)
            val disk = createDownloadFileStore()
            val files =
                object : DownloadFileStore by disk {
                    override suspend fun acquire(key: String): com.storyteller_f.a.app.common.DownloadFileLease {
                        records.save(record.copy(status = DownloadStatus.DOWNLOADED))
                        return com.storyteller_f.a.app.common.DownloadFileLease { }
                    }
                }
            val downloader =
                object : com.storyteller_f.a.app.common.Downloader {
                    override suspend fun download(fileInfo: com.storyteller_f.shared.model.FileInfo, retry: Boolean) =
                        record
                    override fun resume(id: Long) = error("Unused")
                    override fun pause(id: Long) = error("Unused")
                }
            val source = CommunityFontDownload(records, downloader, files, "synthetic")
            val failure = assertFailsWith<IllegalStateException> { source.load(file) }
            assertEquals("Font download changed before reading", failure.message)
        }

    private suspend fun CoroutineScope.downloadFont(bytes: ByteArray, name: String): CommunityFontBundle =
        HttpClient(MockEngine { respond(bytes) }).use { client ->
            val fixture = DownloadTestFixture(this, createDownloadFileStore(), client)
            try {
                load(fixture, fontFile(bytes, name))
            } finally {
                fixture.close()
            }
        }

    private suspend fun load(
        fixture: DownloadTestFixture,
        file: com.storyteller_f.shared.model.FileInfo,
        retry: Boolean = false,
    ) = CommunityFontDownload(fixture.records, fixture.downloader, fixture.files, fixture.namespace).load(file, retry)
}
