/*
 * This is a private project. All rights reserved.
 */

@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class)

package com.storyteller_f.alphaheart.app.rtc

import io.ktor.client.webrtc.MediaTrackFactory
import io.ktor.client.webrtc.WebRtcMedia

/** Converts browser failures into exceptions understood by the shared call lifecycle. */
internal class BrowserRtcMediaDevices(private val devices: MediaTrackFactory = BrowserTrackFactory) :
    MediaTrackFactory {
    override suspend fun createAudioTrack(constraints: WebRtcMedia.AudioTrackConstraints): WebRtcMedia.AudioTrack =
        acquireBrowserMedia { devices.createAudioTrack(constraints) }

    override suspend fun createVideoTrack(constraints: WebRtcMedia.VideoTrackConstraints): WebRtcMedia.VideoTrack =
        acquireBrowserMedia { devices.createVideoTrack(constraints) }
}

private suspend fun <T> acquireBrowserMedia(acquire: suspend () -> T): T =
    try {
        acquire()
    } catch (failure: JsException) {
        // JsException is a Throwable, not an Exception. DOMException can also lack Error.stack,
        // so retaining it as a cause makes Kotlin/Wasm stack formatting fail a second time.
        throw WebRtcMedia.DeviceException("Browser media request failed", BrowserCaptureFailure(failure))
    }

private class BrowserCaptureFailure(failure: JsException) : RuntimeException(failure.message)
