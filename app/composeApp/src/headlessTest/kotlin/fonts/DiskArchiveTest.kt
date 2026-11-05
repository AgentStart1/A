/*
 * This is a private project. All rights reserved.
 */

package fonts

import com.storyteller_f.a.app.common.createFileEngine
import com.storyteller_f.a.app.common.extractedFiles
import com.storyteller_f.a.app.common.write
import com.storyteller_f.storage.DownloadStatus
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import no.synth.kmpzip.kotlinx.ZipOutputStream
import no.synth.kmpzip.zip.ZipEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid
import no.synth.kmpzip.zip.ZipOutputStream as ArchiveWriter

class DiskArchiveTest {
    @Test
    fun rejectsExpansionBeyondTheBudgetAndCleansPartialFiles() =
        runTest {
            assertRejected(repeatedArchive("Regular.ttf" to 81), "decoded size limit")
        }

    @Test
    fun countsNonFontEntriesInTheCumulativeExpansionBudget() =
        runTest {
            assertRejected(repeatedArchive("Regular.ttf" to 40, "README.txt" to 41), "decoded size limit")
        }

    @Test
    fun acceptsTheExactExpansionBudget() =
        runTest {
            val files = createFileEngine()
            val root = files.location("archive-test-${Uuid.random().toHexString()}")
            try {
                val source = "$root/source"
                files.write(source, repeatedArchive("README.txt" to 80))
                files.extractArchive(source)
                assertEquals(80L * 1024 * 1024, assertNotNull(files.extractedFiles(source)).single().size)
            } finally {
                files.delete(root)
            }
        }

    @Test
    fun rejectsDuplicateNamesBeforeTheyCanOverwriteAFont() =
        runTest {
            assertRejected(fontTestZip("Regular.ttf", "Regular.ttf"), "Duplicate archive paths")
        }

    @Test
    fun rejectsCaseInsensitiveTargetCollisions() =
        runTest {
            assertRejected(fontTestZip("Regular.ttf", "regular.ttf"), "Duplicate archive paths")
        }

    @Test
    fun rejectsSeparatorAliasesForTheSameTarget() =
        runTest {
            assertRejected(fontTestZip("family/Regular.ttf", "family\\Regular.ttf"), "Duplicate archive paths")
        }

    @Test
    fun rejectsNormalizedDotAliasesForTheSameTarget() =
        runTest {
            assertRejected(fontTestZip("family/Regular.ttf", "family/./Regular.ttf"), "Duplicate archive paths")
        }

    @Test
    fun rejectsTooManyArchiveEntriesAndCleansPartialFiles() =
        runTest {
            val bytes =
                createArchive {
                    repeat(2049) {
                        putNextEntry(ZipEntry("$it.txt"))
                        closeEntry()
                    }
                }
            assertRejected(bytes, "Too many archive entries")
        }

    private suspend fun assertRejected(bytes: ByteArray, message: String) =
        coroutineScope {
            HttpClient(MockEngine { respond(bytes) }).use { client ->
                val files = createFileEngine()
                val records = DownloadTestRecords()
                val fixture = DownloadTestFixture(this, files, client, records = records)
                try {
                    val file = fontFile(bytes, "family.zip")
                    fixture.downloader.download(file)
                    val terminalStatuses =
                        setOf(DownloadStatus.PROCESSED, DownloadStatus.PROCESS_FAILED, DownloadStatus.DOWNLOAD_FAILED)
                    val completed = records.observeDocumentByFileId(file.id).first { it?.status in terminalStatuses }
                    val record = assertNotNull(completed)
                    assertEquals(DownloadStatus.PROCESS_FAILED, record.status)
                    assertTrue(record.message.contains(message), record.message)
                    assertEquals(0L, record.processedGeneration)
                    assertFalse(records.history.any { it.status == DownloadStatus.PROCESSED })
                    assertNull(files.extractedFiles(record.path))
                    assertEquals(file.size, files.size(record.path))
                } finally {
                    fixture.close()
                }
            }
        }

    private fun repeatedArchive(vararg entries: Pair<String, Int>): ByteArray =
        createArchive {
            val chunk = ByteArray(1024 * 1024)
            entries.forEach { (name, mebibytes) ->
                putNextEntry(ZipEntry(name))
                repeat(mebibytes) { write(chunk) }
                closeEntry()
            }
        }

    private fun createArchive(build: ArchiveWriter.() -> Unit): ByteArray {
        val output = Buffer()
        ZipOutputStream(output).use { archive -> build(archive) }
        return output.readByteArray()
    }
}
