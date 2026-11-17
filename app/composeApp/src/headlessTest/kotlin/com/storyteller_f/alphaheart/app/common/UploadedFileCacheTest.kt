/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.common

import androidx.paging.PagingSource
import com.storyteller_f.alphaheart.app.PlatformHeadlessTest
import com.storyteller_f.alphaheart.client.room.RoomFileInfoStorage
import com.storyteller_f.alphaheart.client.room.getRoomDatabase
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.storage.FileCollection
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class UploadedFileCacheTest : PlatformHeadlessTest() {
    @Test
    fun completedUploadInvalidatesMountedListAndPreservesOtherOwners() =
        runTest {
            val database = getRoomDatabase("upload-cache-${Uuid.random().toHexString()}")
            try {
                val storage = RoomFileInfoStorage(database)
                val owner = FileCollection.FileList(1)
                val other = FileCollection.FileList(2)
                val old = FileInfo.EMPTY.copy(id = 10, owner = 1, name = "old")
                val unrelated = FileInfo.EMPTY.copy(id = 20, owner = 2, ownerType = ObjectType.COMMUNITY)
                storage.saveFirst(owner, old)
                storage.saveFirst(other, unrelated)
                val mounted = storage.observeData(owner)
                assertEquals(listOf(old), load(mounted))
                val invalidated = CompletableDeferred<Unit>()
                mounted.registerInvalidatedCallback { invalidated.complete(Unit) }
                val uploaded = old.copy(id = 11, name = "completed")
                cacheUploadedFiles(listOf(uploaded), storage)
                withContext(Dispatchers.Default) { withTimeout(1000) { invalidated.await() } }
                assertTrue(mounted.invalid)
                assertEquals(listOf(uploaded, old), load(storage.observeData(owner)))
                assertEquals(listOf(unrelated), load(storage.observeData(other)))
                assertEquals(uploaded, storage.getDocument(FileCollection.Files, uploaded.id.toString()))
                cacheUploadedFiles(listOf(uploaded), storage)
                assertEquals(listOf(uploaded, old), load(storage.observeData(owner)))
            } finally {
                database.close()
            }
        }

    private suspend fun load(source: PagingSource<Int, FileInfo>): List<FileInfo> {
        val result = source.load(PagingSource.LoadParams.Refresh(null, 20, false))
        return (result as PagingSource.LoadResult.Page).data
    }
}
