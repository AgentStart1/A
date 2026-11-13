/*
 * This is a private project. All rights reserved.
 */

package device_based

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.rememberNavigationEventDispatcherOwner
import com.storyteller_f.alphaheart.app.pages.file.FileExplorerNavigation
import com.storyteller_f.alphaheart.app.pages.file.FileExplorerRoute
import com.storyteller_f.alphaheart.app.pages.file.FilePreviewToolbar
import com.storyteller_f.alphaheart.app.pages.file.rememberFileExplorerBackStack
import com.storyteller_f.alphaheart.client.compose_core.components.NavRoute
import com.storyteller_f.shared.model.FileInfo
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class FileExplorerNavigationUiTest {
    @Test
    fun ordinaryFileDownloadOpensThePlatformBrowser() =
        runComposeUiTest {
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
                    MaterialTheme { FilePreviewToolbar(file) }
                }
            }
            onNodeWithText("Download").performClick()
            runOnIdle { assertEquals(listOf(file.url), opened) }
        }

    @Test
    fun historySelectionAndDraftSurviveResizing() =
        runComposeUiTest {
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
        runComposeUiTest {
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
        runComposeUiTest {
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
        runComposeUiTest {
            val stack = NavBackStack<NavKey>(FileExplorerRoute.Uploaded, FileExplorerRoute.UploadRecord)
            setContent { FileNavigationFixture(stack, 504.dp) }
            onNodeWithTag("file-navigator").performTouchInput {
                down(center)
                moveBy(Offset(-center.x * 0.8f, 0f), delayMillis = 100)
            }
            runOnIdle { stack.removeLastOrNull() }
            onNodeWithTag("file-navigator").performTouchInput { up() }
            onNodeWithTag("page-/uploaded").assertIsDisplayed()
            runOnIdle { assertEquals(listOf(FileExplorerRoute.Uploaded), stack.toList()) }
        }
}

@Composable
private fun FileNavigationFixture(backStack: NavBackStack<NavKey>, width: Dp) {
    MaterialTheme {
        FileExplorerNavigation(
            backStack = backStack,
            routes =
            listOf(
                NavRoute("/uploaded", Icons.Default.Folder, "Uploaded"),
                NavRoute("/upload-record", Icons.Default.Folder, "Upload"),
                NavRoute("/download-record", Icons.Default.Folder, "Download"),
            ),
            modifier = Modifier.requiredSize(width, 500.dp).testTag("file-navigator"),
            search = { Text("File search") },
        ) { route ->
            var draft by rememberSaveable { mutableStateOf("") }
            Column(Modifier.fillMaxSize().testTag("page-$route")) {
                BasicTextField(draft, { draft = it }, Modifier.testTag("draft-$route"))
            }
        }
    }
}
