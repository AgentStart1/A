/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.client.compose_core.media.LocalMediaPreviewAction
import com.storyteller_f.alphaheart.client.compose_core.media.MediaControlActions
import com.storyteller_f.alphaheart.client.compose_core.media.MediaControlState
import com.storyteller_f.alphaheart.client.compose_core.media.MediaSurfaceLayout
import com.storyteller_f.alphaheart.client.compose_core.utils.readingColorScheme
import java.util.Locale
import kotlin.math.abs
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
internal class MediaHeaderGeometryTest {
    private val previousLocale = Locale.getDefault()

    @BeforeTest
    fun useEnglish() {
        Locale.setDefault(Locale.ENGLISH)
    }

    @AfterTest
    fun restoreLocale() {
        Locale.setDefault(previousLocale)
    }

    @Test
    fun `paper heading owns readable options and expanded close without a duplicate footer exit`() =
        runComposeUiTest {
            val state = mutableStateOf(MediaControlState(playing = true, position = 65.0, duration = 180.0))
            setContent {
                MaterialTheme(colorScheme = readingColorScheme(false)) {
                    ReadingInteractions {
                        MediaPlayerSurface(
                            "White video",
                            state.value,
                            MediaControlActions({}, {}, {}, {}),
                            Modifier.size(360.dp, 260.dp),
                            sourceUrl = "https://media.invalid/video.mp4",
                        ) {
                            Box(Modifier.fillMaxSize().background(Color.White).testTag("video-pixels"))
                        }
                    }
                }
            }
            val header = onNodeWithTag("media-player-heading").getUnclippedBoundsInRoot()
            val video = onNodeWithTag("video-pixels").getUnclippedBoundsInRoot()
            val options = onNodeWithContentDescription("Media options").getUnclippedBoundsInRoot()
            assertTrue(options.top >= header.top && options.bottom <= header.bottom)
            assertTrue(video.top >= header.bottom)
            val image = onNodeWithContentDescription("Media options").captureToImage().toPixelMap()
            assertTrue((0 until image.width).any { x -> (0 until image.height).any { y -> image[x, y].red < .5f } })
            onNodeWithContentDescription("Expand player").performClick()
            onAllNodesWithContentDescription("Exit expanded player").assertCountEquals(1)
            onNodeWithContentDescription("Expand player").assertDoesNotExist()
            val expandedHeader = onNodeWithTag("media-player-heading").getUnclippedBoundsInRoot()
            val close = onNodeWithContentDescription("Exit expanded player").getUnclippedBoundsInRoot()
            assertTrue(close.top >= expandedHeader.top && close.bottom <= expandedHeader.bottom)
            val shell = onNodeWithTag("media-player-shell").getUnclippedBoundsInRoot()
            val frame = onNodeWithTag("media-player-frame").getUnclippedBoundsInRoot()
            assertEquals(shell.left + 20.dp, frame.left)
            assertEquals(shell.right - 20.dp, frame.right)
            assertEquals(shell.top + 20.dp, expandedHeader.top)
            assertEquals(expandedHeader.bottom + 18.dp, frame.top)
            assertEquals(frame.bottom + 20.dp, shell.bottom)
            onNodeWithText("White video").assertIsDisplayed()
            onNodeWithContentDescription("Pause").assertIsDisplayed()
            onNodeWithText("1:05 / 3:00").assertIsDisplayed()
            onNodeWithContentDescription("Exit expanded player").performClick()
            onNodeWithContentDescription("Expand player").assertIsDisplayed()
            onNodeWithContentDescription("Pause").assertIsDisplayed()
            onNodeWithText("1:05 / 3:00").assertIsDisplayed()
        }

