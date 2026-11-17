/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.utils

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.io.readByteArray
import space.kodio.core.AudioRecordingSession
import space.kodio.core.SystemAudioSystem
import space.kodio.core.io.files.AudioFileFormat
import space.kodio.core.io.files.collectAsBuffer

internal sealed interface RecordingState {
    data object Idle : RecordingState
    class Starting : RecordingState
    data class Active(val session: AudioRecordingSession) : RecordingState
}

/** Owns capture for one recorder surface; closing it discards the recording. */
internal class Recorder(
    private val createSession: suspend () -> AudioRecordingSession = { SystemAudioSystem.createRecordingSession() },
) {
    private val mutableState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val state = mutableState.asStateFlow()

    suspend fun startRecord() {
        if (mutableState.value !is RecordingState.Idle) return
        val pending = RecordingState.Starting()
        mutableState.value = pending
        try {
            val session = runRecordingOperation { createSession() }
            AutoCloseable {
                if ((mutableState.value as? RecordingState.Active)?.session !== session) session.stop()
            }.use {
                runRecordingOperation { session.start() }
                currentCoroutineContext().ensureActive()
                if (mutableState.value === pending) mutableState.value = RecordingState.Active(session)
            }
        } finally {
            if (mutableState.value === pending) mutableState.value = RecordingState.Idle
        }
    }

    fun cancelRecord() {
        val previous = mutableState.value
        mutableState.value = RecordingState.Idle
        if (previous is RecordingState.Active) previous.session.stop()
    }

    suspend fun stopRecord(): ByteArray? {
        val active = mutableState.value as? RecordingState.Active ?: return null
        mutableState.value = RecordingState.Idle
        active.session.stop()
        val flow = active.session.audioFlow.value ?: return null
        return flow.collectAsBuffer(AudioFileFormat.Wav).use { it.readByteArray() }
    }
}

internal expect suspend fun <T> runRecordingOperation(block: suspend () -> T): T
