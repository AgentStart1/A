/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebDriver
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration

/** Observes real browser capture tracks and presented remote frames without replacing media. */
internal class WasmRtcProbe(private val browser: WebDriver) {
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
