/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo

private val desktopVideoCoordination = Dispatchers.Default.limitedParallelism(1)

@Composable
actual fun VideoViewEmbed(remoteMediaItem: RemoteMediaItem) {
    DesktopMedia(remoteMediaItem, Modifier.aspectRatio(16f / 9), video = true)
}

@Composable
actual fun VideoViewFullScreen(remoteMediaItem: RemoteMediaItem) {
    DesktopMedia(remoteMediaItem, Modifier.fillMaxSize(), video = true)
}

@Composable
actual fun VideoViewFilled(remoteMediaItem: RemoteMediaItem) {
    DesktopMedia(remoteMediaItem, Modifier.fillMaxSize(), video = true)
}

@Composable
internal fun DesktopMedia(item: RemoteMediaItem, modifier: Modifier, video: Boolean) {
    val scope = rememberCoroutineScope()
    var retryGeneration by remember(item.url, video) { mutableIntStateOf(0) }
    val autoPlay = video || retryGeneration > 0
    val host =
        remember(item.url, video, scope, retryGeneration) {
            DesktopVideoHost(item.url, scope, desktopVideoCoordination, Dispatchers.IO) {
                VlcMediaPlayer(autoPlay = autoPlay)
            }
        }
    DisposableEffect(host) { onDispose { host.close() } }
    val state by host.state.collectAsState()
    val ready = state as? DesktopVideoState.Ready
    val bitmap by rememberDesktopBitmap(ready?.frame)
    val volume =
        remember(host) {
            MediaVolumeControl { level, muted -> host.setVolume(if (muted) 0f else level) }
        }
    val controls = state.toControlState()
    val actions =
        MediaControlActions(
            { if (ready?.playing == true) host.pause() else host.play() },
            host::seek,
            volume::setVolume,
            { volume.toggleMute(controls.volume, controls.muted) },
            retry = { retryGeneration++ },
        )
    val surfaceModifier = if (video) modifier else modifier.fillMaxWidth().heightIn(min = 224.dp)
    key(item.url) {
        MediaPlayerSurface(
            item.title ?: item.name,
            controls,
            actions,
            surfaceModifier,
            video,
            sourceUrl = item.url,
            artwork = item.cover?.url,
        ) {
            bitmap?.let { image ->
                val ratio = ready?.aspectRatio ?: image.width.toFloat() / image.height
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Image(
                        image,
                        "video frame ${item.name}",
                        Modifier.aspectRatio(ratio),
                        contentScale = ContentScale.FillBounds,
                    )
                }
            }
        }
    }
}

@Composable
actual fun rememberIsInPipMode(): Boolean = false

private fun DesktopVideoState.toControlState(): MediaControlState {
    val ready = this as? DesktopVideoState.Ready
    return MediaControlState(
        playing = ready?.playing == true,
        loading = this == DesktopVideoState.Loading || ready?.buffering == true,
        failed = this == DesktopVideoState.Failed,
        enabled = ready != null,
        position = ready?.position ?: 0.0,
        duration = ready?.duration ?: 0.0,
        volume = ready?.volume ?: 1f,
        muted = ready?.volume == 0f,
    )
}

@Composable
private fun rememberDesktopBitmap(frame: DesktopVideoFrame?): State<ImageBitmap?> =
    produceState<ImageBitmap?>(null, frame) {
        value =
            frame?.let { frame ->
                withContext(Dispatchers.Default) {
                    org.jetbrains.skia.Image.makeRaster(
                        ImageInfo(frame.width, frame.height, ColorType.BGRA_8888, ColorAlphaType.OPAQUE),
                        frame.copyPixels(),
                        frame.rowBytes,
                    ).toComposeImageBitmap()
                }
            }
    }
