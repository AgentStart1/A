/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.Image
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
internal class MediaPlayerSurfaceTest {
    private val originalLocale = Locale.getDefault()

    @BeforeTest
    fun setEnglishLocale() {
        Locale.setDefault(Locale.ENGLISH)
    }

    @AfterTest
    fun restoreLocale() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `playback button follows engine state`() =
        runComposeUiTest {
            val state = mutableStateOf(MediaControlState(position = 65.0, duration = 180.0))
            setContent {
                Box(Modifier.size(360.dp, 203.dp)) {
                    MediaPlayerSurface(
                        "Evening light",
                        state.value,
                        MediaControlActions(
                            { state.value = state.value.copy(playing = !state.value.playing) },
                            {},
                            {},
                            {},
                        ),
                        Modifier.fillMaxSize(),
                    ) {}
                }
            }
            onNodeWithText("1:05 / 3:00").assertIsDisplayed()
            onNodeWithContentDescription("Play").performClick()
            onNodeWithContentDescription("Pause").assertIsDisplayed().performClick()
            onNodeWithContentDescription("Play").assertIsDisplayed()
        }

    @Test
    fun `seeking commits the selected position`() =
        runComposeUiTest {
            var position = 0.0
            setContent {
                MediaPlayerSurface(
                    "Track",
                    MediaControlState(duration = 120.0),
                    MediaControlActions({}, { position = it }, {}, {}),
                    Modifier.size(360.dp, 203.dp),
                ) {}
            }
            onNodeWithContentDescription("Playback position").performSemanticsAction(SemanticsActions.SetProgress) {
                it(45f)
            }
            assertEquals(45.0, position)
        }

    @Test
    fun `expansion retains playback state`() =
        runComposeUiTest {
            setContent {
                MediaPlayerSurface(
                    "Track",
                    MediaControlState(playing = true, position = 65.0, duration = 180.0),
                    MediaControlActions({}, {}, {}, {}),
                    Modifier.size(360.dp, 203.dp),
                ) {}
            }
            onNodeWithContentDescription("Expand player").performClick()
            onNodeWithText("1:05 / 3:00").assertIsDisplayed()
            onNodeWithContentDescription("Pause").assertIsDisplayed()
            onNodeWithContentDescription("Exit expanded player").performClick()
            onNodeWithText("1:05 / 3:00").assertIsDisplayed()
        }

    @Test
    fun `compact video controls fit the surface`() =
        runComposeUiTest {
            setContent {
                MediaPlayerSurface(
                    "Evening light — a long title that should truncate",
                    MediaControlState(
                        position = 65.0,
                        duration = 180.0,
                    ),
                    MediaControlActions({}, {}, {}, {}),
                    Modifier.size(240.dp, 135.dp),
                ) {
                    Box(
                        Modifier.fillMaxSize().background(
                            Brush.linearGradient(listOf(Color(0xFF63788D), Color(0xFF263843))),
                        ),
                    )
                }
            }
            onNodeWithContentDescription("Play").assertIsDisplayed()
            onNodeWithContentDescription("Volume").assertIsDisplayed()
            onNodeWithContentDescription("Expand player").assertIsDisplayed()
            saveMediaImage("compact-video")
        }

    @Test
    fun `audio uses the shared transport`() =
        runComposeUiTest {
            setContent {
                MediaPlayerSurface(
                    "Evening light",
                    MediaControlState(position = 65.0, duration = 180.0),
                    MediaControlActions({}, {}, {}, {}),
                    Modifier.size(360.dp, 240.dp),
                    video = false,
                ) {}
            }
            onNodeWithText("Evening light").assertIsDisplayed()
            onNodeWithContentDescription("Play").assertIsDisplayed()
            onNodeWithText("1:05 / 3:00").assertIsDisplayed()
            saveMediaImage("audio")
        }

    @Test
    fun `pending playback disables controls that need an engine`() =
        runComposeUiTest {
            setContent {
                MediaPlayerSurface(
                    "Track",
                    MediaControlState(volumeEnabled = false, expansionEnabled = false),
                    MediaControlActions({}, {}, {}, {}),
                    Modifier.size(360.dp, 203.dp),
                ) {}
            }
            onNodeWithContentDescription("Play").assertIsEnabled()
            onNodeWithContentDescription("Volume").assertIsNotEnabled()
            onNodeWithContentDescription("Expand player").assertIsNotEnabled()
            onNodeWithContentDescription("Playback position").assertIsNotEnabled()
        }

    @Test
    fun `failed playback exposes the supplied retry action`() =
        runComposeUiTest {
            var retries = 0
            setContent {
                MediaPlayerSurface(
                    "Track",
                    MediaControlState(failed = true),
                    MediaControlActions({}, {}, {}, {}, retry = { retries++ }),
                    Modifier.size(360.dp, 203.dp),
                ) {}
            }
            onNodeWithText("Retry").performClick()
            assertEquals(1, retries)
            onNodeWithContentDescription("Play").assertIsNotEnabled()
        }

    private fun ComposeUiTest.saveMediaImage(name: String) {
        val directory = Path.of("build", "test-results", "jvmTest", "media-ui")
        Files.createDirectories(directory)
        Image.makeFromBitmap(onRoot().captureToImage().asSkiaBitmap()).use { image ->
            requireNotNull(image.encodeToData()).use { data ->
                Files.write(directory.resolve("$name.png"), data.bytes)
            }
        }
    }
}
