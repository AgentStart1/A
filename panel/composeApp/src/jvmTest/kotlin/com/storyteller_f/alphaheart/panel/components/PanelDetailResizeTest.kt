/*
 * This is a private project. All rights reserved.
 */

@file:OptIn(androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi::class)

package com.storyteller_f.alphaheart.panel.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.adaptive.Posture
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import com.storyteller_f.alphaheart.client.compose_core.components.InfoTable
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingInteractions
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingTab
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingTabRow
import com.storyteller_f.alphaheart.client.compose_core.navigation.NavRoute
import com.storyteller_f.alphaheart.client.compose_core.utils.readingTypography
import com.storyteller_f.alphaheart.panel.PanelNavigationDrawer
import com.storyteller_f.alphaheart.panel.calculatePanelPaneDirective
import com.storyteller_f.alphaheart.panel.common.PanelAllTitlesScreen
import com.storyteller_f.alphaheart.panel.common.PanelTitleDetailScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import java.awt.datatransfer.DataFlavor
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingHorizontalPager as HorizontalPager

@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
internal class PanelDetailResizeTest {
    @Test
    fun readingTabsKeepTheirHeightAndWrapWhenThePaneNarrows() {
        runDesktopComposeUiTest(width = 720, height = 300) {
            val width = mutableStateOf(680.dp)
            val selected = mutableStateOf(0)
            val labels = listOf("Communities", "Rooms", "Subscriptions")
            setContent {
                MaterialTheme(typography = readingTypography) {
                    Box(Modifier.width(width.value)) {
                        ReadingTabRow(wrap = true) {
                            labels.forEachIndexed { index, label ->
                                ReadingTab(selected.value == index, { selected.value = index }) { Text(label) }
                            }
                        }
                    }
                }
            }
            fun tab(label: String) = onNode(hasText(label) and hasClickAction())
            labels.forEach { tab(it).assertHeightIsEqualTo(37.dp) }
            val firstTop = tab(labels.first()).getUnclippedBoundsInRoot().top
            assertEquals(firstTop, tab(labels.last()).getUnclippedBoundsInRoot().top)
            runOnIdle { width.value = 180.dp }
            val lastNarrowTop = tab(labels.last()).getUnclippedBoundsInRoot().top
            val firstNarrowTop = tab(labels.first()).getUnclippedBoundsInRoot().top
            assertTrue(lastNarrowTop > firstNarrowTop)
            tab(labels.last()).performClick().assertIsSelected().assertHeightIsEqualTo(40.dp)
            tab(labels.first()).assertIsNotSelected().assertHeightIsEqualTo(40.dp)
        }
    }

