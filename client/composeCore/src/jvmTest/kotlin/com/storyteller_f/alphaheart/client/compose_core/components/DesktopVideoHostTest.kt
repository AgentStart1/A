/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DesktopVideoHostTest {
    @Test
    fun `native frames own padded pixels`() {
        val pixels = byteArrayOf(1, 2, 3, 0, 4, 5, 6, 0)
        val buffer = ByteBuffer.wrap(pixels).apply { position(4) }
        val frame = DesktopVideoFrame.copyFrom(1, 1, 8, buffer)
        pixels.fill(0)
        val exported = frame.copyPixels()
        exported.fill(9)

        assertContentEquals(byteArrayOf(1, 2, 3, 0, 4, 5, 6, 0), frame.copyPixels())
        assertEquals(4, buffer.position())
        assertEquals(8, frame.rowBytes)
    }

    @Test
    fun `playback callbacks update controls`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val player = FakePlayer()
            val host = DesktopVideoHost("media", backgroundScope, dispatcher, dispatcher) { player }
            runCurrent()
            assertEquals("media", player.url)
            player.playback(DesktopPlayback.MediaReady)
            runCurrent()
            assertEquals(4f / 3, assertIs<DesktopVideoState.Ready>(host.state.value).aspectRatio)
            player.playback(DesktopPlayback.Buffering)
            runCurrent()
            assertTrue(assertIs<DesktopVideoState.Ready>(host.state.value).buffering)
            player.playback(DesktopPlayback.Buffered)
            runCurrent()
            assertFalse(assertIs<DesktopVideoState.Ready>(host.state.value).buffering)
            host.pause()
            runCurrent()
            assertFalse(assertIs<DesktopVideoState.Ready>(host.state.value).playing)
            assertEquals(1, player.pauses)
            host.play()
            runCurrent()
            assertTrue(assertIs<DesktopVideoState.Ready>(host.state.value).playing)
            assertEquals(1, player.plays)
            host.close()
            runCurrent()
            assertEquals(1, player.closes)
        }

    @Test
    fun `closing during open releases native player`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val player = FakePlayer()
            val host = DesktopVideoHost("media", backgroundScope, dispatcher, dispatcher) { player }
            player.duringOpen = { host.close() }
            runCurrent()

            assertEquals(1, player.closes)
            assertIs<DesktopVideoState.Loading>(host.state.value)
            player.playback(DesktopPlayback.Playing)
            runCurrent()
            assertIs<DesktopVideoState.Loading>(host.state.value)
        }

    @Test
    fun `failed open releases native player`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val player = FakePlayer().apply { duringOpen = { error("native open failed") } }
            val host = DesktopVideoHost("media", backgroundScope, dispatcher, dispatcher) { player }
            runCurrent()

            assertIs<DesktopVideoState.Failed>(host.state.value)
            assertEquals(1, player.closes)
            host.close()
        }

    @Test
    fun `seek commands update the displayed position`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val player = FakePlayer()
            val host = DesktopVideoHost("media", backgroundScope, dispatcher, dispatcher) { player }
            runCurrent()
            host.seek(42.0)
            runCurrent()
            advanceTimeBy(250)
            runCurrent()
            assertEquals(listOf(42.0), player.seeks)
            assertEquals(42.0, assertIs<DesktopVideoState.Ready>(host.state.value).position)
            host.close()
        }

    @Test
    fun `volume commands update the displayed volume`() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val player = FakePlayer()
            val host = DesktopVideoHost("media", backgroundScope, dispatcher, dispatcher) { player }
            runCurrent()
            host.setVolume(0.25f)
            runCurrent()
            advanceTimeBy(250)
            runCurrent()
            assertEquals(0.25f, assertIs<DesktopVideoState.Ready>(host.state.value).volume)
            host.close()
        }

    private class FakePlayer : DesktopVideoPlayer {
        val seeks = mutableListOf<Double>()
        private var currentPosition = 5.0
        private var currentVolume = 0.5f
        var url = ""
        var pauses = 0
        var plays = 0
        var closes = 0
        var duringOpen: () -> Unit = {}
        lateinit var playback: (DesktopPlayback) -> Unit
        override fun open(url: String, frame: (DesktopVideoFrame) -> Unit, playback: (DesktopPlayback) -> Unit) {
            this.url = url
            this.playback = playback
            duringOpen()
        }
        override fun aspectRatio(): Float = 4f / 3
        override fun play() {
            plays++
            playback(DesktopPlayback.Playing)
        }
        override fun pause() {
            pauses++
            playback(DesktopPlayback.Paused)
        }
        override fun position(): Double = currentPosition
        override fun duration(): Double = 60.0
        override fun volume(): Float = currentVolume
        override fun seek(seconds: Double) {
            seeks += seconds
            currentPosition = seconds
        }
        override fun setVolume(volume: Float) {
            currentVolume = volume
        }
        override fun close() {
            closes++
        }
    }
}
