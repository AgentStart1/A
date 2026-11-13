/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.common

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.storyteller_f.alphaheart.api.RoomInfoListResponse
import com.storyteller_f.alphaheart.client.compose_core.error.UiOperationError
import com.storyteller_f.result.CustomResult
import com.storyteller_f.shared.model.RoomInfo
import com.storyteller_f.shared.obj.Pagination
import com.storyteller_f.storage.RemoteKeyStorage
import com.storyteller_f.storage.RemoteKeys
import com.storyteller_f.storage.WrappedPagingSource
import com.storyteller_f.storage.wrap
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalPagingApi::class)
internal class PagingRefreshTest {
    @Test
    fun wrappersDelegateRefreshKeysAndPlaceholderCounts() =
        runTest {
            val raw =
                object : PagingSource<Int, String>() {
                    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, String> =
                        LoadResult.Page(listOf("source-a", "source-b"), 17, 29, 20, 40)
                    override fun getRefreshKey(state: PagingState<Int, String>): Int {
                        assertEquals(17, state.pages.single().prevKey)
                        assertEquals(29, state.pages.single().nextKey)
                        assertEquals("source-b", state.closestItemToPosition(21))
                        assertEquals(20, state.pages.single().itemsBefore)
                        return 77
                    }
                }
            val source = CompatPagingSource(raw, IntKeyConverter)
            val page =
                assertIs<PagingSource.LoadResult.Page<String, String>>(
                    source.load(PagingSource.LoadParams.Refresh("20", 20, true)),
                )
            assertEquals(20, page.itemsBefore)
            assertEquals(40, page.itemsAfter)
            assertEquals("77", source.getRefreshKey(PagingState(listOf(page), 21, PagingConfig(20), 20)))
        }

    @Test
    fun mappedRefreshWorksWithANewSourceGeneration() =
        runTest {
            fun source() =
                WrappedPagingSource(
                    object : PagingSource<Int, String>() {
                        override fun getRefreshKey(state: PagingState<Int, String>): Int? = null
                        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, String> =
                            LoadResult.Page(listOf("source-a", "source-b"), 17, 29, 20, 40)
                    },
                    { state: PagingState<Int, Int> ->
                        assertEquals(listOf(8, 8), state.pages.single().data)
                        state.anchorPosition?.let { (it - state.config.initialLoadSize / 2).coerceAtLeast(0) }
                    },
                ) { it.map(String::length) }
            val page =
                assertIs<PagingSource.LoadResult.Page<Int, Int>>(
                    source().load(PagingSource.LoadParams.Refresh(20, 20, true)),
                )
            assertEquals(20, page.itemsBefore)
            assertEquals(40, page.itemsAfter)
            assertEquals(5, source().getRefreshKey(PagingState(listOf(page), 35, PagingConfig(20), 20)))
        }

    @Test
    fun refreshRetainsLoadedRangeAndOpaqueTokenOrder() =
        runTest {
            val keys = MemoryRemoteKeys()
            val requests = mutableListOf<String?>()
            val commits = mutableListOf<List<RoomInfo>>()
            val rooms = (0 until 80).map { RoomInfo.EMPTY.copy(id = (it * 37 % 101).toLong(), name = "Updated $it") }
            val tokens = listOf(null, "cursor/alpha?x=opaque", "cursor/beta:opaque", "cursor/gamma+opaque", null)
            val source =
                RegularPagingSource<RoomInfo> { key, size ->
                    requests.add(key)
                    val page = tokens.dropLast(1).indexOf(key)
                    assertEquals(20, size)
                    CustomResult.success(
                        RoomInfoListResponse(
                            rooms.drop(page * 20).take(size).toImmutableList(),
                            Pagination(tokens[page + 1], null, 80),
                        ),
                    )
                }
            val mediator =
                CustomRemoteMediator(keys.wrap("rooms"), source) { data, type ->
                    assertEquals(LoadType.REFRESH, type)
                    commits.add(data)
                }
            val result = mediator.load(LoadType.REFRESH, roomState(rooms.take(60), 45))
            assertIs<RemoteMediator.MediatorResult.Success>(result)
            assertEquals(tokens.take(3), requests)
            assertEquals(listOf(rooms.take(60)), commits)
            assertEquals(tokens[3], keys.getNextRemoteKey("rooms")?.key)
        }

    @Test
    fun failedLaterPagePreservesCacheAndRemoteKeys() =
        runTest {
            val keys = MemoryRemoteKeys()
            keys.saveNextRemoteKey(RemoteKeys("rooms", "previous-next"))
            var commits = 0
            val source =
                RegularPagingSource<RoomInfo> { key, _ ->
                    if (key == null) {
                        CustomResult.success(
                            RoomInfoListResponse(
                                List(20) { RoomInfo.EMPTY }.toImmutableList(),
                                Pagination("opaque-next", null, 60),
                            ),
                        )
                    } else {
                        CustomResult.Failure(UiOperationError.from(IllegalStateException("Synthetic page failure")))
                    }
                }
            val mediator = CustomRemoteMediator(keys.wrap("rooms"), source) { _, _ -> commits++ }
            assertIs<RemoteMediator.MediatorResult.Error>(
                mediator.load(LoadType.REFRESH, roomState(List(60) { RoomInfo.EMPTY }, 45)),
            )
            assertEquals(0, commits)
            assertEquals("previous-next", keys.getNextRemoteKey("rooms")?.key)
        }

    @Test
    fun cancelledRefreshDoesNotCommitPartialRange() =
        runTest {
            val keys = MemoryRemoteKeys()
            val started = CompletableDeferred<Unit>()
            var commits = 0
            val source =
                RegularPagingSource<RoomInfo> { key, _ ->
                    if (key == null) {
                        CustomResult.success(
                            RoomInfoListResponse(
                                List(20) { RoomInfo.EMPTY }.toImmutableList(),
                                Pagination("opaque-next", null, 60),
                            ),
                        )
                    } else {
                        started.complete(Unit)
                        awaitCancellation()
                    }
                }
            val mediator = CustomRemoteMediator(keys.wrap("rooms"), source) { _, _ -> commits++ }
            val job = launch { mediator.load(LoadType.REFRESH, roomState(List(60) { RoomInfo.EMPTY }, 45)) }
            started.await()
            job.cancel()
            job.join()
            assertTrue(job.isCancelled)
            assertEquals(0, commits)
            assertEquals(null, keys.getNextRemoteKey("rooms"))
        }

    private fun roomState(rooms: List<RoomInfo>, anchor: Int) =
        PagingState(
            listOf(PagingSource.LoadResult.Page<String, RoomInfo>(rooms, null, null, 0, 0)),
            anchor,
            PagingConfig(20),
            0,
        )
}

private class MemoryRemoteKeys : RemoteKeyStorage {
    private val previous = mutableMapOf<String, RemoteKeys>()
    private val next = mutableMapOf<String, RemoteKeys>()
    override suspend fun getPreRemoteKey(collection: String) = previous[collection]
    override suspend fun getNextRemoteKey(collection: String) = next[collection]
    override suspend fun savePreRemoteKey(remoteKeys: RemoteKeys) {
        previous[remoteKeys.collectionName] = remoteKeys
    }
    override suspend fun saveNextRemoteKey(remoteKeys: RemoteKeys) {
        next[remoteKeys.collectionName] = remoteKeys
    }
    override suspend fun deletePreRemoteKey(collection: String) {
        previous.remove(collection)
    }
    override suspend fun deleteNextRemoteKey(collection: String) {
        next.remove(collection)
    }
}
