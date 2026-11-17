/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebDriver
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration

/** Observes browser capture, recorder samples and presented remote frames in isolated test profiles. */
internal class WasmMediaProbe(private val browser: WebDriver) {
    private val script get() = browser as JavascriptExecutor
    private val wait get() = WebDriverWait(browser, Duration.ofSeconds(30))
    private var frameGeneration = 0L

    fun observeCapture(hold: Boolean = false) {
        script.executeScript(
            """
            const devices = navigator.mediaDevices;
            const acquire = devices.getUserMedia.bind(devices);
            window.appiumRtcTracks = [];
            window.appiumRtcReleaseCapture = null;
            const gate = arguments[0] ? new window.Promise(resolve => {
              window.appiumRtcReleaseCapture = resolve;
            }) : window.Promise.resolve();
            // Chaining the native result preserves the page Promise realm in Firefox.
            devices.getUserMedia = constraints => acquire(constraints).then(stream => {
              window.appiumRtcTracks.push(...stream.getTracks());
              return gate.then(() => stream);
            });
            """.trimIndent(),
            hold,
        )
    }

    fun observeRtcAudio() {
        script.executeScript(
            """
            window.appiumRtcPeers = [];
            const NativePeer = window.RTCPeerConnection;
            window.RTCPeerConnection = new Proxy(NativePeer, {
              construct(target, args) {
                const peer = Reflect.construct(target, args);
                window.appiumRtcPeers.push(peer);
                return peer;
              }
            });
            """.trimIndent(),
        )
    }

    fun awaitReceivedAudioGrowth() {
        val baseline =
            wait.until {
                receivedAudioCounters().takeIf { counters -> counters.all { it > 0 } }
            }
        wait.until {
            receivedAudioCounters().zip(baseline).all { (current, previous) -> current > previous }
        }
    }

    private fun receivedAudioCounters(): List<Long> {
        val counters =
            script.executeAsyncScript(
                """
                const done = arguments[arguments.length - 1];
                window.Promise.all(window.appiumRtcPeers.map(peer => peer.getStats())).then(reports => {
                  let packets = 0;
                  let bytes = 0;
                  reports.forEach(report => report.forEach(stat => {
                    if (stat.type === 'inbound-rtp' && (stat.kind === 'audio' || stat.mediaType === 'audio')) {
                      packets += stat.packetsReceived || 0;
                      bytes += stat.bytesReceived || 0;
                    }
                  }));
                  done([packets, bytes]);
                }, failure => done({failure: String(failure)}));
                """.trimIndent(),
            )
        check(counters is List<*> && counters.size == 2) { "Could not read inbound audio RTP counters" }
        return counters.map { counter ->
            check(counter is Number) { "Audio RTP counter must be numeric" }
            counter.toLong()
        }
    }

    fun observeRecordingSamples() {
        script.executeScript(
            """
            window.appiumRecorderSamples = 0;
            window.appiumRecorderSignal = false;
            const NativeNode = window.AudioWorkletNode;
            window.AudioWorkletNode = new Proxy(NativeNode, {
              construct(target, args) {
                const node = Reflect.construct(target, args);
                node.port.addEventListener('message', event => {
                  if (!ArrayBuffer.isView(event.data)) return;
                  window.appiumRecorderSamples += event.data.length;
                  if (event.data.some(value => Math.abs(value) > 0.0001)) window.appiumRecorderSignal = true;
                });
                node.port.start();
                return node;
              }
            });
            """.trimIndent(),
        )
    }

    fun awaitRecordingSamples() {
        wait.until {
            script.executeScript(
                "return window.appiumRecorderSamples > 0 && window.appiumRecorderSignal;",
            ) == true
        }
    }

    fun rejectNextCapture() {
        script.executeScript(
            """
            const devices = navigator.mediaDevices;
            const acquire = devices.getUserMedia.bind(devices);
            let reject = true;
            devices.getUserMedia = constraints => {
              if (reject) {
                reject = false;
                return window.Promise.reject(
                  new window.DOMException('Synthetic microphone permission denied', 'NotAllowedError'),
                );
              }
              return acquire(constraints);
            };
            """.trimIndent(),
        )
    }

    fun awaitPresentedRemoteFrames() {
        val generation = ++frameGeneration
        wait.until {
            script.executeScript(
                """
                function videos(root) {
                  return [...root.querySelectorAll('video'), ...[...root.querySelectorAll('*')]
                    .filter(node => node.shadowRoot).flatMap(node => videos(node.shadowRoot))];
                }
                const remote = videos(document).filter(video => video.srcObject &&
                  video.style.transform !== 'scaleX(-1)');
                const generation = arguments[0];
                remote.forEach(video => {
                  if (video.appiumFrameProbe?.generation === generation) return;
                  const probe = {count: 0, generation};
                  video.appiumFrameProbe = probe;
                  const frame = () => {
                    probe.count++;
                    if (probe.count < 2) video.requestVideoFrameCallback(frame);
                  };
                  video.requestVideoFrameCallback(frame);
                });
                return remote.some(video => video.appiumFrameProbe.generation === generation &&
                  video.appiumFrameProbe.count >= 2 &&
                  video.videoWidth > 0 && video.videoHeight > 0);
                """.trimIndent(),
                generation,
            ) == true
        }
    }

    fun awaitCaptureReleased(minimum: Int = 2) {
        wait.withMessage { captureState() }.until {
            script.executeScript(
                "return window.appiumRtcTracks.length >= arguments[0] && " +
                    "window.appiumRtcTracks.every(track => track.readyState === 'ended');",
                minimum,
            ) == true
        }
    }

    fun awaitCaptureStarted(minimum: Int = 2) {
        wait.withMessage { captureState() }.until {
            script.executeScript("return window.appiumRtcTracks.length >= arguments[0];", minimum) == true
        }
    }

    fun releaseCapture() {
        script.executeScript("window.appiumRtcReleaseCapture();")
    }

    fun assertCaptureNotRequested() {
        kotlin.test.assertEquals(0L, script.executeScript("return window.appiumRtcTracks.length;"))
    }

    private fun captureState(): String =
        script.executeScript(
            "return JSON.stringify(window.appiumRtcTracks.map(track => " +
                "({kind: track.kind, state: track.readyState, enabled: track.enabled})));",
        )?.toString() ?: "Capture state is unavailable"
}
