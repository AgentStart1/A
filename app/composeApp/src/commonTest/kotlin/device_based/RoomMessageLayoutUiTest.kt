/*
 * This is a private project. All rights reserved.
 */

package device_based

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.pages.topic.RoomMessageLayout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class RoomMessageLayoutUiTest {
    @Test
    fun incomingAndOutgoingKeepIdentityBesideBubbleAndActionsBelow() =
        runAppComposeUiTest {
            val width = mutableStateOf(1000.dp)
            val own = mutableStateOf(false)
            var avatarClicks = 0
            var actionClicks = 0
            setContent {
                MaterialTheme {
                    Box(Modifier.requiredSize(width.value, 500.dp).testTag("pane")) {
                        RoomMessageLayout(
                            ownMessage = own.value,
                            senderName = "Sender",
                            avatar = {
                                Box(Modifier.size(34.dp).testTag("avatar").clickable { avatarClicks++ })
                            },
                            actions = {
                                Text(
                                    "Reply",
                                    Modifier.width(140.dp).height(32.dp).testTag("actions")
                                        .clickable { actionClicks++ },
                                )
                            },
                        ) {
                            Text("Message", Modifier.fillMaxWidth().height(40.dp).testTag("body"))
                        }
                    }
                }
            }
            for (size in listOf(1000.dp, 390.dp)) {
                runOnIdle { width.value = size }
                for (outgoing in listOf(false, true)) {
                    runOnIdle { own.value = outgoing }
                    val pane = onNodeWithTag("pane").getUnclippedBoundsInRoot()
                    val avatar = onNodeWithTag("avatar").getUnclippedBoundsInRoot()
                    val body = onNodeWithTag("body").getUnclippedBoundsInRoot()
                    val actions = onNodeWithTag("actions").getUnclippedBoundsInRoot()
                    val sender = onNodeWithText("Sender").getUnclippedBoundsInRoot()
                    val maxWidth = size * if (size < 600.dp) 0.95f else 0.88f
                    assertTrue(sender.bottom < body.top)
                    assertTrue(actions.top >= body.bottom + 12.dp)
                    if (outgoing) {
                        assertEquals(pane.right, avatar.right)
                        assertTrue(avatar.left > body.right)
                        assertTrue(avatar.right - actions.left <= maxWidth)
                        assertTrue(avatar.right - body.left <= maxWidth)
                    } else {
                        assertEquals(pane.left, avatar.left)
                        assertTrue(avatar.right < body.left)
                        assertTrue(actions.right - avatar.left <= maxWidth)
                        assertTrue(body.right - avatar.left <= maxWidth)
                    }
                    onNodeWithTag("avatar").performClick()
                    onNodeWithTag("actions").performClick()
                }
            }
            runOnIdle {
                assertEquals(4, avatarClicks)
                assertEquals(4, actionClicks)
            }
        }

    @Test
    fun mediaStateAndIdentityAnchorSurviveResizeAndRepeatedExpansion() =
        runAppComposeUiTest {
            val width = mutableStateOf(1000.dp)
            var mounted = 0
            var disposed = 0
            setContent {
                MaterialTheme {
                    Box(Modifier.requiredSize(width.value, 600.dp)) {
                        RoomMessageLayout(
                            ownMessage = false,
                            senderName = "Sender",
                            avatar = { Box(Modifier.size(34.dp).testTag("avatar")) },
                            actions = { Text("Reply", Modifier.testTag("actions")) },
                        ) {
                            MediaStateFixture({ mounted++ }, { disposed++ })
                        }
                    }
                }
            }
            val anchor = onNodeWithTag("avatar").getUnclippedBoundsInRoot().top
            val collapsed = onNodeWithTag("media").getUnclippedBoundsInRoot().let { it.bottom - it.top }
            onNodeWithText("Expand media").performClick()
            val expanded = onNodeWithTag("media").getUnclippedBoundsInRoot().let { it.bottom - it.top }
            assertTrue(expanded > collapsed)
            runOnIdle { width.value = 390.dp }
            assertEquals(expanded, onNodeWithTag("media").getUnclippedBoundsInRoot().let { it.bottom - it.top })
            assertEquals(anchor, onNodeWithTag("avatar").getUnclippedBoundsInRoot().top)
            onNodeWithText("Collapse media").performClick()
            assertEquals(collapsed, onNodeWithTag("media").getUnclippedBoundsInRoot().let { it.bottom - it.top })
            onNodeWithText("Expand media").performClick()
            assertEquals(expanded, onNodeWithTag("media").getUnclippedBoundsInRoot().let { it.bottom - it.top })
            onNodeWithTag("actions").assertIsDisplayed()
            runOnIdle {
                assertEquals(1, mounted)
                assertEquals(0, disposed)
            }
        }

    @Test
    fun currentRoomTopicUsesBubbleWithoutDuplicatingHeaderIdentity() =
        runAppComposeUiTest {
            setContent {
                MaterialTheme {
                    Box(Modifier.requiredSize(390.dp, 400.dp)) {
                        RoomMessageLayout(
                            ownMessage = true,
                            senderName = "Header author",
                            showIdentity = false,
                            avatar = { Box(Modifier.size(34.dp).testTag("avatar")) },
                        ) {
                            Text("Current message", Modifier.testTag("body"))
                        }
                    }
                }
            }
            onNodeWithTag("body").assertIsDisplayed()
            onNodeWithTag("avatar").assertDoesNotExist()
            onNodeWithText("Header author").assertDoesNotExist()
            assertTrue(onNodeWithTag("body").getUnclippedBoundsInRoot().left > 0.dp)
        }
}

@Composable
private fun MediaStateFixture(onMount: () -> Unit, onUnmount: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        onMount()
        onDispose { onUnmount() }
    }
    Column {
        Box(Modifier.width(260.dp).height(if (expanded) 180.dp else 40.dp).testTag("media"))
        Text(if (expanded) "Collapse media" else "Expand media", Modifier.clickable { expanded = !expanded })
    }
}
