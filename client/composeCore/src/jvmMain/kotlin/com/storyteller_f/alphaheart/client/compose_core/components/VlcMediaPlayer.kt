/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import io.github.aakira.napier.Napier
import uk.co.caprica.vlcj.factory.MediaPlayerFactory
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.media.VideoOrientation
import uk.co.caprica.vlcj.player.base.MediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat
import java.nio.ByteBuffer

/** Callback surfaces expose pixels without creating an AWT component. */
internal class VlcMediaPlayer(private val autoPlay: Boolean) : DesktopVideoPlayer {
    private val factory =
        run {
            NativeDiscovery().discover()
            MediaPlayerFactory()
        }
    private val player =
        try {
            factory.mediaPlayers().newEmbeddedMediaPlayer()
        } catch (failure: Throwable) {
            factory.release()
            throw failure
        }

    override fun open(url: String, frame: (DesktopVideoFrame) -> Unit, playback: (DesktopPlayback) -> Unit) {
        Napier.i("Opening desktop media")
        val format =
            object : BufferFormatCallback {
                override fun getBufferFormat(width: Int, height: Int): BufferFormat = RV32BufferFormat(width, height)
                override fun allocatedBuffers(buffers: Array<out ByteBuffer>) = Unit
                override fun newFormatSize(width: Int, height: Int, displayWidth: Int, displayHeight: Int) = Unit
            }
        val render =
            object : RenderCallback {
                override fun lock(mediaPlayer: MediaPlayer) = Unit
                override fun unlock(mediaPlayer: MediaPlayer) = Unit
                override fun display(
                    mediaPlayer: MediaPlayer,
                    buffers: Array<out ByteBuffer>,
                    format: BufferFormat,
                    displayWidth: Int,
                    displayHeight: Int,
                ) {
                    frame(DesktopVideoFrame.copyFrom(format.width, format.height, format.pitches[0], buffers[0]))
                }
            }
        player.videoSurface().set(factory.videoSurfaces().newVideoSurface(format, render, true))
        player.events().addMediaPlayerEventListener(
            object : MediaPlayerEventAdapter() {
                override fun mediaPlayerReady(mediaPlayer: MediaPlayer) {
                    playback(DesktopPlayback.MediaReady)
                }
                override fun playing(mediaPlayer: MediaPlayer) {
                    Napier.i("Desktop media playback started")
                    playback(DesktopPlayback.Playing)
                }
                override fun paused(mediaPlayer: MediaPlayer) {
                    playback(DesktopPlayback.Paused)
                }
                override fun finished(mediaPlayer: MediaPlayer) {
                    playback(DesktopPlayback.Finished)
                }
                override fun buffering(mediaPlayer: MediaPlayer, newCache: Float) {
                    playback(if (newCache < 100f) DesktopPlayback.Buffering else DesktopPlayback.Buffered)
                }
                override fun error(mediaPlayer: MediaPlayer) {
                    playback(DesktopPlayback.Failed)
                }
            },
        )
        val opened = if (autoPlay) player.media().play(url) else player.media().prepare(url)
        check(opened) { "VLC could not open the requested media" }
    }

    override fun aspectRatio(): Float? {
        val track = player.media().info().videoTracks().firstOrNull() ?: return null
        if (track.width() <= 0 || track.height() <= 0) return null
        val sampleRatio =
            if (track.sampleAspectRatio() > 0 && track.sampleAspectRatioBase() > 0) {
                track.sampleAspectRatio().toFloat() / track.sampleAspectRatioBase()
            } else {
                1f
            }
        val ratio = track.width().toFloat() / track.height() * sampleRatio
        return when (track.orientation()) {
            VideoOrientation.LEFT_TOP, VideoOrientation.LEFT_BOTTOM,
            VideoOrientation.RIGHT_TOP, VideoOrientation.RIGHT_BOTTOM,
            -> 1f / ratio

            else -> ratio
        }
    }

    override fun play() {
        player.controls().play()
    }
    override fun pause() {
        player.controls().pause()
    }
    override fun position(): Double = player.status().time().coerceAtLeast(0) / 1000.0
    override fun duration(): Double = player.status().length().coerceAtLeast(0) / 1000.0
    override fun volume(): Float = player.audio().volume().coerceIn(0, 100) / 100f
    override fun seek(seconds: Double) {
        if (player.status().isSeekable) player.controls().setTime((seconds * 1000).toLong())
    }
    override fun setVolume(volume: Float) {
        player.audio().setVolume((volume.coerceIn(0f, 1f) * 100).toInt())
    }
    override fun close() {
        try {
            player.release()
        } finally {
            factory.release()
        }
    }
}
