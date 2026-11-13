/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.rtc

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import dev.onvoid.webrtc.media.video.VideoFrame
import dev.onvoid.webrtc.media.video.VideoTrackSink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo

@Composable
internal actual fun RtcJoinButton(video: Boolean, enabled: Boolean, onJoin: () -> Unit) {
    DefaultRtcJoinButton(video, enabled, onJoin)
}

@Composable
internal actual fun RtcVideoView(video: RtcVideo, modifier: Modifier, local: Boolean) {
    val track = video as DesktopRtcVideo
    val frames =
        remember(track) {
            flow {
                val incoming = Channel<VideoFrame>(Channel.CONFLATED, onUndeliveredElement = { it.release() })
                val sink =
                    VideoTrackSink { frame ->
                        frame.retain()
                        if (incoming.trySend(frame).isFailure) frame.release()
                    }
                if (!track.attach(sink)) {
                    incoming.cancel()
                    return@flow
                }
                try {
                    for (frame in incoming) {
                        try {
                            emit(convertRtcFrame(frame))
                        } finally {
                            frame.release()
                        }
                    }
                } finally {
                    withContext(NonCancellable) { track.detach(sink) }
                    incoming.cancel()
                }
            }.flowOn(Dispatchers.Default)
        }
    val frame by frames.collectAsState(null)
    frame?.let { (image, rotation) ->
        Image(
            image,
            contentDescription = null,
            modifier =
            modifier.graphicsLayer {
                rotationZ = rotation.toFloat()
                scaleX = if (local) -1f else 1f
            },
            contentScale = ContentScale.Fit,
        )
    }
}

/** Converts on a worker and balances the native I420 reference even if rendering is cancelled. */
internal fun convertRtcFrame(frame: VideoFrame): Pair<androidx.compose.ui.graphics.ImageBitmap, Int> {
    val buffer = frame.buffer.toI420()
    try {
        val pixels = ByteArray(buffer.width * buffer.height * 4)
        val yPlane = buffer.dataY
        val uPlane = buffer.dataU
        val vPlane = buffer.dataV
        for (y in 0 until buffer.height) {
            for (x in 0 until buffer.width) {
                val luminance = (yPlane[y * buffer.strideY + x].toInt() and 255) - 16
                val u = (uPlane[y / 2 * buffer.strideU + x / 2].toInt() and 255) - 128
                val v = (vPlane[y / 2 * buffer.strideV + x / 2].toInt() and 255) - 128
                val offset = (y * buffer.width + x) * 4
                pixels[offset] = (298 * luminance + 516 * u + 128 shr 8).coerceIn(0, 255).toByte()
                pixels[offset + 1] = (298 * luminance - 100 * u - 208 * v + 128 shr 8).coerceIn(0, 255).toByte()
                pixels[offset + 2] = (298 * luminance + 409 * v + 128 shr 8).coerceIn(0, 255).toByte()
                pixels[offset + 3] = 255.toByte()
            }
        }
        return Image.makeRaster(
            ImageInfo(buffer.width, buffer.height, ColorType.BGRA_8888, ColorAlphaType.OPAQUE),
            pixels,
            buffer.width * 4,
        ).toComposeImageBitmap() to frame.rotation
    } finally {
        buffer.release()
    }
}
