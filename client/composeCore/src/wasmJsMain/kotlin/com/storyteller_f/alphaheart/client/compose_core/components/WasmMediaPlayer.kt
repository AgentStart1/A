/*
 * This is a private project. All rights reserved.
 */

@file:OptIn(ExperimentalWasmJsInterop::class)

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.graphics.skiaCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
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
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.Promise

@JsModule("wasm-media-host")
private external fun createHost(
    url: String,
    onState: (MediaSnapshot) -> Unit,
    onFrame: (PixelFrame) -> Boolean,
    autoStart: Boolean,
    path: String,
): BrowserMediaHost

private external interface BrowserMediaHost : JsAny {
    fun start(): Promise<JsAny?>
    fun togglePlayback(): Promise<JsAny?>
    fun seek(time: Double): Promise<JsAny?>
    fun setVolume(volume: Double, muted: Boolean)
    fun failRendering()
    fun close(): Promise<JsAny?>
}

private external interface MediaSnapshot : JsAny {
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

@Composable
internal fun WasmMediaPlayer(item: RemoteMediaItem, modifier: Modifier, video: Boolean) {
    val gpu = remember { useGpuTextures() }
    val profiling = remember { profileMediaFrames() }
    val textureRenderer = remember(item.url, gpu) { if (gpu && video) WasmTextureRenderer() else null }
    var state by remember(item.url) { mutableStateOf<MediaSnapshot?>(null) }
    var pixels by remember(item.url) { mutableStateOf<PixelFrame?>(null) }
    val bitmapOwner = remember(item.url) { WasmBitmapOwner() }
    val bitmap = bitmapOwner.bitmap
    val host =
        remember(item.url) {
            createHost(item.url, { state = it }, { frame ->
                if (textureRenderer != null) {
                    textureRenderer.accept(frame)
                } else {
                    if (profiling) recordPixelStage("received", frame.timestamp)
                    pixels = frame
                    true
                }
            }, autoStart = false, path = if (textureRenderer != null) "gpu-texture" else "rgba-copy")
        }
    LaunchedEffect(pixels) {
        host.updateBitmapFrame(pixels, profiling, bitmapOwner::accept)
    }
    DisposableEffect(host) {
        textureRenderer?.setFailureHandler { host.failRendering() }
        host.start()
        onDispose {
            host.close()
            bitmapOwner.close()
            textureRenderer?.close()
        }
    }
    SideEffect { bitmapOwner.retain(bitmap) }
    val actions = rememberBrowserMediaActions(host) { state }
    val surfaceModifier = if (video) modifier else modifier.fillMaxWidth().heightIn(min = 224.dp)
    key(item.url) {
        MediaPlayerSurface(
            item.title ?: item.name,
            state.toControlState(),
            actions,
            surfaceModifier,
            video,
            sourceUrl = item.url,
            artwork = item.cover?.url,
        ) {
            WasmMediaFrame(item, bitmap, textureRenderer, host)
        }
    }
}

@Composable
private fun rememberBrowserMediaActions(
    host: BrowserMediaHost,
    readSnapshot: () -> MediaSnapshot?,
): MediaControlActions {
    val snapshot = rememberUpdatedState(readSnapshot)
    return remember(host) {
        val volume = MediaVolumeControl { level, muted -> host.setVolume(level.toDouble(), muted) }
        MediaControlActions(
            { host.togglePlayback() },
            { host.seek(it) },
            volume::setVolume,
            {
                val state = snapshot.value()
                volume.toggleMute(state?.volume?.toFloat() ?: 1f, state?.muted ?: false)
            },
        )
    }
}

@Composable
private fun WasmMediaFrame(
    item: RemoteMediaItem,
    frame: Bitmap?,
    textureRenderer: WasmTextureRenderer?,
    host: BrowserMediaHost,
) {
    if (textureRenderer != null) {
        Canvas(Modifier.fillMaxSize()) {
            textureRenderer.draw(drawContext.canvas.skiaCanvas, size) { host.failRendering() }
        }
    } else if (frame != null) {
        val composeImage = remember(frame) { frame.asComposeImageBitmap() }
        Image(composeImage, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
    } else {
        AsyncImage(item.cover?.url, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
    }
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

private fun useGpuTextures(): Boolean =
    js(
        "new URLSearchParams(globalThis.location.search).get('mediaPath') === 'gpu-texture'",
    )

private suspend fun BrowserMediaHost.updateBitmapFrame(
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
private external fun recordPixelStage(stage: String, timestamp: Double)

private fun profileMediaFrames(): Boolean =
    js("new URLSearchParams(globalThis.location.search).get('mediaProfile') === 'true'")

private fun MediaSnapshot?.toControlState(): MediaControlState {
    val status = this?.status ?: "loading"
    return MediaControlState(
        playing = status in listOf("playing", "buffering"),
        loading = status in listOf("loading", "buffering"),
        failed = !this?.error.isNullOrEmpty(),
        enabled = status in listOf("playing", "buffering", "paused", "ended"),
        position = this?.position ?: 0.0,
        duration = this?.duration ?: 0.0,
        volume = this?.volume?.toFloat() ?: 1f,
        muted = this?.muted == true || this?.volume == 0.0,
    )
}
