/* This is a private project. All rights reserved. */
import { toggleMediaPlayback } from "./media-controls.mjs";
import { runCase, launchBrowser, ownSite } from "./verification-runner.mjs";
import { getReportDirectory } from "./verification-reports.mjs";
import assert from "node:assert/strict";
import { resolve } from "node:path";
import { pathToFileURL } from "node:url";
import { writeFile } from "node:fs/promises";
import { startSite, work, chromiumBinaries } from "./verification-site.mjs";
import { installMediaDiagnostics } from "./verification-instrumentation.mjs";
const { chromium } = await import(
  pathToFileURL(resolve(work, "browser/node_modules/playwright/index.mjs"))
);
await runCase("verify-av-sync", async () => {
  const site = ownSite(await startSite());
  const results = [];
  const mediaPath = process.env.MEDIA_RENDER_PATH ?? "rgba-copy";
  const outputDelayMs = Number(process.env.MEDIA_SYNC_OUTPUT_DELAY_MS ?? 0);
  assert.ok(
    Number.isFinite(outputDelayMs) &&
      outputDelayMs >= 0 &&
      outputDelayMs <= 500,
  );
  try {
    for (const [browserName, executablePath] of Object.entries(
      chromiumBinaries,
    )) {
      await runCase(`browser ${browserName}`, async () => {
        const browser = await launchBrowser(
          chromium,
          {
            executablePath,
            headless: true,
          },
          { timingProbe: true },
        );
        try {
          const page = await browser.newPage({
            viewport: { width: 900, height: 800 },
            locale: "en-US",
          });
          const errors = [];
          page.on("pageerror", (e) => errors.push(e.message));
          await installMediaDiagnostics(page);
          if (outputDelayMs > 0)
            await page.addInitScript((delay) => {
              const timestamp = AudioContext.prototype.getOutputTimestamp;
              AudioContext.prototype.getOutputTimestamp = function () {
                const value = timestamp.call(this);
                return {
                  contextTime: Math.max(0, value.contextTime - delay / 1000),
                  performanceTime: value.performanceTime,
                };
              };
            }, outputDelayMs);
          await page.goto(
            site.origin + "/compose/?media=/sync.mp4&mediaPath=" + mediaPath,
          );
          await page.waitForFunction(
            () => window.mediaTest?.decoded.length > 0,
          );
          await page.locator("canvas").evaluate((canvas) => {
            mediaTest.canvas = canvas;
          });
          await page.evaluate(
            () =>
              new Promise((resolve) =>
                requestAnimationFrame(() => requestAnimationFrame(resolve)),
              ),
          );
          await toggleMediaPlayback(page);
          await page.waitForFunction(() =>
            mediaTest.contexts.some((c) => c.state === "running"),
          );
          await page.evaluate(() => {
            const stream = mediaTest.canvas.captureStream();
            const reader = new MediaStreamTrackProcessor({
              track: stream.getVideoTracks()[0],
            }).readable.getReader();
            const context = mediaTest.contexts.at(-1),
              gain = mediaTest.gains.at(-1),
              analyser = context.createAnalyser();
            analyser.fftSize = 256;
            gain.connect(analyser);
            const samples = new Float32Array(256);
            window.syncProbe = {
              audio: [],
              video: [],
              running: true,
              error: "",
              reader,
              stream,
              gain,
              analyser,
            };
            let sound = false,
              flash = false;
            syncProbe.timer = setInterval(() => {
              analyser.getFloatTimeDomainData(samples);
              const rms = Math.sqrt(
                samples.reduce((s, x) => s + x * x, 0) / samples.length,
              );
              const next = rms > 0.02;
              if (next && !sound) {
                const stamp = context.getOutputTimestamp();
                if (!(stamp.performanceTime > 0 && stamp.contextTime > 0)) {
                  syncProbe.error = "No initialized audio output timestamp";
                } else {
                  syncProbe.audio.push({
                    graphMs: performance.now(),
                    outputMs:
                      stamp.performanceTime +
                      (context.currentTime - stamp.contextTime) * 1000,
                  });
                }
              }
              sound = next;
            }, 4);
            syncProbe.task = (async () => {
              try {
                while (syncProbe.running) {
                  const { value: frame, done } = await reader.read();
                  if (done) break;
                  try {
                    const options = {
                      format: "RGBA",
                      rect: { x: 50, y: 130, width: 100, height: 100 },
                    };
                    const pixels = new Uint8Array(
                      frame.allocationSize(options),
                    );
                    await frame.copyTo(pixels, options);
                    const next =
                      pixels[0] > 220 && pixels[1] > 220 && pixels[2] > 220;
                    if (next && !flash) syncProbe.video.push(performance.now());
                    flash = next;
                  } finally {
                    frame.close();
                  }
                }
              } catch (error) {
                syncProbe.error = error.message;
              }
            })();
          });
          await page.waitForFunction(
            () =>
              syncProbe.error ||
              (syncProbe.audio.length >= 3 && syncProbe.video.length >= 3),
          );
          const before = await page.evaluate(() => ({
            audio: [...syncProbe.audio],
            video: [...syncProbe.video],
            error: syncProbe.error,
          }));
          assert.equal(before.error, "");
          const pausedWorker = await page.evaluate(
            () => mediaTest.workers.length,
          );
          await toggleMediaPlayback(page);
          await page.waitForFunction(
            (worker) =>
              mediaTest.connected.size === 0 &&
              mediaTest.decoded.at(-1)?.worker === worker,
            pausedWorker,
          );
          await page.evaluate(
            () =>
              new Promise((resolve) =>
                requestAnimationFrame(() => requestAnimationFrame(resolve)),
              ),
          );
          await page.mouse.click(320, 502);
          await page.waitForFunction(
            () => Math.abs(mediaTest.decoded.at(-1).timestamp - 4) < 0.2,
          );
          await page.evaluate(
            () =>
              new Promise((resolve) =>
                requestAnimationFrame(() => requestAnimationFrame(resolve)),
              ),
          );
          const counts = await page.evaluate(() => ({
            audio: syncProbe.audio.length,
            video: syncProbe.video.length,
          }));
          await toggleMediaPlayback(page);
          await page.waitForFunction(
            (counts) =>
              syncProbe.audio.length >= counts.audio + 3 &&
              syncProbe.video.length >= counts.video + 3,
            counts,
          );
          const after = await page.evaluate(
            (counts) => ({
              audio: syncProbe.audio.slice(counts.audio),
              video: syncProbe.video.slice(counts.video),
              error: syncProbe.error,
            }),
            counts,
          );
          assert.equal(after.error, "");
          const compare = (phase, field) =>
            phase.video.slice(0, 3).map((v, i) => v - phase.audio[i][field]);
          const result = {
            version: browser.version(),
            path: mediaPath,
            reportedExtraOutputDelayMs: outputDelayMs,
            beforeSeekMs: compare(before, "outputMs"),
            afterSeekMs: compare(after, "outputMs"),
            graphBeforeSeekMs: compare(before, "graphMs"),
            graphAfterSeekMs: compare(after, "graphMs"),
            measurement:
              "Observed Compose canvas flash onset minus analyser pulse mapped to the browser audio-output timestamp; not a physical speaker/display measurement. Injected delay changes reported timestamps only.",
          };
          for (const skew of [...result.beforeSeekMs, ...result.afterSeekMs])
            assert.ok(Math.abs(skew) < 80, `Observed A/V skew ${skew}ms`);
          await page.evaluate(async () => {
            syncProbe.running = false;
            clearInterval(syncProbe.timer);
            await syncProbe.reader.cancel();
            syncProbe.stream.getTracks().forEach((t) => t.stop());
            await syncProbe.task;
            syncProbe.gain.disconnect(syncProbe.analyser);
          });
          await page.mouse.click(90, 64);
          await page.waitForFunction(
            () =>
              mediaTest.contexts.every((c) => c.state === "closed") &&
              mediaTest.workers.every((w) => w.closed),
          );
          assert.deepEqual(errors, []);
          results.push({ browser: browserName, ...result });
        } finally {
          await browser.close();
        }
      });
    }
    await writeFile(
      resolve(
        await getReportDirectory(),
        `${mediaPath === "gpu-texture" ? "gpu-" : ""}av-sync-results${outputDelayMs ? `-delay-${outputDelayMs}` : ""}.json`,
      ),
      JSON.stringify(results, null, 2),
    );
    console.log(JSON.stringify(results, null, 2));
  } finally {
    await site.close();
  }
});