    @Test
    fun detailPagersFillTheNewPaneWithoutLosingDraftTabOrVerticalScroll() {
        // Desktop Compose tests render on Swing's EDT; Nav3 lifecycle checks must use that same main dispatcher.
        Dispatchers.setMain(Dispatchers.Swing)
        val originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.ENGLISH)
        try {
            runDesktopComposeUiTest(width = 1400, height = 900) {
                val width = mutableStateOf(1400.dp)
                var infoMounts = 0
                val memory = java.awt.datatransfer.Clipboard("isolated-detail-resize")
                val clipboard =
                    object : Clipboard {
                        override val nativeClipboard: Any get() = memory
                        override suspend fun getClipEntry(): ClipEntry? = null
                        override suspend fun setClipEntry(clipEntry: ClipEntry?) = Unit
                    }
                setContent {
                    MaterialTheme {
                        CompositionLocalProvider(LocalClipboard provides clipboard) {
                            ReadingInteractions { DetailSceneFixture(width.value) { infoMounts++ } }
                        }
                    }
                }
                assertDetailNavigationGeometry(compact = false)
                onNodeWithTag("draft").performTextInput("Retained draft")
                onNodeWithTag("info-table").performScrollToNode(hasText("Field 20"))
                onNodeWithTag("info-table").performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 180f) }
                assertDetailNavigationGeometry(compact = false, verifyField = true)
                onNode(hasContentDescription("View full value") and hasText("Value 20")).performMouseInput { click() }
                onNodeWithText("Copy").assertIsDisplayed().performMouseInput { click() }
                onNodeWithText("Copy").assertDoesNotExist()
                waitUntil { memory.isDataFlavorAvailable(DataFlavor.stringFlavor) }
                assertEquals("Value 20", memory.getData(DataFlavor.stringFlavor))
                for (nextWidth in listOf(390.dp, 1400.dp, 390.dp)) {
                    runOnIdle { width.value = nextWidth }
                    assertDetailNavigationGeometry(compact = nextWidth == 390.dp, verifyField = true)
                    val pane = onNodeWithTag("scene-root").fetchSemanticsNode().boundsInRoot
                    val body = onNodeWithTag("detail-body").fetchSemanticsNode().boundsInRoot
                    assertTrue(body.left >= pane.left, "Detail body escaped left of its scene: $body / $pane")
                    assertTrue(body.right <= pane.right, "Detail body escaped right of its scene: $body / $pane")
                    if (nextWidth == 390.dp) assertEquals(pane.width, body.width, 1f)
                    val scroll =
                        onNodeWithTag("info-table").fetchSemanticsNode()
                            .config[SemanticsProperties.VerticalScrollAxisRange].value()
                    assertTrue(scroll > 0f, "Resize reset vertical reading position")
                    onNodeWithTag("draft").assertTextContains("Retained draft", substring = true)
                    onNodeWithContentDescription("Information").assertIsSelected()
                }
                runOnIdle { assertEquals(1, infoMounts) }
                onNodeWithContentDescription("Logs").performClick()
                runOnIdle { width.value = 1400.dp }
                onNodeWithContentDescription("Logs").assertIsSelected()
                onNodeWithText("Log content").assertIsDisplayed()
            }
        } finally {
            Dispatchers.resetMain()
            Locale.setDefault(originalLocale)
        }
    }
}

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.assertDetailNavigationGeometry(compact: Boolean, verifyField: Boolean = false) {
    val pane = onNodeWithTag("detail-pane").fetchSemanticsNode().boundsInRoot
    val body = onNodeWithTag("detail-body").fetchSemanticsNode().boundsInRoot
    val information = onNodeWithContentDescription("Information").fetchSemanticsNode().boundsInRoot
    val logs = onNodeWithContentDescription("Logs").fetchSemanticsNode().boundsInRoot
    if (compact) {
        assertEquals(390f, pane.width, 1f)
        assertEquals(pane.left, body.left, 1f)
        assertEquals(pane.width, body.width, 1f)
        assertEquals(information.top, logs.top, 1f)
        assertTrue(logs.left >= information.right, "Expected horizontal bottom navigation")
    } else {
        assertTrue(pane.width >= 600f, "Fixture must actually cross the detail rail breakpoint: $pane")
        assertEquals(pane.left + 84f, body.left, 1f)
        assertEquals(information.left, logs.left, 1f)
        assertTrue(logs.top >= information.bottom, "Expected vertical rail navigation")
    }
    if (verifyField) {
        val tableNode = onNodeWithTag("info-table").fetchSemanticsNode()
        val table = tableNode.boundsInRoot
        val fields = listOf("Field 20", "Value 20").associateWith { onNodeWithText(it).fetchSemanticsNode() }
        val diagnostic =
            "compact=$compact; pane=$pane; body=$body; table=$table " +
                "rawTable=${tableNode.positionInRoot}/${tableNode.size}; " +
                "scroll=${tableNode.config[SemanticsProperties.VerticalScrollAxisRange].value()}; " +
                fields.entries.joinToString { (text, node) ->
                    "$text clipped=${node.boundsInRoot} raw=${node.positionInRoot}/${node.size}"
                }
        assertTrue(table.left >= body.left && table.right <= body.right, "InfoTable escaped its body: $table / $body")
        for ((text, field) in fields) {
            try {
                onNodeWithText(text).assertIsDisplayed()
            } catch (failure: AssertionError) {
                throw AssertionError("$text is not displayed; $diagnostic", failure)
            }
            val left = field.positionInRoot.x
            val right = left + field.size.width
            assertTrue(left >= body.left && right <= body.right, "Actual field clipped; $diagnostic")
        }
    }
}

