/*
 * This is a private project. All rights reserved.
 */

package device_based

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.cachedIn
import androidx.paging.compose.LazyPagingItems
import com.storyteller_f.alphaheart.api.RoomInfoListResponse
import com.storyteller_f.alphaheart.app.common.OnRoomRead
import com.storyteller_f.alphaheart.client.compose_core.common.PagingViewModel
import com.storyteller_f.alphaheart.client.compose_core.common.buildPager
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.compose_core.components.pagingItems
import com.storyteller_f.alphaheart.client.room.RemoteKeyRoomStorage
import com.storyteller_f.alphaheart.client.room.RoomRoomInfoStorage
import com.storyteller_f.alphaheart.client.room.getRoomDatabase
import com.storyteller_f.result.CustomResult
import com.storyteller_f.shared.model.RoomInfo
import com.storyteller_f.shared.obj.Pagination
import com.storyteller_f.storage.RemoteKeyStorageWrapper
import com.storyteller_f.storage.RoomCollection
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

@OptIn(ExperimentalTestApi::class)
internal class RoomPagingUiTest {
    @Test
    fun roomEventRefreshPreservesPagedScrollAnchor() = assertRoomRefresh(1, 0)

    @Test
    fun serverOrderRefreshPreservesRoomKeyAndOffset() = assertRoomRefresh(2, 3)

    private fun assertRoomRefresh(revision: Int, indexShift: Int) =
        runAppComposeUiTest {
            val fixture = RoomPagingFixture()
            val list = LazyListState()
            try {
                setContent { RoomPagingContent(fixture, list) }
                awaitExternalCondition {
                    list.layoutInfo.totalItemsCount >= 60 && fixture.events.subscriptionCount.value == 1
                }
                val commitsBeforeScroll = fixture.commitCount.value
                onNodeWithTag("paged-rooms").performScrollToIndex(49)
                awaitExternalCondition {
                    fixture.commitCount.value > commitsBeforeScroll &&
                        fixture.presentedItems?.loadState?.append is LoadState.NotLoading
                }
                val anchor = runOnIdle { list.firstVisibleItemIndex to list.firstVisibleItemScrollOffset }
                val key = runOnIdle { list.layoutInfo.visibleItemsInfo.first().key }
                runOnIdle { fixture.revision.value = revision }
                assertTrue(fixture.events.tryEmit(OnRoomRead(1)))
                awaitExternalCondition { fixture.committed.value == revision }
                val label = "Room 48 revision $revision"
                awaitExternalCondition { onAllNodesWithText(label).fetchSemanticsNodes().isNotEmpty() }
                onNodeWithText(label).assertIsDisplayed()
                runOnIdle {
                    assertEquals(anchor.first + indexShift, list.firstVisibleItemIndex)
                    assertEquals(anchor.second, list.firstVisibleItemScrollOffset)
                    assertEquals(key, list.layoutInfo.visibleItemsInfo.first().key)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Throwable) {
                val diagnostic =
                    runCatching { runOnIdle { fixture.diagnostics(list) } }
                        .getOrElse { "Unable to capture paging diagnostics: $it" }
                // Wasm reporters do not serialize suppressed exceptions; retain the cause and diagnostic.
                throw AssertionError("Room paging failed: ${failure.message.orEmpty()}\n$diagnostic", failure)
            } finally {
                closeTestDatabase(fixture.database, fixture.pagingScope)
            }
        }
}

@Composable
private fun RoomPagingContent(fixture: RoomPagingFixture, list: LazyListState) {
    MaterialTheme {
        Box(Modifier.requiredSize(800.dp, 400.dp)) {
            StateView(fixture.model, refreshEvents = fixture.events) { items ->
                SideEffect { fixture.presentedItems = items }
                LazyColumn(state = list, modifier = Modifier.fillMaxSize().testTag("paged-rooms")) {
                    pagingItems(items, { it.id }) { index ->
                        Text(items[index]?.name.orEmpty(), Modifier.height(64.dp))
                    }
                }
            }
        }
    }
}

private class RoomPagingFixture {
    val pagingScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val database = getRoomDatabase("paging-${Uuid.random().toHexString()}")
    val events = MutableSharedFlow<Any>(extraBufferCapacity = 1)
    val revision = MutableStateFlow(0)
    val committed = MutableStateFlow(-1)
    val commitCount = MutableStateFlow(0)
    var presentedItems: LazyPagingItems<RoomInfo>? = null
    private val operations = MutableStateFlow<List<String>>(emptyList())
    private val tokens = listOf(null, "opaque/alpha", "opaque/beta", "opaque/gamma", "opaque/delta", null)
    private val rows = (0 until 100).map { RoomInfo.EMPTY.copy(id = (it * 37 % 101 + 1).toLong(), name = "Room $it") }
    val model =
        object : PagingViewModel<RoomInfo>() {
            override val flow =
                buildPager(
                    RoomCollection.CommunityRooms(1),
                    RemoteKeyStorageWrapper(RemoteKeyRoomStorage(database), "paging-fixture"),
                    RoomRoomInfoStorage(database),
                    onUpdated = {
                        operations.update { it + "commit observed revision=${revision.value}" }
                        committed.value = revision.value
                        commitCount.update { it + 1 }
                    },
                ) { token, size ->
                    val requestedRevision = revision.value
                    val page = tokens.dropLast(1).indexOf(token)
                    val ordered = if (revision.value < 2) rows else rows.takeLast(3) + rows.dropLast(3)
                    val data =
                        ordered.drop(
                            page * 20,
                        ).take(size).map { it.copy(name = "${it.name} revision ${revision.value}") }
                    val firstName = data.firstOrNull()?.name ?: "<empty>"
                    val lastName = data.lastOrNull()?.name ?: "<empty>"
                    operations.update {
                        it + "request token=${token ?: "<initial>"} page=$page size=$size " +
                            "revisionAtStart=$requestedRevision returned=$firstName..$lastName"
                    }
                    CustomResult.success(
                        RoomInfoListResponse(data.toImmutableList(), Pagination(tokens[page + 1], null, 100)),
                    )
                }.flow.cachedIn(pagingScope)
        }

    fun diagnostics(list: LazyListState): String =
        buildString {
            appendLine("Room paging diagnostic: revision=${revision.value}, committed=${committed.value}")
            appendLine(
                "list first=${list.firstVisibleItemIndex}:${list.firstVisibleItemScrollOffset}, total=${list.layoutInfo.totalItemsCount}",
            )
            appendLine(
                "visible=${list.layoutInfo.visibleItemsInfo.map { "${it.index}:${it.key}@${it.offset}+${it.size}" }}",
            )
            val items = presentedItems
            val snapshot = items?.itemSnapshotList
            appendLine(
                "paging count=${items?.itemCount ?: -1}, before=${snapshot?.placeholdersBefore ?: -1}, " +
                    "after=${snapshot?.placeholdersAfter ?: -1}",
            )
            appendLine("loadStates=${items?.loadState?.toString() ?: "<unavailable>"}")
            appendLine("loaded=${snapshot?.items?.map { it.name }.orEmpty()}")
            appendLine("operations=${operations.value}")
        }
}