    @Test
    fun `file page heading is not repeated while player options and expansion keep their actions`() =
        runComposeUiTest {
            var previews = 0
            setContent {
                MaterialTheme(colorScheme = readingColorScheme(false)) {
                    CompositionLocalProvider(LocalMediaPreviewAction provides { previews++ }) {
                        ReadingInteractions {
                            Column {
                                Text("File video")
                                MediaMenuScope(showDownload = false, showHeading = false) {
                                    MediaPlayerSurface(
                                        "File video",
                                        MediaControlState(playing = true, position = 65.0, duration = 180.0),
                                        MediaControlActions({}, {}, {}, {}),
                                        Modifier.size(360.dp, 260.dp),
                                        sourceUrl = "https://media.invalid/video.mp4",
                                    ) { Box(Modifier.fillMaxSize()) }
                                }
                            }
                        }
                    }
                }
            }
            onAllNodesWithText("File video").assertCountEquals(1)
            onNodeWithTag("media-player-heading").assertDoesNotExist()
            onNodeWithContentDescription("Media options").performClick()
            onNodeWithContentDescription("Download").assertDoesNotExist()
            onNodeWithContentDescription("Preview").performClick()
            runOnIdle { assertEquals(1, previews) }
            onNodeWithContentDescription("Expand player").performClick()
            onNodeWithTag("media-player-heading").assertExists()
            onNodeWithContentDescription("Pause").assertIsDisplayed()
            onNodeWithText("1:05 / 3:00").assertIsDisplayed()
            onNodeWithContentDescription("Exit expanded player").performClick()
            onAllNodesWithText("File video").assertCountEquals(1)
            onNodeWithContentDescription("Media options").assertIsDisplayed()
        }

    @Test
    fun `embedded video reserves its picture ratio inside a lazy chat bubble without moving its anchor`() =
        runComposeUiTest {
            val width = mutableStateOf(232.dp)
            val state = mutableStateOf(MediaControlState(playing = true, position = 65.0, duration = 180.0))
            setContent {
                MaterialTheme(colorScheme = readingColorScheme(false)) {
                    ReadingInteractions {
                        LazyColumn(Modifier.width(width.value).height(600.dp)) {
                            item("video") {
                                MediaPlayerSurface(
                                    "Bubble video",
                                    state.value,
                                    MediaControlActions({}, {}, {}, {}, retry = {}),
                                    Modifier.fillMaxWidth(),
                                    layout = MediaSurfaceLayout.EmbeddedVideo(),
                                ) { Box(Modifier.fillMaxSize()) }
                            }
                            item("anchor") { Text("Next message", Modifier.testTag("next-message")) }
                        }
                    }
                }
            }
            for (size in listOf(232.dp, 330.dp)) {
                runOnIdle { width.value = size }
                val stage = onNodeWithTag("media-player-stage").getUnclippedBoundsInRoot()
                val header = onNodeWithTag("media-player-heading").getUnclippedBoundsInRoot()
                val shell = onNodeWithTag("media-player-shell").getUnclippedBoundsInRoot()
                val anchor = onNodeWithTag("next-message").getUnclippedBoundsInRoot()
                assertEquals(size, stage.right - stage.left)
                assertTrue(abs((stage.bottom - stage.top).value - size.value * 9f / 16f) <= 1f)
                assertTrue(stage.top >= header.bottom)
                assertTrue(shell.bottom > stage.bottom)
                assertEquals(shell.bottom, anchor.top)
                for (next in listOf(state.value.copy(loading = true), state.value.copy(failed = true))) {
                    runOnIdle { state.value = next }
                    assertEquals(stage, onNodeWithTag("media-player-stage").getUnclippedBoundsInRoot())
                    assertEquals(anchor, onNodeWithTag("next-message").getUnclippedBoundsInRoot())
                }
                runOnIdle { state.value = state.value.copy(loading = false, failed = false) }
                onNodeWithContentDescription("Expand player").performClick()
                assertEquals(anchor, onNodeWithTag("next-message").getUnclippedBoundsInRoot())
                onNodeWithContentDescription("Pause").assertIsDisplayed()
                onNodeWithText("1:05 / 3:00").assertIsDisplayed()
                onNodeWithContentDescription("Exit expanded player").performClick()
                assertEquals(stage, onNodeWithTag("media-player-stage").getUnclippedBoundsInRoot())
                assertEquals(anchor, onNodeWithTag("next-message").getUnclippedBoundsInRoot())
            }
        }
}
