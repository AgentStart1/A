/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.room

import androidx.paging.PagingSource
import com.storyteller_f.shared.model.ReactionInfo
import com.storyteller_f.storage.ReactionCollection
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.uuid.Uuid

class ReactionCollectionTest {
    @Test
    fun reactionPagingWritesTheRequestedTopicCollectionAndKeepsDefaultDetailsAndOtherTopics() =
        runTest {
            val database = getRoomDatabase("reaction-collection-${Uuid.random().toHexString()}")
            try {
                val storage = RoomReactionInfoStorage(database)
                val first = ReactionCollection.ReactionList(101)
                val second = ReactionCollection.ReactionList(202)
                val smile = ReactionInfo("😀", 101, 1, true, 500)
                val heart = ReactionInfo("❤️", 101, 2, false, 501)
                val fire = ReactionInfo("🔥", 101, 3, false, 502)
                val otherSmile = ReactionInfo("😀", 202, 4, false, 503)
                storage.saveLast(first, smile)
                storage.saveLast(first, heart)
                storage.saveFirst(first, fire)
                storage.saveLast(second, otherSmile)
                assertEquals(listOf(fire, smile, heart), load(storage.observeData(first)))
                assertEquals(listOf(otherSmile), load(storage.observeData(second)))
                assertEquals(smile, storage.getDocument(ReactionCollection.Reactions, "101-😀"))
                assertEquals(otherSmile, storage.getDocument(ReactionCollection.Reactions, "202-😀"))
                storage.clean(first)
                assertEquals(emptyList(), load(storage.observeData(first)))
                assertEquals(listOf(otherSmile), load(storage.observeData(second)))
                assertEquals(smile, storage.getDocument(ReactionCollection.Reactions, "101-😀"))
                assertEquals(otherSmile, storage.getDocument(ReactionCollection.Reactions, "202-😀"))
            } finally {
                database.close()
            }
        }

    private suspend fun load(source: PagingSource<Int, ReactionInfo>): List<ReactionInfo> =
        assertIs<PagingSource.LoadResult.Page<Int, ReactionInfo>>(
            source.load(PagingSource.LoadParams.Refresh(null, 20, false)),
        ).data
}
