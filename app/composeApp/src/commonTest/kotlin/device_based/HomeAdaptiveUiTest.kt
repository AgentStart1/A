/*
 * This is a private project. All rights reserved.
 */

package device_based

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import com.storyteller_f.alphaheart.api.RoomActivityInfo
import com.storyteller_f.alphaheart.api.RoomActivityListResponse
import com.storyteller_f.alphaheart.api.RoomInfoListResponse
import com.storyteller_f.alphaheart.api.TopicActivityListResponse
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.common.ActivityPreviewHost
import com.storyteller_f.alphaheart.app.common.MessagePreview
import com.storyteller_f.alphaheart.app.common.MessageSummary
import com.storyteller_f.alphaheart.app.common.OnRoomActivityRefreshRequested
import com.storyteller_f.alphaheart.app.common.OnRoomRead
import com.storyteller_f.alphaheart.app.common.PreviewState
import com.storyteller_f.alphaheart.app.common.RoomActivityPreview
import com.storyteller_f.alphaheart.app.pages.ActivityPreviewSection
import com.storyteller_f.alphaheart.app.pages.AdaptiveEditorLayout
import com.storyteller_f.alphaheart.app.pages.AdaptiveFormLayout
import com.storyteller_f.alphaheart.app.pages.AdaptiveItemGrid
import com.storyteller_f.alphaheart.app.pages.AdaptiveSectionScaffold
import com.storyteller_f.alphaheart.app.pages.DetailReadingColumn
import com.storyteller_f.alphaheart.app.pages.HomeFeedLayout
import com.storyteller_f.alphaheart.app.pages.ObserveActivityEvents
import com.storyteller_f.alphaheart.app.pages.ObserveUnreadMessageCountEvents
import com.storyteller_f.alphaheart.app.pages.RoomActivityPreviewContent
import com.storyteller_f.alphaheart.app.pages.room.RoomRowContent
import com.storyteller_f.alphaheart.app.pages.search.DesktopSearchLayout
import com.storyteller_f.alphaheart.app.pages.topic.addLatestRoomAnchor
import com.storyteller_f.alphaheart.app.refresh
import com.storyteller_f.alphaheart.client.compose_core.common.PagingViewModel
import com.storyteller_f.alphaheart.client.compose_core.common.RegularPagingSource
import com.storyteller_f.alphaheart.client.compose_core.components.NavRoute
import com.storyteller_f.alphaheart.client.compose_core.components.StateView
import com.storyteller_f.alphaheart.client.core.LoadingHandler
import com.storyteller_f.alphaheart.client.core.LoadingState
import com.storyteller_f.result.CustomResult
import com.storyteller_f.shared.model.RoomInfo
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.jetbrains.compose.resources.getString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class HomeAdaptiveUiTest {
    @Test
    fun formRemainsScrollableInShortWindows() =
        runComposeUiTest {
            val width = mutableStateOf(1200.dp)
            setContent {
                MaterialTheme {
                    Box(Modifier.requiredSize(width.value, 180.dp)) {
                        AdaptiveFormLayout {
                            repeat(8) { index ->
                                Text("Field $index", Modifier.height(100.dp).testTag("field-$index"))
                            }
                        }
                    }
                }
            }
            onNodeWithTag("field-7").performScrollTo().assertIsDisplayed()
            runOnIdle { width.value = 540.dp }
            onNodeWithTag("field-7").performScrollTo().assertIsDisplayed()
        }

    @Test
    fun formFieldsReflowAndKeepInput() =
        runComposeUiTest {
            val width = mutableStateOf(540.dp)
            setContent {
                MaterialTheme {
                    Box(Modifier.requiredSize(width.value, 400.dp)) {
                        AdaptiveFormLayout {
                            var draft by rememberSaveable { mutableStateOf("") }
                            BasicTextField(draft, { draft = it }, Modifier.testTag("form-draft"))
                            Text("Second field", Modifier.testTag("form-second"))
                        }
                    }
                }
            }
            onNodeWithTag("form-draft").performTextReplacement("Keep this draft")
            val compactFirst = onNodeWithTag("form-draft").getUnclippedBoundsInRoot()
            val compactSecond = onNodeWithTag("form-second").getUnclippedBoundsInRoot()
            assertTrue(compactSecond.top > compactFirst.top)
            runOnIdle { width.value = 1200.dp }
            val wideFirst = onNodeWithTag("form-draft").getUnclippedBoundsInRoot()
            val wideSecond = onNodeWithTag("form-second").getUnclippedBoundsInRoot()
            assertEquals(wideFirst.top, wideSecond.top)
            assertTrue(wideSecond.left >= wideFirst.right)
            onNodeWithTag("form-draft").assertTextEquals("Keep this draft")
            runOnIdle { width.value = 540.dp }
            onNodeWithTag("form-draft").assertTextEquals("Keep this draft")
        }

    @Test
    fun editorKeepsDraftWhenPreviewAppears() =
        runComposeUiTest {
            val width = mutableStateOf(700.dp)
            setContent {
                MaterialTheme {
                    Box(Modifier.requiredSize(width.value, 500.dp)) {
                        AdaptiveEditorLayout(preview = { Text("Live preview") }) {
                            var draft by rememberSaveable { mutableStateOf("") }
                            BasicTextField(draft, { draft = it }, Modifier.testTag("editor-draft"))
                        }
                    }
                }
            }
            onNodeWithTag("editor-draft").performTextReplacement("Retained editor")
            onNodeWithText("Live preview").assertDoesNotExist()
            runOnIdle { width.value = 1200.dp }
            onNodeWithText("Live preview").assertIsDisplayed()
            onNodeWithTag("editor-draft").assertTextEquals("Retained editor")
            runOnIdle { width.value = 700.dp }
            onNodeWithText("Live preview").assertDoesNotExist()
            onNodeWithTag("editor-draft").assertTextEquals("Retained editor")
        }

    @Test
    fun resizingPreservesSectionSearchAndScroll() =
        runComposeUiTest {
            val width = mutableStateOf(540.dp)
            val routes =
                listOf(
                    "World",
                    "Communities",
                    "Rooms",
                ).map { NavRoute("/${it.lowercase()}", Icons.Default.Public, it) }
            setContent {
                MaterialTheme {
                    Box(Modifier.requiredSize(width.value, 600.dp)) {
                        AdaptiveSectionScaffold(rememberPagerState { 3 }, routes, false, search = { page, _ ->
                            var query by rememberSaveable { mutableStateOf("") }
                            BasicTextField(query, { query = it }, Modifier.testTag("query-$page"))
                        }) { page ->
                            LazyColumn(Modifier.fillMaxSize().testTag("list-$page")) {
                                items(100) { index -> Text("$page:$index", Modifier.requiredSize(200.dp, 48.dp)) }
                            }
                        }
                    }
                }
            }
            onNodeWithTag("Rooms").performClick()
            onNodeWithTag("query-2").performTextReplacement("saved room search")
            onNodeWithTag("list-2").performScrollToIndex(20)
            runOnIdle { width.value = 1000.dp }
            onNodeWithTag("query-2").assertTextEquals("saved room search")
            onNodeWithText("2:20").assertExists()
            runOnIdle { width.value = 540.dp }
            onNodeWithTag("query-2").assertTextEquals("saved room search")
            onNodeWithText("2:20").assertExists()
        }

    @Test
    fun contextAppearsWithoutResettingReadingContent() =
        runComposeUiTest {
            val width = mutableStateOf(900.dp)
            setContent {
                MaterialTheme {
                    Box(Modifier.requiredSize(width.value, 600.dp)) {
                        HomeFeedLayout(context = {
                            Column {
                                Text("Home context")
                                var query by rememberSaveable { mutableStateOf("") }
                                BasicTextField(query, { query = it }, Modifier.testTag("context-state"))
                            }
                        }) {
                            var query by rememberSaveable { mutableStateOf("") }
                            BasicTextField(query, { query = it }, Modifier.testTag("reading-state"))
                        }
                    }
                }
            }
            onNodeWithTag("reading-state").performTextReplacement("keep reading")
            onNodeWithText("Home context").assertDoesNotExist()
            runOnIdle { width.value = 1280.dp }
            onNodeWithText("Home context").assertExists()
            onNodeWithTag("reading-state").assertTextEquals("keep reading")
            onNodeWithTag("context-state").performTextReplacement("saved context")
            runOnIdle { width.value = 900.dp }
            onNodeWithText("Home context").assertDoesNotExist()
            onNodeWithTag("reading-state").assertTextEquals("keep reading")
            runOnIdle { width.value = 1280.dp }
            onNodeWithTag("context-state").assertTextEquals("saved context")
        }

    @Test
    fun searchResultsUseMoreWidthAndFitShortViewport() =
        runComposeUiTest {
            setContent {
                MaterialTheme {
                    Box(Modifier.requiredSize(1000.dp, 320.dp).testTag("viewport")) {
                        DesktopSearchLayout("Communities", true, {
                            BasicTextField("Design", {}, Modifier.testTag("search-input"))
                        }, {}) {
                            LazyColumn(Modifier.fillMaxSize().testTag("results")) {
                                items(100) { Text("Result $it", Modifier.requiredSize(200.dp, 48.dp)) }
                            }
                        }
                    }
                }
            }
            val inputBounds = onNodeWithTag("search-input").getUnclippedBoundsInRoot()
            val resultsBounds = onNodeWithTag("results").getUnclippedBoundsInRoot()
            val viewportBounds = onNodeWithTag("viewport").getUnclippedBoundsInRoot()
            val inputWidth = inputBounds.right - inputBounds.left
            val resultsWidth = resultsBounds.right - resultsBounds.left
            assertTrue(inputWidth <= 640.dp)
            assertTrue(resultsWidth > inputWidth)
            assertTrue(resultsBounds.bottom <= viewportBounds.bottom)
            onNodeWithTag("results").performScrollToIndex(80)
            onNodeWithText("Result 80").assertExists()
        }

    @Test
    fun communityGridRetainsVisibleItemAcrossColumnChanges() =
        runComposeUiTest {
            val width = mutableStateOf(540.dp)
            setContent {
                MaterialTheme {
                    Box(Modifier.requiredSize(width.value, 600.dp)) {
                        AdaptiveItemGrid {
                            items(100, key = { it }) { Text("Community $it", Modifier.requiredSize(200.dp, 80.dp)) }
                        }
                    }
                }
            }
            onNodeWithTag("adaptive-grid").performScrollToIndex(20)
            runOnIdle { width.value = 1000.dp }
            onNodeWithText("Community 20").assertExists()
            runOnIdle { width.value = 540.dp }
            onNodeWithText("Community 20").assertExists()
        }

    @Test
    fun detailReadingAndComposerStayAlignedAcrossResizes() =
        runComposeUiTest {
            val width = mutableStateOf(1280.dp)
            setContent {
                MaterialTheme {
                    Box(Modifier.requiredSize(width.value, 320.dp)) {
                        DetailReadingColumn {
                            LazyColumn(Modifier.weight(1f).fillMaxSize().testTag("detail-list")) {
                                items(100) { Text("Message $it", Modifier.requiredSize(200.dp, 48.dp)) }
                            }
                            var draft by rememberSaveable { mutableStateOf("") }
                            BasicTextField(
                                draft,
                                { draft = it },
                                Modifier.fillMaxSize().weight(0.2f).testTag("composer"),
                            )
                        }
                    }
                }
            }
            val reading = onNodeWithTag("detail-list").getUnclippedBoundsInRoot()
            val composer = onNodeWithTag("composer").getUnclippedBoundsInRoot()
            assertTrue(reading.right - reading.left <= 900.dp)
            assertTrue(reading.left == composer.left && reading.right == composer.right)
            onNodeWithTag("composer").performTextReplacement("Saved draft")
            onNodeWithTag("detail-list").performScrollToIndex(20)
            runOnIdle { width.value = 390.dp }
            onNodeWithTag("composer").assertTextEquals("Saved draft")
            onNodeWithText("Message 20").assertExists()
            runOnIdle { width.value = 1280.dp }
            onNodeWithTag("composer").assertTextEquals("Saved draft")
            onNodeWithText("Message 20").assertExists()
        }

    @Test
    fun contextRetryDoesNotHideOtherSource() =
        runComposeUiTest {
            val room = RoomInfo.EMPTY.copy(name = "Room preview")
            val preview =
                RoomActivityPreview(
                    room,
                    42,
                    MessagePreview(MessageSummary.Unavailable, "Sample author", "2026-01-01 12:00"),
                )
            val unread = mutableStateOf<PreviewState<RoomActivityPreview>>(PreviewState.Failed)
            setContent {
                MaterialTheme {
                    Column(Modifier.requiredSize(300.dp, 600.dp)) {
                        ActivityPreviewSection("Unread", unread.value, {
                            unread.value = PreviewState.Ready(persistentListOf(preview), 1)
                        }) { RoomActivityPreviewContent(it, open = {}) }
                        ActivityPreviewSection("Pinned", PreviewState.Ready(persistentListOf("Pin preview"), 1), {}) {
                            Text(it)
                        }
                    }
                }
            }
            onNodeWithText("Pin preview").assertExists()
            onAllNodesWithText(getString(Res.string.refresh))[0].performClick()
            onNodeWithText("Pin preview").assertExists()
            onNodeWithText("42").assertExists()
        }

    @Test
    fun roomPreviewClickInvokesNavigation() =
        runComposeUiTest {
            val room = RoomInfo.EMPTY.copy(id = 42, name = "Room preview")
            val preview =
                RoomActivityPreview(
                    room,
                    42,
                    MessagePreview(MessageSummary.Unavailable, "Sample author", "2026-01-01 12:00"),
                )
            var opened = 0
            setContent {
                MaterialTheme { RoomActivityPreviewContent(preview, open = { opened++ }) }
            }
            onNodeWithText("Room preview").performClick()
            runOnIdle { assertEquals(1, opened) }
        }

    @Test
    fun roomSummaryAndExactCountSurviveResizing() =
        runComposeUiTest {
            val width = mutableStateOf(1000.dp)
            val room = RoomInfo.EMPTY.copy(name = "Room preview")
            val preview =
                RoomActivityPreview(
                    room,
                    42,
                    MessagePreview(MessageSummary.Text("Recent message"), "Sample author", "2026-01-01 12:00"),
                )
            setContent {
                MaterialTheme {
                    Box(Modifier.requiredSize(width.value, 240.dp)) {
                        RoomRowContent(
                            room,
                            null,
                            width.value >= 760.dp,
                            Modifier.fillMaxWidth().clickable {}.testTag("room-row").padding(16.dp),
                            { Box(Modifier.requiredSize(60.dp, 60.dp)) },
                            {},
                            preview,
                        )
                    }
                }
            }
            val descriptions =
                onNodeWithTag("room-row").fetchSemanticsNode().config.getOrNull(SemanticsProperties.ContentDescription)
            // Native accessible names prefer merged descriptions over text when a preview provides one.
            descriptions?.let { labels ->
                assertTrue(labels.contains("Room preview"))
                assertTrue(labels.contains("Recent message"))
            }
            onNodeWithText("Room preview").assertExists()
            onNodeWithText("Recent message").assertExists()
            onNodeWithText("42").assertExists()
            runOnIdle { width.value = 390.dp }
            onNodeWithText("Recent message").assertExists()
            onNodeWithText("42").assertExists()
            onNodeWithText("Sample author").assertExists()
            onNodeWithText("2026-01-01 12:00").assertExists()
        }

    @Test
    fun listRefreshAlsoRefreshesSupplementalActivity() =
        runComposeUiTest {
            var refreshes = 0
            val model =
                object : PagingViewModel<RoomInfo>() {
                    override val flow =
                        flowOf(
                            PagingData.from(
                                listOf(RoomInfo.EMPTY),
                                sourceLoadStates =
                                LoadStates(
                                    LoadState.NotLoading(false),
                                    LoadState.NotLoading(true),
                                    LoadState.NotLoading(true),
                                ),
                            ),
                        )
                }
            setContent {
                MaterialTheme {
                    Box(Modifier.requiredSize(800.dp, 300.dp)) {
                        StateView(model, showRefresh = true, onRefresh = { refreshes++ }) { Text("Loaded room") }
                    }
                }
            }
            onNodeWithText("Loaded room").assertExists()
            onNodeWithText(getString(Res.string.refresh)).performClick()
            runOnIdle { assertEquals(1, refreshes) }
        }

    @Test
    fun roomAnchorFollowsNewMessagesAndPreservesHistory() =
        runComposeUiTest {
            val messages = mutableStateOf(listOf(4, 3, 2, 1))
            val list = androidx.compose.foundation.lazy.LazyListState()
            setContent {
                MaterialTheme {
                    LazyColumn(
                        state = list,
                        reverseLayout = true,
                        modifier = Modifier.requiredSize(500.dp, 200.dp).testTag("messages"),
                    ) {
                        addLatestRoomAnchor()
                        items(messages.value.size, key = { messages.value[it] }) { index ->
                            Text("Message ${messages.value[index]}", Modifier.height(80.dp))
                        }
                    }
                }
            }
            onNodeWithText("Message 4").assertIsDisplayed()
            runOnIdle { messages.value = listOf(5) + messages.value }
            onNodeWithText("Message 5").assertIsDisplayed()
            onNodeWithTag("messages").performScrollToIndex(4)
            val historyKey = runOnIdle { list.layoutInfo.visibleItemsInfo.first().key }
            runOnIdle { messages.value = listOf(6) + messages.value }
            runOnIdle { assertEquals(historyKey, list.layoutInfo.visibleItemsInfo.first().key) }
        }

    @Test
    fun roomRefreshEventsReloadEmptyList() =
        runComposeUiTest {
            val events = MutableSharedFlow<Any>(extraBufferCapacity = 1)
            val loads = MutableStateFlow(0)
            val model =
                object : PagingViewModel<RoomInfo>() {
                    override val flow =
                        androidx.paging.Pager(androidx.paging.PagingConfig(20)) {
                            RegularPagingSource<RoomInfo> { _, _ ->
                                loads.value++
                                CustomResult.success(RoomInfoListResponse(persistentListOf()))
                            }
                        }.flow
                }
            setContent {
                MaterialTheme {
                    Box(Modifier.requiredSize(800.dp, 300.dp)) {
                        StateView(model, refreshEvents = events) { Text("Room content") }
                    }
                }
            }
            waitUntil { events.subscriptionCount.value == 1 && loads.value > 0 }
            awaitIdle()
            val beforeRefresh = loads.value
            assertTrue(events.tryEmit(OnRoomRead(1)))
            waitUntil { loads.value > beforeRefresh }
            onNodeWithText("Room content").assertDoesNotExist()
        }

    @Test
    fun previewBindingRefreshesOnlyWhilePageIsActive() =
        runComposeUiTest {
            val owner =
                object : LifecycleOwner {
                    override val lifecycle = LifecycleRegistry.createUnsafe(this)
                }
            val events = MutableSharedFlow<Any>(extraBufferCapacity = 1)
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            val host =
                ActivityPreviewHost(scope, Dispatchers.Default.limitedParallelism(1), Dispatchers.Default, {
                    CustomResult.success(
                        RoomActivityListResponse(
                            persistentListOf(RoomActivityInfo(RoomInfo.EMPTY.copy(name = "Live room"), null, null, 5)),
                        ),
                    )
                }, { CustomResult.success(TopicActivityListResponse(persistentListOf())) })
            try {
                setContent {
                    CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                        MaterialTheme {
                            ObserveActivityEvents(host, context = true, events = events)
                            val unread by host.unread.collectAsStateWithLifecycle()
                            ActivityPreviewSection(
                                "Unread",
                                unread,
                                { host.refreshUnread() },
                            ) { RoomActivityPreviewContent(it) }
                        }
                    }
                }
                runOnIdle { owner.lifecycle.currentState = Lifecycle.State.RESUMED }
                awaitIdle()
                waitUntil { host.unread.value is PreviewState.Ready }
                onNodeWithText("Live room").assertExists()
                runOnIdle { owner.lifecycle.currentState = Lifecycle.State.CREATED }
                awaitIdle()
                waitUntil { host.unread.value is PreviewState.Idle }
                assertTrue(events.tryEmit(OnRoomRead(1)))
                runOnIdle { owner.lifecycle.currentState = Lifecycle.State.RESUMED }
                awaitIdle()
                waitUntil { host.unread.value is PreviewState.Ready }
                onNodeWithText("Live room").assertExists()
            } finally {
                host.close()
                scope.cancel()
            }
        }

    @Test
    fun unreadBadgeRefreshesAfterReadAndResume() =
        runComposeUiTest {
            val owner =
                object : LifecycleOwner {
                    override val lifecycle = LifecycleRegistry.createUnsafe(this)
                }
            val events = MutableSharedFlow<Any>(extraBufferCapacity = 1)
            val refreshes = MutableStateFlow(0)
            val handler =
                object : LoadingHandler<Boolean> {
                    override val state = MutableStateFlow<LoadingState?>(LoadingState.Loading)
                    override val data = MutableStateFlow<Boolean?>(false)
                    override fun refresh() {
                        refreshes.value++
                    }
                }
            setContent {
                CompositionLocalProvider(LocalLifecycleOwner provides owner) {
                    ObserveUnreadMessageCountEvents(handler, events)
                }
            }
            runOnIdle { owner.lifecycle.currentState = Lifecycle.State.RESUMED }
            awaitIdle()
            runOnIdle { assertEquals(0, refreshes.value) }
            runOnIdle { handler.state.value = LoadingState.Done }
            waitUntil { events.subscriptionCount.value == 1 && refreshes.value == 1 }
            runOnIdle { handler.state.value = LoadingState.Loading }
            assertTrue(events.tryEmit(OnRoomRead(1)))
            awaitIdle()
            runOnIdle { assertEquals(1, refreshes.value) }
            runOnIdle { handler.state.value = LoadingState.Done }
            waitUntil { refreshes.value == 2 }
            assertTrue(events.tryEmit(OnRoomActivityRefreshRequested))
            waitUntil { refreshes.value == 3 }
            runOnIdle { owner.lifecycle.currentState = Lifecycle.State.CREATED }
            awaitIdle()
            waitUntil { events.subscriptionCount.value == 0 }
            assertTrue(events.tryEmit(OnRoomRead(1)))
            runOnIdle { assertEquals(3, refreshes.value) }
            runOnIdle { owner.lifecycle.currentState = Lifecycle.State.RESUMED }
            awaitIdle()
            waitUntil { refreshes.value == 4 }
            runOnIdle { owner.lifecycle.currentState = Lifecycle.State.DESTROYED }
        }

    @Test
    fun unreadNavigationDescriptionSurvivesResize() =
        runComposeUiTest {
            val width = mutableStateOf(900.dp)
            val unread = mutableStateOf(true)
            val total = mutableStateOf(123L)
            setContent {
                MaterialTheme {
                    val pager = rememberPagerState { 1 }
                    Box(Modifier.requiredSize(width.value, 600.dp)) {
                        AdaptiveSectionScaffold(
                            pager,
                            listOf(NavRoute("/rooms", Icons.Default.Public, "Rooms")),
                            unread.value,
                            unreadMessageCount = total.value,
                            search = { _, _ -> },
                        ) { Text("Room content") }
                    }
                }
            }
            val unreadDescription =
                onNodeWithTag("Rooms").fetchSemanticsNode()
                    .config[androidx.compose.ui.semantics.SemanticsProperties.ContentDescription].first()
            assertTrue(unreadDescription.startsWith("Rooms, "))
            onNodeWithContentDescription(unreadDescription).assertExists()
            onNodeWithText("123", useUnmergedTree = true).assertExists()
            runOnIdle {
                total.value = 122L
                width.value = 390.dp
            }
            onNodeWithContentDescription(unreadDescription).assertExists()
            onNodeWithText("122", useUnmergedTree = true).assertExists()
            runOnIdle { unread.value = false }
            onNodeWithContentDescription(unreadDescription).assertDoesNotExist()
            onNodeWithContentDescription("Rooms").assertExists()
        }
}
