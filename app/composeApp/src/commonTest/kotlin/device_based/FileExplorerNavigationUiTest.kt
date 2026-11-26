/*
 * This is a private project. All rights reserved.
 */

package device_based

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import com.storyteller_f.alphaheart.app.navigation.FileExplorerRoute
import com.storyteller_f.alphaheart.app.pages.file.FileExplorerNavigation
import com.storyteller_f.alphaheart.app.pages.file.UploadItemRow
import com.storyteller_f.alphaheart.app.pages.file.rememberFileExplorerBackStack
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingFilePreview
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingFilePreviewHeading
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIconButton
import com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.NavigationGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon
import com.storyteller_f.alphaheart.client.compose_core.icons.navigationIcon
import com.storyteller_f.alphaheart.client.compose_core.media.ResourceDownloadRequest
import com.storyteller_f.alphaheart.client.compose_core.navigation.NavRoute
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.storage.UploadInfo
import com.storyteller_f.storage.UploadStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class FileExplorerNavigationUiTest {
    @Test
    fun fileNavigationUsesHomeBreakpointAndKeepsScopedHistoryAndDraft() =
        runAppComposeUiTest {
            val width = mutableStateOf(600.dp)
            val stack = NavBackStack<NavKey>(FileExplorerRoute.Uploaded, FileExplorerRoute.UploadRecord)
            setContent { FileNavigationFixture(stack, width.value, allowDownloads = false) }
            onNodeWithTag("draft-/upload-record").performTextReplacement("Scoped upload draft")
            for (size in listOf(600.dp, 601.dp, 390.dp, 900.dp, 600.dp)) {
                runOnIdle { width.value = size }
                if (size <= 600.dp) {
                    onNodeWithTag("reading-bottom-nav").assertExists()
                } else {
                    onNodeWithTag("reading-bottom-nav").assertDoesNotExist()
                }
                onNodeWithTag("draft-/upload-record").assertTextEquals("Scoped upload draft")
                onNodeWithText("Download").assertDoesNotExist()
                runOnIdle {
                    assertEquals(listOf(FileExplorerRoute.Uploaded, FileExplorerRoute.UploadRecord), stack.toList())
                }
            }
        }

    @Test
    fun ordinaryFileDownloadOpensThePlatformBrowser() =
        runAppComposeUiTest {
            val opened = mutableListOf<String>()
            val file = FileInfo.EMPTY.copy(name = "document.pdf", url = "https://files.invalid/document.pdf")
            setContent {
                CompositionLocalProvider(
                    LocalUriHandler provides
                        object : UriHandler {
                            override fun openUri(uri: String) {
                                opened += uri
                            }
                        },
                ) {
                    MaterialTheme {
                        val uriHandler = LocalUriHandler.current
                        ReadingFilePreviewHeading(ResourceDownloadRequest(file.url, file.name)) {
                            uriHandler.openUri(file.url)
                        }
                    }
                }
            }
            onNodeWithContentDescription("Download").performClick()
            runOnIdle { assertEquals(listOf(file.url), opened) }
        }

    @Test
    fun ordinaryFileContentUsesCanonicalGuttersWithoutAReadingWidthCap() =
        runComposeUiTest {
            val width = mutableStateOf(390.dp)
            val file = FileInfo.EMPTY.copy(name = "document.pdf")
            setContent {
                MaterialTheme {
                    Box(Modifier.requiredSize(width.value, 700.dp)) {
                        ReadingFilePreview(ResourceDownloadRequest(file.url, file.name), {}) {
                            Box(Modifier.fillMaxSize().testTag("file-preview-pixels"))
                        }
                    }
                }
            }
            for ((size, inset) in listOf(390.dp to 14.dp, 700.dp to 22.dp, 1280.dp to 30.dp)) {
                runOnIdle { width.value = size }
                val main = onNodeWithTag("file-preview-main").getUnclippedBoundsInRoot()
                val content = onNodeWithTag("file-preview-content").getUnclippedBoundsInRoot()
                val pixels = onNodeWithTag("file-preview-pixels").getUnclippedBoundsInRoot()
                assertEquals(main.left + inset, content.left)
                assertEquals(main.right - inset, content.right)
                assertEquals(main.top + if (size <= 600.dp) 20.dp else inset, content.top)
                assertEquals(content.left, pixels.left)
                assertEquals(content.right, pixels.right)
                val copy = onNodeWithContentDescription("Copy resource address").getUnclippedBoundsInRoot()
                val download = onNodeWithContentDescription("Download").getUnclippedBoundsInRoot()
                assertEquals(copy.right, download.left)
                assertEquals(pixels.right, download.right)
                onAllNodesWithText(file.name).assertCountEquals(1)
            }
        }

    @Test
    fun communityFilesRestoreUnavailableDownloadHistoryToUploadedFiles() =
        runAppComposeUiTest {
            val stack =
                NavBackStack<NavKey>(
                    FileExplorerRoute.Uploaded,
                    FileExplorerRoute.UploadRecord,
                    FileExplorerRoute.DownloadRecord,
                )
            setContent { FileNavigationFixture(stack, 1000.dp, allowDownloads = false) }
            onNodeWithTag("page-/uploaded").assertIsDisplayed()
            runOnIdle { assertEquals(listOf(FileExplorerRoute.Uploaded), stack.toList()) }
        }

    @Test
    fun roomFilesDiscardUnavailableDownloadHistoryWhileMounted() =
        runAppComposeUiTest {
            val stack = NavBackStack<NavKey>(FileExplorerRoute.Uploaded)
            setContent { FileNavigationFixture(stack, 504.dp, allowDownloads = false) }
            runOnIdle { stack.add(FileExplorerRoute.DownloadRecord) }
            onNodeWithTag("page-/uploaded").assertIsDisplayed()
            runOnIdle { assertEquals(listOf(FileExplorerRoute.Uploaded), stack.toList()) }
        }

    @Test
    fun changingToAnOwnerWithoutDownloadsNormalizesThePagerAndHistory() =
        runAppComposeUiTest {
            val allowDownloads = mutableStateOf(true)
            val stack = NavBackStack<NavKey>(FileExplorerRoute.Uploaded, FileExplorerRoute.DownloadRecord)
            setContent { FileNavigationFixture(stack, 1000.dp, allowDownloads.value) }
            onNodeWithTag("page-/download-record").assertIsDisplayed()
            runOnIdle { allowDownloads.value = false }
            onNodeWithTag("page-/uploaded").assertIsDisplayed()
            runOnIdle { assertEquals(listOf(FileExplorerRoute.Uploaded), stack.toList()) }
        }

    @Test
    fun personalFilesKeepRestoredDownloadsAvailable() =
        runAppComposeUiTest {
            val stack = NavBackStack<NavKey>(FileExplorerRoute.Uploaded, FileExplorerRoute.DownloadRecord)
            setContent { FileNavigationFixture(stack, 1000.dp) }
            onNodeWithTag("page-/download-record").assertIsDisplayed()
            runOnIdle {
                assertEquals(listOf(FileExplorerRoute.Uploaded, FileExplorerRoute.DownloadRecord), stack.toList())
            }
        }

    @Test
    fun historySelectionAndDraftSurviveResizing() =
        runAppComposeUiTest {
            val width = mutableStateOf(1000.dp)
            val stack = NavBackStack<NavKey>(FileExplorerRoute.Uploaded)
            setContent { FileNavigationFixture(stack, width.value) }
            onNodeWithText("Upload").performClick()
            onNodeWithTag("draft-/upload-record").performTextReplacement("Keep upload draft")
            runOnIdle { width.value = 504.dp }
            onNodeWithTag("draft-/upload-record").assertTextEquals("Keep upload draft")
            runOnIdle { stack.removeLastOrNull() }
            onNodeWithTag("page-/uploaded").assertIsDisplayed()
            runOnIdle { stack.add(FileExplorerRoute.UploadRecord) }
            onNodeWithTag("draft-/upload-record").assertTextEquals("Keep upload draft")
            runOnIdle { width.value = 1000.dp }
            onNodeWithTag("draft-/upload-record").assertTextEquals("Keep upload draft")
            runOnIdle {
                assertEquals(listOf(FileExplorerRoute.Uploaded, FileExplorerRoute.UploadRecord), stack.toList())
            }
        }

    @Test
    fun compactSwipeUpdatesTheNavigationStack() =
        runAppComposeUiTest {
            val stack = NavBackStack<NavKey>(FileExplorerRoute.Uploaded)
            setContent { FileNavigationFixture(stack, 504.dp) }
            onNodeWithTag("file-navigator").performTouchInput { swipeLeft() }
            onNodeWithTag("page-/upload-record").assertIsDisplayed()
            runOnIdle {
                assertEquals(listOf(FileExplorerRoute.Uploaded, FileExplorerRoute.UploadRecord), stack.toList())
            }
        }

    @Test
    fun platformBackPopsTheFileTabAndKeepsItsRoot() =
        runAppComposeUiTest {
            val input = DirectNavigationEventInput()
            lateinit var stack: NavBackStack<NavKey>
            setContent {
                val owner = rememberNavigationEventDispatcherOwner(parent = null)
                DisposableEffect(owner) {
                    owner.navigationEventDispatcher.addInput(input)
                    onDispose { owner.navigationEventDispatcher.removeInput(input) }
                }
                CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner) {
                    stack = rememberFileExplorerBackStack()
                    FileNavigationFixture(stack, 1000.dp)
                }
            }
            onNodeWithText("Upload").performClick()
            runOnIdle { input.backCompleted() }
            onNodeWithTag("page-/uploaded").assertIsDisplayed()
            runOnIdle {
                input.backCompleted()
                assertEquals(listOf(FileExplorerRoute.Uploaded), stack.toList())
            }
        }

    @Test
    fun historyNavigationCancelsAnObsoleteDrag() =
        runAppComposeUiTest {
            val stack = NavBackStack<NavKey>(FileExplorerRoute.Uploaded, FileExplorerRoute.UploadRecord)
            setContent { FileNavigationFixture(stack, 504.dp) }
            onNodeWithTag("file-navigator").performTouchInput {
                down(center)
                moveBy(Offset(-center.x * 0.8f, 0f), delayMillis = 100)
            }
            runOnIdle { stack.removeLastOrNull() }
            onNodeWithTag("file-navigator").performTouchInput { up() }
            onNodeWithTag("page-/uploaded").assertIsDisplayed()
            assertPageFillsNavigator("/uploaded")
            runOnIdle { assertEquals(listOf(FileExplorerRoute.Uploaded), stack.toList()) }
        }

    @Test
    fun focusingTheRightmostUploadActionKeepsTheCompactPagerOnItsPage() =
        runAppComposeUiTest {
            val stack = NavBackStack<NavKey>(FileExplorerRoute.Uploaded, FileExplorerRoute.UploadRecord)
            val uploadStatus = mutableStateOf(UploadStatus.PAUSED)
            lateinit var inputMode: InputModeManager
            setContent {
                inputMode = LocalInputModeManager.current
                FileNavigationFixture(stack, 390.dp, uploadStatus = uploadStatus)
            }

            val action = onNodeWithTag("upload-status-action", useUnmergedTree = true)
            val viewport = onNodeWithTag("file-navigator").getUnclippedBoundsInRoot()
            val actionBounds = action.getUnclippedBoundsInRoot()
            assertTrue((actionBounds.left + actionBounds.right) / 2 > (viewport.left + viewport.right) / 2)
            // Compose key injection dispatches directly to the view on Android. Enter keyboard mode first.
            runOnIdle { assertTrue(inputMode.requestInputMode(InputMode.Keyboard)) }
            action.performSemanticsAction(SemanticsActions.RequestFocus) { requestFocus ->
                assertEquals(true, requestFocus())
            }
            action.assertIsFocused()
            onNodeWithTag("page-/upload-record").assertIsDisplayed()
            assertPageFillsNavigator("/upload-record")

            action.performClick()
            runOnIdle { assertEquals(UploadStatus.UPLOADING, uploadStatus.value) }
            assertPageFillsNavigator("/upload-record")
            runOnIdle { assertEquals(FileExplorerRoute.UploadRecord, stack.last()) }
        }

    @Test
    fun compactFilePagerSettlesAfterPartialAccessibilityScroll() =
        runAppComposeUiTest {
            val stack = NavBackStack<NavKey>(FileExplorerRoute.Uploaded, FileExplorerRoute.UploadRecord)
            setContent { FileNavigationFixture(stack, 390.dp) }

            val scrollNodes = onAllNodes(hasScrollAction()).fetchSemanticsNodes()
            val horizontalPagerIndices =
                scrollNodes.mapIndexedNotNull { index, node ->
                    val horizontalRange = node.config.getOrNull(SemanticsProperties.HorizontalScrollAxisRange)
                    index.takeIf { horizontalRange?.maxValue?.invoke()?.let { max -> max > 0f } == true }
                }
            assertEquals(1, horizontalPagerIndices.size, "Expected one horizontally scrollable file pager")
            onAllNodes(hasScrollAction())[horizontalPagerIndices.single()].performSemanticsAction(
                SemanticsActions.ScrollBy,
            ) { scrollBy ->
                assertTrue(scrollBy(133.2f, 0f))
            }

            runOnIdle { assertEquals(FileExplorerRoute.UploadRecord, stack.last()) }
            onNodeWithTag("page-/upload-record").assertIsDisplayed()
            assertPageFillsNavigator("/upload-record")
        }
}

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.assertPageFillsNavigator(route: String) {
    val viewport = onNodeWithTag("file-navigator").getUnclippedBoundsInRoot()
    val page = onNodeWithTag("page-$route").getUnclippedBoundsInRoot()
    assertEquals(viewport.left, page.left, "Selected file page should begin at the navigator's left edge")
    assertEquals(viewport.right, page.right, "Selected file page should end at the navigator's right edge")
}

