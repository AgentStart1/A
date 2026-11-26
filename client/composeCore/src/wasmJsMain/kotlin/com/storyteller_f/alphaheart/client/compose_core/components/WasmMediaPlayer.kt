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
import com.storyteller_f.alphaheart.client.compose_core.media.BrowserMediaHost
import com.storyteller_f.alphaheart.client.compose_core.media.MediaControlActions
import com.storyteller_f.alphaheart.client.compose_core.media.MediaSnapshot
import com.storyteller_f.alphaheart.client.compose_core.media.MediaSurfaceLayout
import com.storyteller_f.alphaheart.client.compose_core.media.MediaVolumeControl
import com.storyteller_f.alphaheart.client.compose_core.media.PixelFrame
import com.storyteller_f.alphaheart.client.compose_core.media.RemoteMediaItem
import com.storyteller_f.alphaheart.client.compose_core.media.WasmBitmapOwner
import com.storyteller_f.alphaheart.client.compose_core.media.WasmTextureRenderer
import com.storyteller_f.alphaheart.client.compose_core.media.createHost
import com.storyteller_f.alphaheart.client.compose_core.media.profileMediaFrames
import com.storyteller_f.alphaheart.client.compose_core.media.recordPixelStage
import com.storyteller_f.alphaheart.client.compose_core.media.toControlState
import com.storyteller_f.alphaheart.client.compose_core.media.updateBitmapFrame
import com.storyteller_f.alphaheart.client.compose_core.media.useGpuTextures
import org.jetbrains.skia.Bitmap
import kotlin.js.ExperimentalWasmJsInterop

@Composable
internal fun WasmMediaPlayer(
    item: RemoteMediaItem,
    modifier: Modifier,
    video: Boolean,
    layout: MediaSurfaceLayout = MediaSurfaceLayout.Bounded,
    loadOnPlay: Boolean = false,
) {
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
        if (!loadOnPlay) host.start()
        onDispose {
            host.close()
            bitmapOwner.close()
            textureRenderer?.close()
        }
    }
    SideEffect { bitmapOwner.retain(bitmap) }
    val actions = rememberBrowserMediaActions(host, loadOnPlay) { state }
    val surfaceModifier =
        if (video) {
            modifier
        } else {
            modifier.fillMaxWidth().heightIn(
                min = if (item.cover == null) 64.dp else 224.dp,
            )
        }
    key(item.url) {
        MediaPlayerSurface(
            item.title ?: item.name,
            state.toControlState(loadOnPlay),
            actions,
            surfaceModifier,
            video,
            sourceUrl = item.url,
            artwork = item.cover?.url,
            layout = layout,
        ) {
            WasmMediaFrame(bitmap, textureRenderer, host)
        }
    }
}

@Composable
private fun rememberBrowserMediaActions(
    host: BrowserMediaHost,
    loadOnPlay: Boolean,
    readSnapshot: () -> MediaSnapshot?,
): MediaControlActions {
    val snapshot = rememberUpdatedState(readSnapshot)
    return remember(host, loadOnPlay) {
        val volume = MediaVolumeControl { level, muted -> host.setVolume(level.toDouble(), muted) }
        val togglePlayback: () -> Unit = {
            if (loadOnPlay) host.togglePlaybackOnDemand() else host.togglePlayback()
        }
        MediaControlActions(
            togglePlayback,
            { host.seek(it) },
            volume::setVolume,
            {
                val state = snapshot.value()
                volume.toggleMute(state?.volume?.toFloat() ?: 1f, state?.muted ?: false)
            },
            retry = togglePlayback.takeIf { loadOnPlay },
        )
    }
}

@Composable
private fun WasmMediaFrame(frame: Bitmap?, textureRenderer: WasmTextureRenderer?, host: BrowserMediaHost) {
    if (textureRenderer != null) {
        Canvas(Modifier.fillMaxSize()) {
            textureRenderer.draw(drawContext.canvas.skiaCanvas, size) { host.failRendering() }
        }
    } else if (frame != null) {
        val composeImage = remember(frame) { frame.asComposeImageBitmap() }
        Image(composeImage, null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
    }
}