@Composable
private fun DetailSceneFixture(width: Dp, onInfoMount: () -> Unit) {
    val stack = remember { NavBackStack<NavKey>(PanelAllTitlesScreen, PanelTitleDetailScreen(41)) }
    val owner = rememberNavigationEventDispatcherOwner(parent = null)
    val originalWindow = LocalWindowInfo.current
    val size by rememberUpdatedState(with(LocalDensity.current) { IntSize(width.roundToPx(), 620.dp.roundToPx()) })
    val dpSize by rememberUpdatedState(DpSize(width, 620.dp))
    val window =
        remember(originalWindow) {
            object : WindowInfo by originalWindow {
                override val containerSize: IntSize get() = size
                override val containerDpSize: DpSize get() = dpSize
            }
        }
    CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner, LocalWindowInfo provides window) {
        Box(Modifier.width(width).height(620.dp)) {
            PanelNavigationDrawer(
                drawerState = rememberDrawerState(DrawerValue.Closed),
                drawerContent = { permanent ->
                    if (permanent) {
                        PermanentDrawerSheet(Modifier.width(170.dp)) { Text("Panel navigation") }
                    } else {
                        ModalDrawerSheet { Text("Panel navigation") }
                    }
                },
            ) {
                DetailNavigationScene(stack, onInfoMount)
            }
        }
    }
}

@Composable
private fun DetailNavigationScene(stack: NavBackStack<NavKey>, onInfoMount: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().testTag("scene-root")) {
        val directive = calculatePanelPaneDirective(DpSize(maxWidth, maxHeight), Posture())
        val strategy = rememberListDetailSceneStrategy<NavKey>(directive = directive)
        NavDisplay(
            backStack = stack,
            entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
            sceneStrategies = listOf(strategy),
            entryProvider =
            entryProvider {
                entry<PanelAllTitlesScreen>(metadata = {
                    ListDetailSceneStrategy.listPane("titles", detailPlaceholder = { Text("Choose title") })
                }) { Text("Title directory") }
                entry<PanelTitleDetailScreen>(metadata = { ListDetailSceneStrategy.detailPane("titles") }) {
                    Box(Modifier.fillMaxSize().testTag("detail-pane")) { DetailPagerFixture(onInfoMount) }
                }
            },
        )
    }
}

@Composable
private fun DetailPagerFixture(onInfoMount: () -> Unit) {
    val state = rememberPagerState { 2 }
    val routes =
        listOf(NavRoute("/info", Icons.Default.Info, "Information"), NavRoute("/logs", Icons.Default.List, "Logs"))
    DetailNavigationScaffold(state, routes, topBar = { Text("Title detail") }) { page ->
        if (page == 0) NestedInfoFixture(onInfoMount) else Text("Log content")
    }
}

@Composable
private fun NestedInfoFixture(onMount: () -> Unit) {
    val nested = rememberPagerState { 1 }
    var draft by remember { mutableStateOf("") }
    DisposableEffect(Unit) {
        onMount()
        onDispose { }
    }
    Column {
        ReadingTabRow { ReadingTab(true, {}) { Text("Fields") } }
        HorizontalPager(nested, Modifier.weight(1f)) {
            Box(Modifier.fillMaxSize().testTag("detail-body")) {
                Column {
                    InfoTable((1..30).map { "Field $it" to "Value $it" }, Modifier.weight(1f).testTag("info-table"))
                    TextField(draft, { draft = it }, Modifier.testTag("draft"))
                }
            }
        }
    }
}