@Composable
private fun FileNavigationFixture(
    backStack: NavBackStack<NavKey>,
    width: Dp,
    allowDownloads: Boolean = true,
    uploadStatus: MutableState<UploadStatus>? = null,
) {
    MaterialTheme {
        Box {
            FileExplorerNavigation(
                backStack = backStack,
                routes =
                listOf(
                    NavRoute("/uploaded", navigationIcon(NavigationGlyph.FOLDER), "Uploaded"),
                    NavRoute("/upload-record", navigationIcon(NavigationGlyph.FOLDER), "Upload"),
                ) +
                    if (allowDownloads) {
                        listOf(
                            NavRoute("/download-record", navigationIcon(NavigationGlyph.FOLDER), "Download"),
                        )
                    } else {
                        emptyList()
                    },
                modifier = Modifier.requiredSize(width, 500.dp).testTag("file-navigator"),
                search = { Text("File search") },
            ) { route ->
                var draft by rememberSaveable { mutableStateOf("") }
                Column(Modifier.fillMaxSize().testTag("page-$route")) {
                    if (route == FileExplorerRoute.UploadRecord && uploadStatus != null) {
                        UploadStatusFixtureRow(uploadStatus)
                    }
                    BasicTextField(draft, { draft = it }, Modifier.testTag("draft-$route"))
                }
            }
        }
    }
}

@Composable
private fun UploadStatusFixtureRow(uploadStatus: MutableState<UploadStatus>) {
    val status = uploadStatus.value
    val upload =
        UploadInfo.EMPTY.copy(
            pathHash = "upload",
            path = "/synthetic/upload.bin",
            total = 64L * 1024 * 1024,
            status = status,
            name = "upload.bin",
            contentType = "application/octet-stream",
            chunkSize = 10L * 1024 * 1024,
        )
    UploadItemRow(
        uploadInfo = upload,
        statusLabel = status.name,
        onClick = {},
        onLongClick = {},
    ) {
        ReadingIconButton(
            onClick = {
                uploadStatus.value =
                    if (status == UploadStatus.PAUSED) {
                        UploadStatus.UPLOADING
                    } else {
                        UploadStatus.PAUSED
                    }
            },
            modifier = Modifier.testTag("upload-status-action"),
        ) {
            Icon(actionIcon(ActionGlyph.CIRCLE_PLAY), contentDescription = "Resume upload")
        }
    }
}
