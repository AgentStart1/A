/*
 * This is a private project. All rights reserved.
 */

@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.storyteller_f.alphaheart.app.rtc

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import kotlin.js.JsAny
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class BrowserRtcCaptureTest {
    @Test
    fun capturesTracksAndBuildsAudioVideoOffer() =
        runTest {
            withContext(Dispatchers.Main) {
                val fixture = installSyntheticCapture(hold = false)
                try {
                    val engine = createRtcEngine()
                    val media = engine.openMedia(video = true)
                    try {
                        assertNotNull(media.video)
                        val peer = engine.createPeer(media, RtcPeerEvents({}, {}, {}), this)
                        try {
                            val offer = peer.createOffer()
                            assertTrue(offer.contains("m=audio"))
                            assertTrue(offer.contains("m=video"))
                        } finally {
                            peer.close()
                        }
                    } finally {
                        media.close()
                    }
                    assertTrue(syntheticTracksStopped(fixture, 2))
                    assertEquals("audio,video", syntheticCaptureKinds(fixture))
                } finally {
                    restoreSyntheticCapture(fixture)
                }
            }
        }

    @Test
    fun cancellationReleasesCaptureResolvedLater() =
        runTest {
            withContext(Dispatchers.Main) {
                val fixture = installSyntheticCapture(hold = true)
                try {
                    val capture =
                        launch(start = CoroutineStart.UNDISPATCHED) {
                            createRtcEngine().openMedia(video = true).close()
                        }
                    assertEquals("audio", syntheticCaptureKinds(fixture))
                    capture.cancel()
                    releaseSyntheticCapture(fixture)
                    capture.join()
                    assertTrue(capture.isCancelled)
                    assertTrue(syntheticTracksStopped(fixture, 1))
                } finally {
                    restoreSyntheticCapture(fixture)
                }
            }
        }

    @Test
    fun cancellationSurvivesLateBrowserFailure() =
        runTest {
            withContext(Dispatchers.Main) {
                val fixture = installSyntheticCapture(hold = true)
                try {
                    val capture =
                        launch(start = CoroutineStart.UNDISPATCHED) {
                            assertFailsWith<CancellationException> {
                                createRtcEngine().openMedia(video = true).close()
                            }
                        }
                    assertEquals("audio", syntheticCaptureKinds(fixture))
                    capture.cancel()
                    rejectSyntheticCapture(fixture)
                    capture.join()
                    assertTrue(capture.isCancelled)
                } finally {
                    restoreSyntheticCapture(fixture)
                }
            }
        }
}

@JsFun(
    """(hold) => {
        const original = navigator.mediaDevices.getUserMedia;
        const context = new AudioContext();
        const destination = context.createMediaStreamDestination();
        const oscillator = context.createOscillator();
        oscillator.connect(destination);
        oscillator.start();
        const canvas = document.createElement('canvas');
        canvas.width = 640;
        canvas.height = 480;
        canvas.getContext('2d').fillRect(0, 0, 640, 480);
        const audio = destination.stream.getAudioTracks()[0];
        const video = canvas.captureStream(1).getVideoTracks()[0];
        const tracks = [];
        const requests = [];
        let release, reject;
        const gate = hold ? new Promise((resolve, fail) => { release = resolve; reject = fail; }) : Promise.resolve();
        navigator.mediaDevices.getUserMedia = constraints => {
            requests.push([...(constraints.audio ? ['audio'] : []), ...(constraints.video ? ['video'] : [])].join('+'));
            const captured = [
                ...(constraints.audio ? [audio.clone()] : []), ...(constraints.video ? [video.clone()] : [])
            ];
            tracks.push(...captured);
            return gate.then(() => new MediaStream(captured));
        };
        return {original, context, oscillator, audio, video, tracks, requests, release, reject};
    }""",
)
private external fun installSyntheticCapture(hold: Boolean): JsAny

@JsFun(
    "(fixture, count) => fixture.tracks.length === count && fixture.tracks.every(track => track.readyState === 'ended')",
)
private external fun syntheticTracksStopped(fixture: JsAny, count: Int): Boolean

@JsFun("(fixture) => fixture.release()")
private external fun releaseSyntheticCapture(fixture: JsAny)

@JsFun("(fixture) => fixture.reject(new DOMException('Capture denied', 'NotAllowedError'))")
private external fun rejectSyntheticCapture(fixture: JsAny)

@JsFun("(fixture) => fixture.requests.join(',')")
private external fun syntheticCaptureKinds(fixture: JsAny): String

@JsFun(
    """(fixture) => {
        navigator.mediaDevices.getUserMedia = fixture.original;
        fixture.audio.stop();
        fixture.video.stop();
        fixture.tracks.forEach(track => track.stop());
        fixture.oscillator.stop();
        fixture.context.close();
    }""",
)
private external fun restoreSyntheticCapture(fixture: JsAny)
