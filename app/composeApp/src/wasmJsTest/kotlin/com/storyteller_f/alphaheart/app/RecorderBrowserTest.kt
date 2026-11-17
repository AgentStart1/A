/*
 * This is a private project. All rights reserved.
 */

@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.storyteller_f.alphaheart.app

import com.storyteller_f.alphaheart.app.utils.Recorder
import com.storyteller_f.alphaheart.app.utils.RecordingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import space.kodio.core.AudioFlow
import space.kodio.core.DefaultRecordingInt16
import kotlin.js.JsAny
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RecorderBrowserTest {
    @Test
    fun finishedRecordingExportsWaveWithoutNodeFiles() =
        runTest {
            val pcm = byteArrayOf(0, 1, 2, 3, 4, 5, 6, 7)
            val session = RecordingFixture(audio = AudioFlow(DefaultRecordingInt16, flowOf(pcm)))
            val recorder = Recorder { session }
            recorder.startRecord()
            val bytes = assertNotNull(recorder.stopRecord())
            assertEquals("RIFF", bytes.decodeToString(0, 4))
            assertEquals("WAVE", bytes.decodeToString(8, 12))
            assertContentEquals(pcm, bytes.copyOfRange(44, bytes.size))
            assertEquals(1, session.stops)
            assertIs<RecordingState.Idle>(recorder.state.value)
        }

    @Test
    fun rejectedCaptureIsReportableAndRecorderReturnsIdle() =
        runTest {
            withContext(Dispatchers.Main) {
                val original = rejectRecordingCapture()
                try {
                    val recorder = Recorder()
                    val failure = assertFailsWith<RuntimeException> { recorder.startRecord() }
                    assertTrue(failure.message.orEmpty().contains("Synthetic recording rejection"))
                    assertTrue(failure.stackTraceToString().contains("BrowserRecordingFailure"))
                    assertIs<RecordingState.Idle>(recorder.state.value)
                } finally {
                    restoreRecordingCapture(original)
                }
            }
        }
}

@JsFun(
    """() => {
        const original = navigator.mediaDevices.getUserMedia;
        navigator.mediaDevices.getUserMedia = () => Promise.reject(
            new DOMException('Synthetic recording rejection', 'NotAllowedError'));
        return original;
    }""",
)
private external fun rejectRecordingCapture(): JsAny

@JsFun("(original) => { navigator.mediaDevices.getUserMedia = original; }")
private external fun restoreRecordingCapture(original: JsAny)
