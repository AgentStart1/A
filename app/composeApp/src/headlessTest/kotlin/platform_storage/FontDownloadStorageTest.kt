/*
 * This is a private project. All rights reserved.
 */

package fonts

import androidx.paging.PagingSource
import com.storyteller_f.alphaheart.app.PlatformHeadlessTest
import com.storyteller_f.alphaheart.app.common.createDownloadRecord
import com.storyteller_f.alphaheart.app.common.createFileEngine
import com.storyteller_f.alphaheart.app.common.write
import com.storyteller_f.alphaheart.app.pages.community.CommunityFontDownload
import com.storyteller_f.alphaheart.client.room.RoomDownloadInfoStorage
import com.storyteller_f.alphaheart.client.room.getRoomDatabase
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.storage.DownloadStatus
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlinx.io.files.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.uuid.Uuid

class FontDownloadStorageTest : PlatformHeadlessTest() {
    @Test
    fun publicCollectionsSeparateServersAndIgnoreLegacyRows() =
        runTest {
            val database = getRoomDatabase("font-collections-${Uuid.random().toHexString()}")
            try {
                val legacy = RoomDownloadInfoStorage(database)
                val left = legacy.forServer("server-one")
                val right = legacy.forServer("server-two")
                val file = fontFile(CommunityFontTestData.primary)
                val old = createDownloadRecord(file, "legacy/source").copy(status = DownloadStatus.PROCESSED)
                legacy.save(old)
                kotlin.test.assertNull(left.getDocumentByFileId(file.id))
                kotlin.test.assertNull(right.getDocumentByFileId(file.id))
                val first = old.copy(path = "public/server-one/source", processedGeneration = 7)
                val second = old.copy(path = "public/server-two/source", processedGeneration = 3)
                left.save(first)
                right.save(second)
                assertEquals(first, legacy.forServer("server-one").getDocumentByFileId(file.id))
                assertEquals(second, right.getDocumentByFileId(file.id))
                assertEquals(old, legacy.getDocumentByFileId(file.id))
                val page =
                    left.observeData().load(PagingSource.LoadParams.Refresh(null, 10, false))
                        as PagingSource.LoadResult.Page
                assertEquals(listOf(first), page.data)
            } finally {
                database.close()
            }
        }

    @Test
    fun revisedDownloadsPreserveLegacyRecordIdentity() =
        runTest {
            val database = getRoomDatabase("font-legacy-${Uuid.random().toHexString()}")
            val records = RoomDownloadInfoStorage(database)
            HttpClient(MockEngine { respond(CommunityFontTestData.primary) }).use { client ->
                val fixture = DownloadTestFixture(this, createFileEngine(), client, records = records)
                try {
                    val original = fontFile(CommunityFontTestData.primary)
                    val path = fixture.files.location("${fixture.namespace}/legacy-source")
                    fixture.files.write(path, CommunityFontTestData.primary)
                    records.save(
                        createDownloadRecord(
                            original,
                            path,
                        ).copy(id = 1_700_000_000, status = DownloadStatus.PROCESSED),
                    )
                    val revised = original.copy(lastModified = kotlinx.datetime.LocalDateTime(2026, 2, 3, 0, 0))
                    CommunityFontDownload(records, fixture.downloader, fixture.files, fixture.namespace).load(revised)
                    val current = assertNotNull(records.getDocumentByFileId(revised.id))
                    assertEquals(1_700_000_000L, current.id)
                    assertEquals(revised, current.fileInfo)
                    val page =
                        records.observeData().load(PagingSource.LoadParams.Refresh(null, 10, false))
                            as PagingSource.LoadResult.Page
                    assertEquals(listOf(current), page.data)
                } finally {
                    fixture.close()
                    database.close()
                }
            }
        }

    @Test
    fun simultaneousFontRecordsRemainIndependent() =
        runTest {
            val database = getRoomDatabase("font-downloads-${Uuid.random().toHexString()}")
            try {
                val storage = RoomDownloadInfoStorage(database)
                val files = (1L..3L).map { FileInfo.EMPTY.copy(id = it, name = "font-$it.ttf") }
                val records = files.map { createDownloadRecord(it, Path(it.name).toString()) }
                records.map { async { storage.save(it) } }.awaitAll()
                files.forEach { file ->
                    assertEquals(file, assertNotNull(storage.getDocumentByFileId(file.id)).fileInfo)
                }
                val revised = files[1].copy(url = "https://fonts.invalid/renewed")
                storage.save(createDownloadRecord(revised, Path(revised.name).toString()))
                assertEquals(revised, assertNotNull(storage.getDocumentByFileId(revised.id)).fileInfo)
                listOf(files.first(), files.last()).forEach { file ->
                    assertEquals(file, assertNotNull(storage.getDocumentByFileId(file.id)).fileInfo)
                }
            } finally {
                database.close()
            }
        }
}
