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
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.components.TopicContentView
import com.storyteller_f.alphaheart.app.pages.topic.RoomMessageLayout
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.model.TopicContent
import com.storyteller_f.shared.model.TopicInfo
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class RoomMessageLayoutUiTest {
    @Test
    fun incomingAndOutgoingKeepTimeAndActionsAtBothWidths() =
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
                            sentAt = LocalDateTime(2026, 1, 1, 0, 0),
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
                    val body = onNodeWithTag("body").getUnclippedBoundsInRoot()
                    val actions = onNodeWithTag("actions").getUnclippedBoundsInRoot()
                    val maxWidth = size * if (size < 600.dp) 0.95f else 0.88f
                    assertTrue(actions.top >= body.bottom + 12.dp)
                    if (outgoing) {
                        onNodeWithTag("avatar").assertDoesNotExist()
                        onNodeWithText("Sender").assertDoesNotExist()
                        assertEquals(pane.right - if (size < 600.dp) 12.dp else 18.dp, body.right)
                        assertTrue(pane.right - actions.left <= maxWidth)
                    } else {
                        val avatar = onNodeWithTag("avatar").getUnclippedBoundsInRoot()
                        val sender = onNodeWithText("Sender").getUnclippedBoundsInRoot()
                        assertTrue(sender.bottom < body.top)
                        assertEquals(pane.left, avatar.left)
                        assertTrue(avatar.right < body.left)
                        assertTrue(actions.right - avatar.left <= maxWidth)
                        assertTrue(body.right - avatar.left <= maxWidth)
                        onNodeWithTag("avatar").performClick()
                    }
                    onNodeWithTag("topic-relative-time").assertIsDisplayed()
                    onNodeWithTag("actions").performClick()
                }
            }
            runOnIdle { assertEquals(2 to 4, avatarClicks to actionClicks) }
        }

    @Test
    fun realMarkdownAndMediaStayWithinTheMessageWidth() =
        runAppComposeUiTest {
            val viewport = mutableStateOf(1000.dp)
            val own = mutableStateOf(false)
            val body = mutableStateOf(TopicContent.Plain("Hi"))
            val longText = "A longer message should wrap at the edge of the bubble. ".repeat(12)
            val code = "```kotlin\n" + "val a = 42\n".repeat(3) + "```"
            setContent {
                MaterialTheme {
                    Box(Modifier.requiredSize(viewport.value, 760.dp).testTag("pane")) {
                        RoomMessageLayout(
                            ownMessage = own.value,
                            avatar = { Box(Modifier.size(34.dp).testTag("avatar")) },
                            actions = { Box(Modifier.width(180.dp).height(30.dp).testTag("actions")) },
                        ) {
                            TopicContentView(
                                TopicInfo.EMPTY.copy(content = body.value),
                                onClick = {},
                                isEmbed = true,
                                refBlock = {},
                                fillWidth = false,
                            )
                        }
                    }
                }
            }
            for (width in listOf(1000.dp, 390.dp)) {
                for (outgoing in listOf(false, true)) {
                    runOnIdle {
                        viewport.value = width
                        own.value = outgoing
                        body.value = TopicContent.Plain("Hi")
                    }
                    onAllNodesWithText("Hi").onLast().assertIsDisplayed()
                    val short = onNodeWithTag("room-message-bubble").getUnclippedBoundsInRoot()
                    val actions = onNodeWithTag("actions").getUnclippedBoundsInRoot()
                    assertTrue(short.right - short.left < actions.right - actions.left, "Short bubble must wrap")
                    runOnIdle { body.value = TopicContent.Plain(longText) }
                    onAllNodesWithText(longText.trim(), substring = true).onLast().assertIsDisplayed()
                    val long = onNodeWithTag("room-message-bubble").getUnclippedBoundsInRoot()
                    assertTrue(long.right - long.left > short.right - short.left)
                    assertTrue(long.bottom - long.top > short.bottom - short.top)
                    assertBubbleGeometry(width, outgoing)
                    runOnIdle { body.value = TopicContent.Plain(code) }
                    val codeNodes = onAllNodesWithText("val a = 42", substring = true, useUnmergedTree = true)
                    waitUntil(timeoutMillis = 5_000) { codeNodes.fetchSemanticsNodes().isNotEmpty() }
                    codeNodes.onLast().assertIsDisplayed()
                    assertBubbleGeometry(width, outgoing)
                    for ((name, mime) in listOf("sample.wav" to "audio/wav", "sample.mp4" to "video/mp4")) {
                        val file = FileInfo.EMPTY.copy(name = name, contentType = mime, url = "http://127.0.0.1/$name")
                        runOnIdle {
                            val source = "```object\n" + """{"name":"$name"}""" + "\n```"
                            body.value = TopicContent.Plain(source, listOf(file))
                        }
                        onNodeWithContentDescription("Playback position").assertIsDisplayed()
                        assertBubbleGeometry(width, outgoing)
                    }
                }
            }
        }

    private fun ComposeUiTest.assertBubbleGeometry(width: Dp, outgoing: Boolean) {
        val pane = onNodeWithTag("pane").getUnclippedBoundsInRoot()
        val bubble = onNodeWithTag("room-message-bubble").getUnclippedBoundsInRoot()
        val actions = onNodeWithTag("actions").getUnclippedBoundsInRoot()
        val maxWidth = width * if (width < 600.dp) 0.95f else 0.88f
        assertTrue(bubble.right - bubble.left <= maxWidth + 1.dp)
        if (outgoing) {
            assertEquals(pane.right, bubble.right)
            assertEquals(bubble.right, actions.right)
        } else {
            val avatar = onNodeWithTag("avatar").getUnclippedBoundsInRoot()
            assertTrue(avatar.right < bubble.left)
            assertEquals(bubble.left, actions.left)
            assertTrue(bubble.right - pane.left <= maxWidth + 1.dp)
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
