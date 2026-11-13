/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.room

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.storyteller_f.shared.model.RoomInfo
import com.storyteller_f.shared.model.TitleSearchType
import com.storyteller_f.shared.type.JoinStatusSearch
import com.storyteller_f.storage.RoomCollection
import com.storyteller_f.storage.TitleCollection
import com.storyteller_f.storage.getName
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.uuid.Uuid

class StorageCollectionTest {
    @Test
    fun roomRefreshPreservesDetailsAndOtherLists() =
        runTest {
            val database = getRoomDatabase("collection-${Uuid.random().toHexString()}")
            try {
                val collection = RoomCollection.SearchRoom("", null, JoinStatusSearch.JOINED)
                val other = RoomCollection.CommunityRooms(2)
                assertCollectionCleanup(
                    database,
                    RoomCollection.Rooms.getName(),
                    collection.getName(),
                    other.getName(),
                ) {
                    RoomRoomInfoStorage(database).clean(collection)
                }
            } finally {
                database.close()
            }
        }

    @Test
    fun titleRefreshPreservesDetailsAndOtherLists() =
        runTest {
            val database = getRoomDatabase("collection-${Uuid.random().toHexString()}")
            try {
                val collection = TitleCollection.SearchTitle(1, TitleSearchType.CREATOR)
                assertCollectionCleanup(
                    database,
                    TitleCollection.Titles.getName(),
                    collection.getName(),
                    TitleCollection.AllTitles.getName(),
                ) {
                    RoomTitleInfoStorage(database).clean(collection)
                }
            } finally {
                database.close()
            }
        }

    @Test
    fun refreshReplacementPreservesOrderAndLocalAnchor() =
        runTest {
            val database = getRoomDatabase("paging-${Uuid.random().toHexString()}")
            try {
                val storage = RoomRoomInfoStorage(database)
                val collection = RoomCollection.CommunityRooms(1)
                val rows =
                    (0 until 60).map {
                        RoomInfo.EMPTY.copy(id = (it * 37 % 101).toLong(), aid = "room-$it", name = "Room $it")
                    }
                storage.saveLast(RoomCollection.CommunityRooms(2), rows.first())
                storage.replace(collection, rows)
                val page =
                    assertIs<PagingSource.LoadResult.Page<Int, RoomInfo>>(
                        storage.observeData(collection).load(PagingSource.LoadParams.Refresh(null, 60, true)),
                    )
                assertEquals(rows.map { it.id }, page.data.map { it.id })
                assertEquals(
                    15,
                    storage.observeData(collection).getRefreshKey(
                        PagingState(listOf(page), 45, PagingConfig(20), 0),
                    ),
                )
                val updated = rows.map { it.copy(name = "Updated ${it.name}") }
                storage.replace(collection, updated)
                assertEquals(updated.first(), storage.getDocument(collection, updated.first().id))
                assertEquals(updated.first(), storage.getDocument(RoomCollection.Rooms, updated.first().aid))
                assertEquals(rows.first(), storage.getDocument(RoomCollection.CommunityRooms(2), rows.first().id))
            } finally {
                database.close()
            }
        }

    private suspend fun assertCollectionCleanup(
        database: AppDatabase,
        defaultCollection: String,
        selectedCollection: String,
        otherCollection: String,
        clean: suspend () -> Unit,
    ) {
        val dao = database.getCommonDao()
        listOf(defaultCollection, selectedCollection, otherCollection).forEach { collection ->
            dao.insert(CommonEntity(1, collection, "preserved"))
        }
        dao.insert(CommonEntity("alias", defaultCollection, "preserved-alias"))
        clean()
        assertNull(dao.get(selectedCollection, "1"))
        assertEquals("preserved", dao.get(defaultCollection, "1")?.data)
        assertEquals("preserved-alias", dao.get(defaultCollection, "alias")?.data)
        assertEquals("preserved", dao.get(otherCollection, "1")?.data)
    }
}
