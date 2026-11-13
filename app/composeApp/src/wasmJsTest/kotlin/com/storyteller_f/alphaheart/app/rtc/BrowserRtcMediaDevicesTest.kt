/*
 * This is a private project. All rights reserved.
 */

@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class, kotlin.js.ExperimentalWasmJsInterop::class)

package com.storyteller_f.alphaheart.app.rtc

import io.ktor.client.webrtc.MediaTrackFactory
import io.ktor.client.webrtc.WebRtcMedia
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.js.JsAny
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class BrowserRtcMediaDevicesTest {
    @Test
    fun missingAudioDeviceBecomesReportable() =
        runTest {
            val devices = BrowserRtcMediaDevices(FailingMediaDevices { throwMissingDevice() })
            val failure = assertFailsWith<WebRtcMedia.DeviceException> { devices.createAudioTrack() }
            assertTrue(failure.cause?.message.orEmpty().contains("No capture device"))
            assertTrue(failure.stackTraceToString().contains("DeviceException"))
        }

    @Test
    fun missingVideoDeviceBecomesReportable() =
        runTest {
            val devices = BrowserRtcMediaDevices(FailingMediaDevices { throwMissingDevice() })
            val failure = assertFailsWith<WebRtcMedia.DeviceException> { devices.createVideoTrack() }
            assertTrue(failure.cause?.message.orEmpty().contains("No capture device"))
            assertTrue(failure.stackTraceToString().contains("DeviceException"))
        }

    @Test
    fun navigatorAudioConstraintsAreLinked() =
        runTest {
            val original = rejectBrowserCapture()
            try {
                val constraints = WebRtcMedia.AudioTrackConstraints().apply { echoCancellation = true }
                val failure =
                    assertFailsWith<WebRtcMedia.DeviceException> {
                        BrowserRtcMediaDevices().createAudioTrack(constraints)
                    }
                assertTrue(failure.cause?.message.orEmpty().contains("Synthetic capture rejection"))
            } finally {
                restoreBrowserCapture(original)
            }
        }

    @Test
    fun navigatorVideoConstraintsAreLinked() =
        runTest {
            val original = rejectBrowserCapture()
            try {
                val constraints =
                    WebRtcMedia.VideoTrackConstraints().apply {
                        width = 640
                        height = 480
                    }
                val failure =
                    assertFailsWith<WebRtcMedia.DeviceException> {
                        BrowserRtcMediaDevices().createVideoTrack(constraints)
                    }
                assertTrue(failure.cause?.message.orEmpty().contains("Synthetic capture rejection"))
            } finally {
                restoreBrowserCapture(original)
            }
        }

    @Test
    fun availableTracksKeepTheirIdentity() =
        runTest {
            val audio =
                object : WebRtcMedia.AudioTrack {
                    override val id = "audio"
                    override val kind = WebRtcMedia.TrackType.AUDIO
                    override val enabled = true
                    override fun enable(enabled: Boolean) = Unit
                    override fun close() = Unit
                }
            val video =
                object : WebRtcMedia.VideoTrack {
                    override val id = "video"
                    override val kind = WebRtcMedia.TrackType.VIDEO
                    override val enabled = true
                    override fun enable(enabled: Boolean) = Unit
                    override fun close() = Unit
                }
            val devices =
                BrowserRtcMediaDevices(
                    object : MediaTrackFactory {
                        override suspend fun createAudioTrack(constraints: WebRtcMedia.AudioTrackConstraints) = audio
                        override suspend fun createVideoTrack(constraints: WebRtcMedia.VideoTrackConstraints) = video
                    },
                )
            assertSame(audio, devices.createAudioTrack())
            assertSame(video, devices.createVideoTrack())
        }

    @Test
    fun cancellationRemainsCancellation() =
        runTest {
            val cancelled = CancellationException("Capture cancelled")
            val devices = BrowserRtcMediaDevices(FailingMediaDevices { throw cancelled })
            assertSame(cancelled, assertFailsWith<CancellationException> { devices.createAudioTrack() })
            assertSame(cancelled, assertFailsWith<CancellationException> { devices.createVideoTrack() })
        }
}

private class FailingMediaDevices(private val fail: () -> Unit) : MediaTrackFactory {
    override suspend fun createAudioTrack(constraints: WebRtcMedia.AudioTrackConstraints): WebRtcMedia.AudioTrack {
        fail()
        error("Expected capture failure")
    }

    override suspend fun createVideoTrack(constraints: WebRtcMedia.VideoTrackConstraints): WebRtcMedia.VideoTrack {
        fail()
        error("Expected capture failure")
    }
}

@JsFun("() => { throw new DOMException('No capture device', 'NotFoundError'); }")
private external fun throwMissingDevice(): JsAny

@JsFun(
    """() => {
        const original = navigator.mediaDevices.getUserMedia;
        navigator.mediaDevices.getUserMedia = () => Promise.reject(
            new DOMException('Synthetic capture rejection', 'NotFoundError'));
        return original;
    }""",
)
private external fun rejectBrowserCapture(): JsAny

@JsFun("(original) => { navigator.mediaDevices.getUserMedia = original; }")
private external fun restoreBrowserCapture(original: JsAny)
