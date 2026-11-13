/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.unit.dp
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.CompletableDeferred
import org.jetbrains.skia.Image
import java.net.InetSocketAddress
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale
import kotlin.math.abs
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class NativeMediaPlaybackTest {
    private val fixture = Path.of("../../dev/appiumCore/src/main/resources/media/sample.mp4").toAbsolutePath()
    private val output = Path.of("build/test-results/nativeMediaTest/media").toAbsolutePath()

    @Test
    fun `real VLC frames and progress survive expansion`() =
        withEnglish {
            runComposeUiTest {
                setContent { DesktopMedia(item(fixture.toUri().toString()), Modifier.size(320.dp, 180.dp), true) }
                waitForNode(hasContentDescription("video frame sample.mp4"))
                waitForProgress(2)
                save("playing")
                onNodeWithContentDescription("Pause").performClick()
                onNodeWithContentDescription("Play").assertIsDisplayed()
                onNodeWithContentDescription(
                    "Playback position",
                ).performSemanticsAction(SemanticsActions.SetProgress) { it(12f) }
                waitUntil(timeoutMillis = 15_000) { position() >= 11 }
                onNodeWithContentDescription("Play").performClick()
                val before = position()
                onNodeWithContentDescription("Expand player").performClick()
                waitForProgress(before)
                onNodeWithContentDescription("video frame sample.mp4").assertIsDisplayed()
                save("expanded")
                val expanded = position()
                onNodeWithContentDescription("Exit expanded player").performClick()
                waitForProgress(expanded)
                save("restored")
            }
        }

    @Test
    fun `real VLC network error recovers after user retry`() = checkNetworkRetry(true)

    @Test
    fun `real VLC audio network error resumes after user retry`() = checkNetworkRetry(false)

    private fun checkNetworkRetry(video: Boolean) =
        withEnglish {
            Files.createDirectories(output)
            val source = if (video) fixture else fixture.resolveSibling("sample.wav")
            val prefix = if (video) "network" else "audio-network"
            val available = CompletableDeferred<Unit>()
            val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
            server.createContext("/") { exchange ->
                exchange.use {
                    if (!available.isCompleted) {
                        it.sendResponseHeaders(503, -1)
                    } else {
                        it.responseHeaders.add("Content-Type", if (video) "video/mp4" else "audio/wav")
                        it.sendResponseHeaders(200, Files.size(source))
                        Files.newInputStream(source).buffered().use { input ->
                            it.responseBody.buffered().use { output -> input.copyTo(output) }
                        }
                    }
                }
            }
            try {
                server.start()
                runComposeUiTest {
                    setContent {
                        DesktopMedia(
                            item("http://127.0.0.1:${server.address.port}/${source.fileName}", video),
                            Modifier.size(480.dp, 300.dp),
                            video,
                        )
                    }
                    if (!video) {
                        waitForNode(isEnabled() and hasContentDescription("Play"))
                        onNodeWithContentDescription("Play").performClick()
                    }
                    waitForNode(hasText("Retry"))
                    save("$prefix-error")
                    available.complete(Unit)
                    onNodeWithText("Retry").performClick()
                    if (video) {
                        waitForNode(hasContentDescription("video frame sample.mp4"))
                    }
                    waitForProgress(1)
                    onNodeWithContentDescription("Pause").assertIsDisplayed()
                    save("$prefix-recovered")
                }
            } finally {
                server.stop(0)
            }
        }

    @Test
    fun `real VLC audio seeks and replays after ending`() =
        withEnglish {
            runComposeUiTest {
                val audio = fixture.resolveSibling("sample.wav")
                setContent {
                    DesktopMedia(
                        RemoteMediaItem("audio", audio.toUri().toString(), "audio/wav", "sample.wav"),
                        Modifier.size(480.dp, 300.dp),
                        false,
                    )
                }
                waitForNode(isEnabled() and hasContentDescription("Play"))
                onNodeWithContentDescription("Play").assertIsDisplayed().performClick()
                waitForProgress(1)
                onNodeWithContentDescription("Pause").performClick()
                onNodeWithContentDescription(
                    "Playback position",
                ).performSemanticsAction(SemanticsActions.SetProgress) { it(58f) }
                waitUntil(timeoutMillis = 15_000) { position() >= 57 }
                onNodeWithContentDescription("Play").performClick()
                waitUntil(timeoutMillis = 15_000) {
                    val ended = position() >= 59
                    ended && onAllNodesWithContentDescription("Play").fetchSemanticsNodes().isNotEmpty()
                }
                save("audio-ended")
                onNodeWithContentDescription("Play").performClick()
                waitUntil(timeoutMillis = 15_000) { position() in 1..10 }
                onNodeWithContentDescription("Pause").assertIsDisplayed()
                save("audio-replayed")
            }
        }

    @Test
    fun `real VLC volume restores after mute`() =
        withEnglish {
            runComposeUiTest {
                val audio = fixture.resolveSibling("sample.wav")
                setContent {
                    DesktopMedia(
                        RemoteMediaItem("audio", audio.toUri().toString(), "audio/wav", "sample.wav"),
                        Modifier.size(480.dp, 300.dp),
                        false,
                    )
                }
                waitForNode(isEnabled() and hasContentDescription("Play"))
                onNodeWithContentDescription("Play").performClick()
                waitForProgress(1)
                onNodeWithContentDescription("Volume").performClick()
                val slider =
                    hasContentDescription("Volume") and SemanticsMatcher.keyIsDefined(SemanticsActions.SetProgress)
                onNode(slider).performSemanticsAction(SemanticsActions.SetProgress) { it(0.25f) }
                assertVolume(slider, 0.25f)
                onNodeWithText("Mute").performClick()
                onNodeWithContentDescription("Volume").performClick()
                waitForNode(hasText("Unmute"))
                onNodeWithText("Unmute").assertIsDisplayed().performClick()
                onNodeWithContentDescription("Volume").performClick()
                assertVolume(slider, 0.25f)
                save("audio-volume-restored")
            }
        }

    private fun ComposeUiTest.waitForNode(matcher: SemanticsMatcher) {
        waitUntil(timeoutMillis = 15_000) { onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun ComposeUiTest.assertVolume(slider: SemanticsMatcher, expected: Float) {
        waitUntil(timeoutMillis = 15_000) {
            val current = onNode(slider).fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current
            abs(current - expected) < 0.02f
        }
    }

    private fun item(url: String, video: Boolean = true) =
        RemoteMediaItem(
            "fixture",
            url,
            if (video) "video/mp4" else "audio/wav",
            if (video) "sample.mp4" else "sample.wav",
        )
    private fun ComposeUiTest.position(): Int {
        val nodes = onAllNodes(hasText(" / ", substring = true)).fetchSemanticsNodes()
        val value = nodes.single().config[SemanticsProperties.Text].single().text
        return value.substringBefore(" / ").split(':').map(String::toInt).let { it[0] * 60 + it[1] }
    }
    private fun ComposeUiTest.waitForProgress(previous: Int) =
        waitUntil(
            timeoutMillis = 15_000,
        ) { position() > previous }
    private fun ComposeUiTest.save(name: String) {
        Files.createDirectories(output)
        Image.makeFromBitmap(onAllNodes(isRoot()).onLast().captureToImage().asSkiaBitmap()).use { image ->
            image.encodeToData()!!.use { Files.write(output.resolve("$name.png"), it.bytes) }
        }
    }
    private fun withEnglish(block: () -> Unit) {
        val original = Locale.getDefault()
        Locale.setDefault(Locale.ENGLISH)
        try {
            block()
        } finally {
            Locale.setDefault(original)
        }
    }
}
