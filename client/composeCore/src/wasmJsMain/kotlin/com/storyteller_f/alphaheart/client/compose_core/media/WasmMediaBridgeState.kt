/*
 * This is a private project. All rights reserved.
 */

@file:OptIn(ExperimentalWasmJsInterop::class)

package com.storyteller_f.alphaheart.client.compose_core.media

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.webext.installPixelsFromArrayBuffer
import org.jetbrains.skiko.ExperimentalSkikoApi
import org.khronos.webgl.ArrayBuffer
import kotlin.js.JsAny
import kotlin.js.Promise

@JsModule("wasm-media-host")
internal external fun createHost(
    url: String,
    onState: (MediaSnapshot) -> Unit,
    onFrame: (PixelFrame) -> Boolean,
    autoStart: Boolean,
    path: String,
): BrowserMediaHost

internal external interface BrowserMediaHost : JsAny {
    fun start(): Promise<JsAny?>
    fun togglePlayback(): Promise<JsAny?>
    fun togglePlaybackOnDemand(): Promise<JsAny?>
    fun seek(time: Double): Promise<JsAny?>
    fun setVolume(volume: Double, muted: Boolean)
    fun failRendering()
    fun close(): Promise<JsAny?>
}

internal external interface MediaSnapshot : JsAny {
    val status: String
    val position: Double
    val duration: Double
    val volume: Double
    val muted: Boolean
    val error: String
}

internal external interface PixelFrame : JsAny {
    val generation: Int
    val timestamp: Double
    val pixels: ArrayBuffer
    val width: Int
    val height: Int
}

@OptIn(ExperimentalSkikoApi::class)
private suspend fun PixelFrame.toSkiaBitmap(): Bitmap {
    val info = ImageInfo(width, height, ColorType.RGBA_8888, ColorAlphaType.UNPREMUL, ColorSpace.sRGB)
    val bitmap = Bitmap()
    return try {
        check(bitmap.installPixelsFromArrayBuffer(info, pixels, info.minRowBytes)) { "Cannot install media pixels" }
        bitmap.setImmutable()
    } catch (failure: Throwable) {
        bitmap.close()
        throw failure
    }
}

internal fun useGpuTextures(): Boolean =
    js(
        "new URLSearchParams(globalThis.location.search).get('mediaPath') === 'gpu-texture'",
    )

internal suspend fun BrowserMediaHost.updateBitmapFrame(
    frame: PixelFrame?,
    profiling: Boolean,
    publish: (Bitmap?) -> Unit,
) {
    try {
        installMediaBitmap(frame, profiling, publish)
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Throwable) {
        currentCoroutineContext().ensureActive()
        failRendering()
    }
}

private suspend fun installMediaBitmap(frame: PixelFrame?, profiling: Boolean, publish: (Bitmap?) -> Unit) {
    if (frame == null) {
        publish(null)
        return
    }
    if (profiling) recordPixelStage("begin", frame.timestamp)
    val decoded = withContext(NonCancellable) { frame.toSkiaBitmap() }
    try {
        currentCoroutineContext().ensureActive()
        publish(decoded)
        if (profiling) recordPixelStage("published", frame.timestamp)
    } catch (cancelled: CancellationException) {
        decoded.close()
        if (profiling) recordPixelStage("cancelled", frame.timestamp)
        throw cancelled
    }
}

@JsFun("(stage, timestamp) => { performance.mark('media.rgba.' + stage, {detail:{timestamp}}); }")
internal external fun recordPixelStage(stage: String, timestamp: Double)

internal fun profileMediaFrames(): Boolean =
    js("new URLSearchParams(globalThis.location.search).get('mediaProfile') === 'true'")

internal fun MediaSnapshot?.toControlState(loadOnPlay: Boolean): MediaControlState {
    val status = this?.status ?: if (loadOnPlay) "idle" else "loading"
    val duration = this?.duration ?: 0.0
    val startingOnDemand = loadOnPlay && status == "starting"
    return MediaControlState(
        playing = status in listOf("playing", "buffering"),
        loading = status in listOf("loading", "buffering") || startingOnDemand,
        failed = !this?.error.isNullOrEmpty(),
        enabled = status in listOf("idle", "playing", "buffering", "paused", "ended"),
        volumeEnabled = status != "idle",
        durationKnown = duration > 0.0,
        ended = status == "ended",
        position = this?.position ?: 0.0,
        duration = duration,
        volume = this?.volume?.toFloat() ?: 1f,
        muted = this?.muted == true || this?.volume == 0.0,
    )
}
