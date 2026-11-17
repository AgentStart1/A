/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app

import kotlinx.coroutines.flow.MutableStateFlow
import space.kodio.core.AudioFlow
import space.kodio.core.AudioRecordingSession

internal class RecordingFixture(
    private val start: suspend () -> Unit = {},
    private val stop: () -> Unit = {},
    audio: AudioFlow? = null,
) : AudioRecordingSession {
    override val state = MutableStateFlow<AudioRecordingSession.State>(AudioRecordingSession.State.Idle)
    override val audioFlow = MutableStateFlow(audio)
    var stops = 0
        private set
    override suspend fun start() = start.invoke()
    override fun stop() {
        stops += 1
        stop.invoke()
    }
    override fun reset() = Unit
}
