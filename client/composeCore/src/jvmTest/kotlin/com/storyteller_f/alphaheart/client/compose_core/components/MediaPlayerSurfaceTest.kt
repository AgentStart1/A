/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import org.jetbrains.skia.Image
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
    fun `file preview scope hides download while preserving other media actions`() =
        runComposeUiTest {
            val showDownload = mutableStateOf(false)
            var previews = 0
            setContent {
                CompositionLocalProvider(LocalMediaPreviewAction provides { previews++ }) {
                    MediaMenuScope(showDownload = showDownload.value) {
                        MediaPlayerSurface(
                            "Track",
                            MediaControlState(),
                            MediaControlActions({}, {}, {}, {}),
                            Modifier.size(360.dp, 203.dp),
                            sourceUrl = "https://media.invalid/track.flac",
                        ) {}
                    }
                }
            }
            onNodeWithContentDescription("Media options").performClick()
            onNodeWithContentDescription("Download").assertDoesNotExist()
            onNodeWithContentDescription("Copy").assertIsDisplayed()
            onNodeWithContentDescription("Preview").assertIsDisplayed()
            runOnIdle { showDownload.value = true }
            onNodeWithContentDescription("Download").assertIsDisplayed()
            onNodeWithContentDescription("Preview").performClick()
            assertEquals(1, previews)
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
    fun `pointer click and drag seek from an ended position`() =
        runComposeUiTest {
            val state = mutableStateOf(MediaControlState(position = 3.0, duration = 3.0))
            val seeks = mutableListOf<Double>()
            var thumbCenterInset = 0f
            setContent {
                val density = LocalDensity.current
                SideEffect {
                    // Material's semantic range adds 10dp per side beyond the real track;
                    // the custom 10dp thumb's center is another 5dp inside that track.
                    thumbCenterInset = with(density) { 15.dp.toPx() }
                }
                MediaPlayerSurface(
                    "Track",
                    state.value,
                    MediaControlActions({}, {
                        seeks.add(it)
                        state.value = state.value.copy(position = it)
                    }, {}, {}),
                    Modifier.size(360.dp, 203.dp),
                ) {}
            }
            val slider = onNodeWithContentDescription("Playback position")
            slider.performTouchInput { click(center) }
            runOnIdle {
                assertEquals(1, seeks.size)
                assertTrue(seeks.single() in 1.3..1.7)
                state.value = state.value.copy(position = 3.0)
            }
            val before = slider.fetchSemanticsNode()
            assertEquals(3f, before.config[SemanticsProperties.ProgressBarRangeInfo].current)
            slider.performTouchInput {
                down(centerRight - Offset(thumbCenterInset, 0f))
                moveTo(Offset(center.x / 2, center.y))
            }
            val during = slider.fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current
            slider.performTouchInput { up() }
            runOnIdle {
                assertEquals(
                    2,
                    seeks.size,
                    "Drag bounds=${before.boundsInRoot}, intermediate=$during, callbacks=$seeks",
                )
                assertTrue(seeks.last() in 0.1..1.4)
                assertEquals(seeks.last(), state.value.position)
            }
        }

    @Test
    fun `focused slider commits a keyboard seek from an ended position`() =
        runComposeUiTest {
            val state = mutableStateOf(MediaControlState(position = 3.0, duration = 3.0))
            val seeks = mutableListOf<Double>()
            setContent {
                MediaPlayerSurface(
                    "Track",
                    state.value,
                    MediaControlActions({}, {
                        seeks.add(it)
                        state.value = state.value.copy(position = it)
                    }, {}, {}),
                    Modifier.size(360.dp, 203.dp),
                ) {}
            }
            val slider = onNodeWithContentDescription("Playback position")
            slider.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
            slider.performKeyInput { pressKey(Key.DirectionLeft) }
            runOnIdle {
                assertEquals(1, seeks.size)
                assertTrue(seeks.single() in 0.0..<3.0)
                assertEquals(seeks.single(), state.value.position)
            }
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

    @Test
    fun `compact audio expansion keeps following content in place at both widths`() =
        runComposeUiTest {
            val width = mutableStateOf(360.dp)
            var followingTop = Float.NaN
            setContent {
                ReadingInteractions {
                    Column(Modifier.width(width.value)) {
                        MediaPlayerSurface(
                            "Track",
                            MediaControlState(position = 65.0, duration = 180.0),
                            MediaControlActions({}, {}, {}, {}),
                            Modifier.fillMaxWidth(),
                            video = false,
                        ) {}
                        Text(
                            "Following content",
                            Modifier.onGloballyPositioned { followingTop = it.positionInRoot().y },
                        )
                    }
                }
            }
            listOf(360.dp, 800.dp).forEach { size ->
                runOnIdle { width.value = size }
                val before = runOnIdle { followingTop }
                onNodeWithContentDescription("Expand player").performClick()
                onNodeWithContentDescription("Exit expanded player").assertIsDisplayed()
                runOnIdle { assertEquals(before, followingTop, "Expansion moved the following content at $size") }
                onNodeWithContentDescription("Exit expanded player").performClick()
                onNodeWithText("Following content").assertIsDisplayed()
                runOnIdle {
                    assertEquals(
                        before,
                        followingTop,
                        "Closing expansion moved the following content at $size",
                    )
                }
            }
        }

    @Test
    fun `compact audio pending and failed states keep visible actions and stable geometry`() =
        runComposeUiTest {
            val width = mutableStateOf(360.dp)
            val state = mutableStateOf(MediaControlState(duration = 120.0))
            var followingTop = Float.NaN
            var previewCount = 0
            var retryCount = 0
            setContent {
                CompositionLocalProvider(LocalMediaPreviewAction provides { previewCount++ }) {
                    ReadingInteractions {
                        Column(Modifier.width(width.value)) {
                            MediaPlayerSurface(
                                "Track",
                                state.value,
                                MediaControlActions({}, {}, {}, {}, retry = { retryCount++ }),
                                Modifier.fillMaxWidth(),
                                video = false,
                                sourceUrl = "https://media.invalid/track.flac",
                            ) {}
                            Text(
                                "Following content",
                                Modifier.onGloballyPositioned { followingTop = it.positionInRoot().y },
                            )
                        }
                    }
                }
            }
            listOf(360.dp, 800.dp).forEach { size ->
                runOnIdle {
                    width.value = size
                    state.value = MediaControlState(duration = 120.0)
                }
                val before = runOnIdle { followingTop }
                val menuPixels = onNodeWithContentDescription("Media options").capturePixels()
                val expandPixels = onNodeWithContentDescription("Expand player").capturePixels()
                listOf(state.value.copy(loading = true), state.value.copy(failed = true)).forEach { next ->
                    runOnIdle { state.value = next }
                    onNodeWithContentDescription("Media options").assertIsDisplayed().assertHasClickAction()
                    onNodeWithContentDescription("Expand player").assertIsDisplayed().assertHasClickAction()
                    assertContentEquals(menuPixels, onNodeWithContentDescription("Media options").capturePixels())
                    assertContentEquals(expandPixels, onNodeWithContentDescription("Expand player").capturePixels())
                    runOnIdle { assertEquals(before, followingTop, "Status moved the following content at $size") }
                    onNodeWithContentDescription("Media options").performClick()
                    onNodeWithContentDescription("Preview").performClick()
                }
                onNodeWithContentDescription("Expand player").performClick()
                onNodeWithContentDescription("Exit expanded player").assertIsDisplayed().performClick()
                onNodeWithContentDescription("Retry").performClick()
            }
            runOnIdle {
                assertEquals(4, previewCount)
                assertEquals(2, retryCount)
            }
        }

    @Test
    fun `compact audio respects hidden transport controls at both widths`() =
        runComposeUiTest {
            val width = mutableStateOf(360.dp)
            setContent {
                ReadingInteractions {
                    MediaPlayerSurface(
                        "Track",
                        MediaControlState(duration = 120.0),
                        MediaControlActions({}, {}, {}, {}),
                        Modifier.width(width.value),
                        video = false,
                        controlsVisible = false,
                        sourceUrl = "https://media.invalid/track.flac",
                    ) {}
                }
            }
            listOf(360.dp, 800.dp).forEach { size ->
                runOnIdle { width.value = size }
                onNodeWithText("Track").assertIsDisplayed()
                listOf("Play", "Playback position", "Volume", "Expand player", "Media options").forEach { label ->
                    onNodeWithContentDescription(label).assertDoesNotExist()
                }
            }
        }

    private fun SemanticsNodeInteraction.capturePixels(): IntArray {
        val image = captureToImage()
        return IntArray(image.width * image.height).also { image.readPixels(it) }
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
