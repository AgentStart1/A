/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

/** Owns a copy of VLC's borrowed, opaque BGRA frame, including row padding. */
internal class DesktopVideoFrame private constructor(
    val width: Int,
    val height: Int,
    val rowBytes: Int,
    private val pixels: ByteArray,
) {
    fun copyPixels(): ByteArray = pixels.copyOf()

    companion object {
        fun copyFrom(width: Int, height: Int, rowBytes: Int, buffer: ByteBuffer): DesktopVideoFrame {
            require(width > 0 && height > 0 && rowBytes >= Math.multiplyExact(width, 4))
            val bytes = ByteArray(Math.multiplyExact(rowBytes, height))
            buffer.duplicate().apply { rewind() }.get(bytes)
            return DesktopVideoFrame(width, height, rowBytes, bytes)
        }
    }
}

internal sealed interface DesktopVideoState {
    data object Loading : DesktopVideoState
    data class Ready(
        val playing: Boolean = false,
        val buffering: Boolean = false,
        val frame: DesktopVideoFrame? = null,
        val aspectRatio: Float? = null,
        val position: Double = 0.0,
        val duration: Double = 0.0,
        val volume: Float = 1f,
    ) : DesktopVideoState
    data object Failed : DesktopVideoState
}

internal enum class DesktopPlayback { MediaReady, Playing, Paused, Buffering, Buffered, Finished, Failed }

/** Methods run on the worker dispatcher; callbacks may arrive on VLC threads. */
internal interface DesktopVideoPlayer : AutoCloseable {
    fun open(url: String, frame: (DesktopVideoFrame) -> Unit, playback: (DesktopPlayback) -> Unit)
    fun aspectRatio(): Float?
    fun play()
    fun pause()
    fun position(): Double
    fun duration(): Double
    fun volume(): Float
    fun seek(seconds: Double)
    fun setVolume(volume: Float)
}

/** Owns native lifetime without UI objects. State is confined to the injected serial dispatcher. */
internal class DesktopVideoHost(
    url: String,
    parentScope: CoroutineScope,
    coordination: CoroutineDispatcher,
    worker: CoroutineDispatcher,
    createPlayer: () -> DesktopVideoPlayer,
) {
    private val scope =
        CoroutineScope(
            parentScope.coroutineContext + SupervisorJob(parentScope.coroutineContext[Job]) + coordination,
        )
    private val frames = Channel<DesktopVideoFrame>(Channel.CONFLATED)
    private val events = Channel<Event>(Channel.UNLIMITED)
    private val mutableState = MutableStateFlow<DesktopVideoState>(DesktopVideoState.Loading)
    val state = mutableState.asStateFlow()

    init {
        scope.launch {
            try {
                // Ownership stays inside the worker context even if creation/open is cancelled.
                withContext(worker) {
                    createPlayer().use { player ->
                        player.open(url, { frames.trySend(it) }, { events.trySend(Event.Playback(it)) })
                        withContext(coordination) { receiveEvents(player, worker) }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                Napier.e("Desktop video playback failed", failure)
                mutableState.value = DesktopVideoState.Failed
            } finally {
                frames.close()
                events.close()
            }
        }
    }

    fun play() {
        events.trySend(Event.Play)
    }
    fun pause() {
        events.trySend(Event.Pause)
    }
    fun seek(seconds: Double) {
        events.trySend(Event.Seek(seconds))
    }
    fun setVolume(volume: Float) {
        events.trySend(Event.Volume(volume))
    }
    fun close() {
        scope.cancel()
    }

    private suspend fun receiveEvents(player: DesktopVideoPlayer, worker: CoroutineDispatcher) =
        coroutineScope {
            mutableState.value = DesktopVideoState.Ready()
            launch {
                for (frame in frames) {
                    val current = mutableState.value
                    if (current is DesktopVideoState.Ready) mutableState.value = current.copy(frame = frame)
                }
            }
            launch {
                while (isActive) {
                    events.send(Event.Tick)
                    delay(250)
                }
            }
            for (event in events) {
                when (event) {
                    Event.Play -> withContext(worker) { player.play() }

                    Event.Pause -> withContext(worker) { player.pause() }

                    is Event.Seek -> withContext(worker) { player.seek(event.seconds) }

                    is Event.Volume -> withContext(worker) { player.setVolume(event.volume) }

                    Event.Tick -> {
                        val progress =
                            withContext(worker) {
                                Triple(player.position(), player.duration(), player.volume())
                            }
                        val current = mutableState.value as? DesktopVideoState.Ready
                        if (current != null) {
                            mutableState.value =
                                current.copy(
                                    position = progress.first,
                                    duration = progress.second,
                                    volume = progress.third,
                                )
                        }
                    }

                    is Event.Playback -> publishPlayback(event.value, player, worker)
                }
            }
        }

    private suspend fun publishPlayback(
        playback: DesktopPlayback,
        player: DesktopVideoPlayer,
        worker: CoroutineDispatcher,
    ) {
        val ratio =
            if (playback == DesktopPlayback.MediaReady) {
                withContext(worker) { player.aspectRatio() }
            } else {
                null
            }
        val current = mutableState.value as? DesktopVideoState.Ready ?: return
        mutableState.value =
            when (playback) {
                DesktopPlayback.MediaReady -> current.copy(aspectRatio = ratio)
                DesktopPlayback.Playing -> current.copy(playing = true, buffering = false)
                DesktopPlayback.Paused, DesktopPlayback.Finished -> current.copy(playing = false, buffering = false)
                DesktopPlayback.Buffering -> current.copy(buffering = true)
                DesktopPlayback.Buffered -> current.copy(buffering = false)
                DesktopPlayback.Failed -> DesktopVideoState.Failed
            }
    }

    private sealed interface Event {
        data object Tick : Event
        data class Seek(val seconds: Double) : Event
        data class Volume(val volume: Float) : Event
        data object Play : Event
        data object Pause : Event
        data class Playback(val value: DesktopPlayback) : Event
    }
}
