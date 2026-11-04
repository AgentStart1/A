/*
 * This is a private project. All rights reserved.
 */

package fonts

import androidx.paging.PagingSource
import com.storyteller_f.a.app.PlatformHeadlessTest
import com.storyteller_f.a.app.common.createDownloadFileStore
import com.storyteller_f.a.app.common.createDownloadRecord
import com.storyteller_f.a.app.common.write
import com.storyteller_f.a.app.pages.community.CommunityFontDownload
import com.storyteller_f.a.client.room.RoomDownloadInfoStorage
import com.storyteller_f.a.client.room.getRoomDatabase
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
    fun revisedDownloadsPreserveLegacyRecordIdentity() =
        runTest {
            val database = getRoomDatabase("font-legacy-${Uuid.random().toHexString()}")
            val records = RoomDownloadInfoStorage(database)
            HttpClient(MockEngine { respond(CommunityFontTestData.primary) }).use { client ->
                val fixture = DownloadTestFixture(this, createDownloadFileStore(), client, records = records)
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
