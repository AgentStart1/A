/*
 * This is a private project. All rights reserved.
 */

package fonts

import com.storyteller_f.alphaheart.app.common.createExclusiveArchiveSink
import com.storyteller_f.alphaheart.app.common.createFileEngine
import com.storyteller_f.alphaheart.app.common.extractedFiles
import com.storyteller_f.alphaheart.app.common.write
import com.storyteller_f.storage.DownloadStatus
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.io.Buffer
import kotlinx.io.files.Path
import kotlinx.io.readByteArray
import no.synth.kmpzip.kotlinx.ZipOutputStream
import no.synth.kmpzip.zip.ZipEntry
import java.nio.file.FileAlreadyExistsException
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
    fun rejectsTrailingDotFileAliases() =
        runTest {
            assertRejected(fontTestZip("Regular.ttf", "Regular.ttf."), "trailing dot or space")
        }

    @Test
    fun rejectsTrailingSpaceFileAliases() =
        runTest {
            assertRejected(fontTestZip("Regular.ttf", "Regular.ttf "), "trailing dot or space")
        }

    @Test
    fun rejectsTrailingDotParentDirectoryAliases() =
        runTest {
            assertRejected(fontTestZip("family/Regular.ttf", "family./Regular.ttf"), "trailing dot or space")
        }

    @Test
    fun rejectsTrailingSpaceParentDirectoryAliases() =
        runTest {
            assertRejected(fontTestZip("family/Regular.ttf", "family /Regular.ttf"), "trailing dot or space")
        }

    @Test
    fun rejectsTooManyArchiveEntriesAndCleansPartialFiles() =
        runTest {
            val bytes =
                createArchive {
                    repeat(129) {
                        putNextEntry(ZipEntry("$it.txt"))
                        closeEntry()
                    }
                }
            assertRejected(bytes, "Too many archive entries")
        }

    @Test
    fun acceptsExactly128MixedArchiveEntries() =
        runTest {
            val files = createFileEngine()
            val root = files.location("archive-test-${Uuid.random().toHexString()}")
            try {
                val source = "$root/source"
                files.write(source, mixedArchive(127))
                files.extractArchive(source)
                assertEquals(1, assertNotNull(files.extractedFiles(source)).size)
            } finally {
                files.delete(root)
            }
        }

    @Test
    fun rejects129MixedArchiveEntries() =
        runTest {
            assertRejected(mixedArchive(128), "Too many archive entries")
        }

    @Test
    fun rejects129DirectoryEntries() =
        runTest {
            val bytes =
                createArchive {
                    repeat(129) {
                        putNextEntry(ZipEntry("dir$it/"))
                        closeEntry()
                    }
                }
            assertRejected(bytes, "Too many archive entries")
        }

    private fun mixedArchive(directories: Int): ByteArray =
        createArchive {
            repeat(directories) {
                putNextEntry(ZipEntry("dir$it/"))
                closeEntry()
            }
            putNextEntry(ZipEntry("Regular.ttf"))
            write(CommunityFontTestData.primary)
            closeEntry()
        }

    @Test
    fun exclusiveCreationPreservesAnExistingFile() =
        runTest {
            val files = createFileEngine()
            val root = files.location("exclusive-test-${Uuid.random().toHexString()}")
            val target = "$root/Regular.ttf"
            try {
                files.write(target, CommunityFontTestData.primary)
                withContext(Dispatchers.IO) {
                    assertFailsWith<FileAlreadyExistsException> {
                        createExclusiveArchiveSink(Path(target)).use { it.write(CommunityFontTestData.code) }
                    }
                }
                assertContentEquals(
                    CommunityFontTestData.primary,
                    files.read(target, CommunityFontTestData.primary.size.toLong()),
                )
            } finally {
                files.delete(root)
            }
        }

    @Test
    fun shortFilenameAliasesNeverOverwriteEarlierEntries() =
        runTest {
            val files = createFileEngine()
            val root = files.location("short-name-test-${Uuid.random().toHexString()}")
            val source = "$root/source"
            val bytes =
                fontTestArchive(
                    "RegularLongFont.ttf" to CommunityFontTestData.primary,
                    "REGULA~1.TTF" to CommunityFontTestData.code,
                )
            try {
                files.write(source, bytes)
                val collision =
                    try {
                        files.extractArchive(source)
                        null
                    } catch (failure: FileAlreadyExistsException) {
                        failure
                    }
                if (collision != null) {
                    assertTrue(collision.message.orEmpty().contains("REGULA~1.TTF"), collision.toString())
                    assertNull(files.extractedFiles(source))
                } else {
                    // Filesystems without DOS aliases keep two independent names; neither may be overwritten.
                    assertEquals(2, assertNotNull(files.extractedFiles(source)).size)
                    assertContentEquals(
                        CommunityFontTestData.primary,
                        files.read(
                            "$source.extracted/RegularLongFont.ttf",
                            CommunityFontTestData.primary.size.toLong(),
                        ),
                    )
                    assertContentEquals(
                        CommunityFontTestData.code,
                        files.read("$source.extracted/REGULA~1.TTF", CommunityFontTestData.code.size.toLong()),
                    )
                }
            } finally {
                files.delete(root)
            }
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
