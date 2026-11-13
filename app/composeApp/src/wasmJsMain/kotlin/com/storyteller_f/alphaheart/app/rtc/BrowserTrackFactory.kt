/*
 * This is a private project. All rights reserved.
 */

@file:OptIn(io.ktor.utils.io.ExperimentalKtorApi::class, kotlin.js.ExperimentalWasmJsInterop::class)

package com.storyteller_f.alphaheart.app.rtc

import io.ktor.client.webrtc.JsAudioTrack
import io.ktor.client.webrtc.JsVideoTrack
import io.ktor.client.webrtc.MediaTrackFactory
import io.ktor.client.webrtc.WebRtcMedia
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import web.mediadevices.getUserMedia
import web.mediastreams.MediaStream
import web.mediastreams.MediaStreamConstraints
import web.mediastreams.MediaStreamTrack
import web.navigator.navigator
import kotlin.js.toArray

/** Owns every captured track until exactly one requested track is delivered to Ktor. */
internal object BrowserTrackFactory : MediaTrackFactory {
    override suspend fun createAudioTrack(constraints: WebRtcMedia.AudioTrackConstraints): WebRtcMedia.AudioTrack {
        val json =
            buildJsonObject {
                constraints.volume?.let { put("volume", it) }
                constraints.latency?.let { put("latency", it) }
                constraints.sampleRate?.let { put("sampleRate", it) }
                constraints.sampleSize?.let { put("sampleSize", it) }
                constraints.channelCount?.let { put("channelCount", it) }
                constraints.autoGainControl?.let { put("autoGainControl", it) }
                constraints.echoCancellation?.let { put("echoCancellation", it) }
                constraints.noiseSuppression?.let { put("noiseSuppression", it) }
            }
        return captureTrack(browserStreamConstraints(json.toString(), audio = true), {
            it.getAudioTracks().toArray().firstOrNull()
        }, ::JsAudioTrack)
    }

    override suspend fun createVideoTrack(constraints: WebRtcMedia.VideoTrackConstraints): WebRtcMedia.VideoTrack {
        val json =
            buildJsonObject {
                constraints.width?.let { put("width", it) }
                constraints.height?.let { put("height", it) }
                constraints.frameRate?.let { put("frameRate", it) }
                constraints.aspectRatio?.let { put("aspectRatio", it) }
                constraints.facingMode?.let { put("facingMode", it.name.lowercase()) }
                constraints.resizeMode?.let { put("resizeMode", it.name.lowercase().replace('_', '-')) }
            }
        return captureTrack(browserStreamConstraints(json.toString(), audio = false), {
            it.getVideoTracks().toArray().firstOrNull()
        }, ::JsVideoTrack)
    }
}

private suspend fun <T : WebRtcMedia.Track> captureTrack(
    constraints: MediaStreamConstraints,
    select: (MediaStream) -> MediaStreamTrack?,
    wrap: (MediaStreamTrack) -> T,
): T {
    val caller = currentCoroutineContext()
    var stream: MediaStream? = null
    var delivered = false
    try {
        val acquired = withContext(NonCancellable) { navigator.mediaDevices.getUserMedia(constraints) }
        stream = acquired
        val selected = select(acquired) ?: throw WebRtcMedia.DeviceException("Requested capture track is unavailable")
        acquired.getTracks().toArray().filter { it.id != selected.id }.forEach { it.stop() }
        val track = wrap(selected)
        caller.ensureActive()
        delivered = true
        return track
    } catch (failure: Throwable) {
        caller.ensureActive()
        throw failure
    } finally {
        if (!delivered) stream?.getTracks()?.toArray()?.forEach { it.stop() }
    }
}

@JsFun("(json, audio) => ({audio: audio ? JSON.parse(json) : false, video: audio ? false : JSON.parse(json)})")
private external fun browserStreamConstraints(json: String, audio: Boolean): MediaStreamConstraints
