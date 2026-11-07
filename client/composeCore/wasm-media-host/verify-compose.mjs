/* This is a private project. All rights reserved. */
import {
  toggleMediaPlayback,
  toggleMediaMute,
  seekMediaMidpoint,
} from "./media-controls.mjs";
import { runCase, launchBrowser, ownSite } from "./verification-runner.mjs";
import { getReportDirectory } from "./verification-reports.mjs";
import assert from "node:assert/strict";
import { resolve } from "node:path";
import { pathToFileURL } from "node:url";
import { writeFile } from "node:fs/promises";
import { createHash } from "node:crypto";
import { startSite, work, chromiumBinaries } from "./verification-site.mjs";
import { installMediaDiagnostics } from "./verification-instrumentation.mjs";
const { chromium } = await import(
  pathToFileURL(resolve(work, "browser/node_modules/playwright/index.mjs"))
);
const { PNG } = await import(
  pathToFileURL(resolve(work, "browser/node_modules/pngjs/lib/png.js"))
);
await runCase("verify-compose", async () => {
  const site = ownSite(await startSite());
  const results = [];
  const mediaPath = process.env.MEDIA_RENDER_PATH ?? "rgba-copy";
  const prefix = mediaPath === "gpu-texture" ? "gpu" : "compose";
  try {
    for (const [browserName, executablePath] of Object.entries(
      chromiumBinaries,
    )) {
      await runCase(`browser ${browserName}`, async () => {
        const browser = await launchBrowser(chromium, {
          executablePath,
          headless: true,
        });
        try {
          for (const [kind, query, width, height] of [
            ["av", "", 320, 180],
            ["hd", "?media=/fixture-hd.mp4", 1280, 720],
            ["fhd", "?media=/fixture-fhd.mp4", 1920, 1080],
            ["audio", "?audio=true", 0, 0],
          ]) {
            await runCase(`Compose ${mediaPath} ${kind}`, async () => {
              const page = await browser.newPage({
                viewport: { width: 900, height: 800 },
                locale: "en-US",
              });
              const errors = [];
              let rejectPageError;
              const pageFailure = new Promise((_, reject) => {
                rejectPageError = reject;
              });
              pageFailure.catch(() => {});
              page.on("pageerror", (e) => {
                errors.push(e.message);
                rejectPageError(e);
              });
              const waitFor = (predicate, arg) =>
                Promise.race([
                  page.waitForFunction(predicate, arg),
                  pageFailure,
                ]);
              await installMediaDiagnostics(page);
              await page.goto(
                site.origin +
                  "/compose/" +
                  (query || "?") +
                  (query ? "&" : "") +
                  "mediaPath=" +
                  mediaPath,
              );
              await Promise.race([page.waitForSelector("canvas"), pageFailure]);
              await waitFor(() => mediaTest.workers.length > 0);
              if (kind !== "audio")
                await waitFor(() => mediaTest.decoded.length > 0);
              else await page.waitForTimeout(300);
              assert.equal(await page.locator("video,audio").count(), 0);
              await page.locator("canvas").evaluate((canvas) => {
                mediaTest.canvas = canvas;
              });
              assert.deepEqual(
                await page
                  .locator("canvas")
                  .evaluate((c) => ({ width: c.width, height: c.height })),
                { width: 900, height: 800 },
              );
              await page.evaluate(
                () =>
                  new Promise((resolve) =>
                    requestAnimationFrame(() => requestAnimationFrame(resolve)),
                  ),
              );
              if (kind !== "audio") {
                await page.evaluate(() => {
                  const stream = mediaTest.canvas.captureStream();
                  const reader = new MediaStreamTrackProcessor({
                    track: stream.getVideoTracks()[0],
                  }).readable.getReader();
                  mediaTest.capture = {
                    stream,
                    reader,
                    running: true,
                    frames: [],
                    error: "",
                  };
                  mediaTest.capture.task = (async () => {
                    try {
                      while (mediaTest.capture.running) {
                        const { value: frame, done } = await reader.read();
                        if (done) break;
                        try {
                          const options = {
                            format: "RGBA",
                            rect: { x: 16, y: 88, width: 608, height: 342 },
                          };
                          const pixels = new Uint8Array(
                            frame.allocationSize(options),
                          );
                          await frame.copyTo(pixels, options);
                          let hash = 0;
                          for (let i = 0; i < pixels.length; i += 97)
                            hash = (hash * 31 + pixels[i]) | 0;
                          mediaTest.capture.frames.push({
                            time: performance.now(),
                            hash,
                          });
                        } finally {
                          frame.close();
                        }
                      }
                    } catch (error) {
                      mediaTest.capture.error = error.message;
                    }
                  })();
                });
              }
              await toggleMediaPlayback(page);
              await waitFor(() =>
                mediaTest.contexts.some((c) => c.state === "running"),
              );
              const start = await page.evaluate(() => performance.now());
              const clip =
                kind === "audio"
                  ? { x: 16, y: 88, width: 608, height: 170 }
                  : { x: 145, y: 100, width: 250, height: 120 };
              const first = await page.screenshot({ clip });
              await page.waitForTimeout(1200);
              const second = await page.screenshot({ clip });
              if (kind !== "audio")
                assert.notEqual(
                  createHash("sha256").update(first).digest("hex"),
                  createHash("sha256").update(second).digest("hex"),
                );
              const measured = await page.evaluate(
                async ({ width, height, start }) => {
                  const context = mediaTest.contexts.at(-1),
                    gain = mediaTest.gains.at(-1),
                    a = context.createAnalyser();
                  gain.connect(a);
                  await new Promise((r) => setTimeout(r, 100));
                  const b = new Float32Array(a.fftSize);
                  a.getFloatTimeDomainData(b);
                  gain.disconnect(a);
                  const frames = mediaTest.decoded.filter(
                    (f) => f.time >= start,
                  );
                  return {
                    registeredTextureHandles: [...mediaTest.frameTextures].map(
                      (t) => t.name ?? 0,
                    ),
                    rms: Math.sqrt(b.reduce((s, v) => s + v * v, 0) / b.length),
                    elapsed: (performance.now() - start) / 1000,
                    decoded: frames.length,
                    readbackMs: frames.reduce((s, f) => s + f.readbackMs, 0),
                    copiedBytes: frames.reduce((s, f) => s + f.bytes, 0),
                    captureError: mediaTest.capture?.error ?? "",
                    capturedUnique: new Set(
                      (mediaTest.capture?.frames ?? [])
                        .filter((f) => f.time >= start)
                        .map((f) => f.hash),
                    ).size,
                    matchingUploads: mediaTest.uploads.filter(
                      (u) => u.w === width && u.h === height && u.time >= start,
                    ).length,
                  };
                },
                { width, height, start },
              );
              assert.ok(measured.rms > 0.01);
              if (mediaPath === "gpu-texture" && kind !== "audio") {
                assert.ok(measured.registeredTextureHandles.length > 0);
                assert.ok(
                  measured.registeredTextureHandles.every(
                    (id) => Number.isInteger(id) && id > 0,
                  ),
                );
              }
              if (kind !== "audio") {
                assert.equal(measured.captureError, "");
                assert.ok(measured.capturedUnique > 10);
                await page.evaluate(async () => {
                  mediaTest.capture.running = false;
                  await mediaTest.capture.reader.cancel();
                  mediaTest.capture.stream.getTracks().forEach((t) => t.stop());
                  await mediaTest.capture.task;
                });
              }
              if (kind !== "audio") {
                assert.ok(measured.decoded > 10);
                assert.ok(measured.matchingUploads > 10);
              }
              await toggleMediaMute(page);
              assert.equal(
                await page.evaluate(() => mediaTest.gains.at(-1).gain.value),
                0,
              );
              await toggleMediaMute(page);
              assert.equal(
                await page.evaluate(() => mediaTest.gains.at(-1).gain.value),
                1,
              );
              // Wait for the replacement generation's paused preview before using its controls.
              const pausedWorker = await page.evaluate(
                () => mediaTest.workers.length,
              );
              await toggleMediaPlayback(page);
              await waitFor(
                ({ pausedWorker, kind }) =>
                  (kind === "audio" ? mediaTest.audio : mediaTest.decoded).at(
                    -1,
                  )?.worker === pausedWorker,
                { pausedWorker, kind },
              );
              await page.evaluate(
                () =>
                  new Promise((resolve) =>
                    requestAnimationFrame(() => requestAnimationFrame(resolve)),
                  ),
              );
              assert.equal(
                await page.evaluate(() => mediaTest.connected.size),
                0,
              );
              if (kind !== "audio") {
                const pauseA = await page.screenshot({ clip });
                await page.waitForTimeout(180);
                const pauseB = await page.screenshot({ clip });
                assert.equal(
                  createHash("sha256").update(pauseA).digest("hex"),
                  createHash("sha256").update(pauseB).digest("hex"),
                );
                const target = kind === "av" ? 3 : 2;
                await seekMediaMidpoint(page);
                await waitFor(
                  (target) =>
                    Math.abs(mediaTest.decoded.at(-1).timestamp - target) < 0.2,
                  target,
                );
                assert.equal(
                  await page.evaluate(() => mediaTest.connected.size),
                  0,
                );
                // Click an ordinary Compose control painted on top of the video.
                const before = PNG.sync.read(await page.screenshot());
                await page.mouse.click(545, 112);
                await page.waitForTimeout(200);
                await page.screenshot({
                  path: resolve(
                    await getReportDirectory(),
                    `${prefix}-${browserName}-${kind}-menu.png`,
                  ),
                });
                await page.mouse.click(545, 160);
                await page.waitForTimeout(200);
                const dialog = await page.screenshot();
                await writeFile(
                  resolve(
                    await getReportDirectory(),
                    `${prefix}-${browserName}-${kind}-dialog.png`,
                  ),
                  dialog,
                );
                const after = PNG.sync.read(dialog);
                assert.notEqual(before.data[3 * 4], after.data[3 * 4]);
                await page.keyboard.press("Escape");
                await page.waitForTimeout(200);
                const clipped = PNG.sync.read(await page.screenshot());
                const offset = (90 * clipped.width + 18) * 4;
                assert.ok(
                  clipped.data[offset] > 240 &&
                    clipped.data[offset + 1] > 240 &&
                    clipped.data[offset + 2] > 240,
                );
              }
              await page.screenshot({
                path: resolve(
                  await getReportDirectory(),
                  `${prefix}-${browserName}-${kind}.png`,
                ),
              });
              await toggleMediaPlayback(page);
              await waitFor(
                () => mediaTest.contexts.at(-1).state === "running",
              );
              const workersBefore = await page.evaluate(
                () => mediaTest.workers.length,
              );
              await page.mouse.click(235, 64);
              await waitFor(
                ({ workersBefore, kind }) =>
                  (kind === "audio" ? mediaTest.audio : mediaTest.decoded).at(
                    -1,
                  )?.worker === workersBefore,
                { workersBefore, kind },
              );
              await page.evaluate(
                () =>
                  new Promise((resolve) =>
                    requestAnimationFrame(() => requestAnimationFrame(resolve)),
                  ),
              );
              if (kind !== "audio") {
                assert.ok(
                  await page.evaluate(
                    () => mediaTest.decoded.at(-1).timestamp < 0.1,
                  ),
                );
                assert.equal(
                  await page.evaluate(() => mediaTest.decoded.at(-1).bytes),
                  mediaPath === "gpu-texture" ? 0 : 320 * 180 * 4,
                );
              }
              await waitFor(
                () =>
                  mediaTest.contexts.every((c) => c.state === "closed") &&
                  mediaTest.connected.size === 0,
              );
              assert.equal(
                await page.evaluate(
                  () => mediaTest.workers.filter((w) => !w.closed).length,
                ),
                1,
              );
              await page.mouse.click(90, 64);
              await page.waitForTimeout(250);
              const disposed = await page.evaluate(() => ({
                activeWorkers: mediaTest.workers.filter((w) => !w.closed)
                  .length,
                contexts: mediaTest.contexts.map((c) => c.state),
                connected: mediaTest.connected.size,
                liveFrames: mediaTest.transferredFrames.size,
                liveTextures: mediaTest.frameTextures.size,
                deletedTextures: mediaTest.deletedFrameTextures,
              }));
              assert.equal(disposed.activeWorkers, 0);
              assert.ok(disposed.contexts.every((s) => s === "closed"));
              assert.equal(disposed.connected, 0);
              assert.equal(disposed.liveFrames, 0);
              assert.equal(disposed.liveTextures, 0);
              assert.deepEqual(errors, []);
              if (mediaPath === "gpu-texture" && kind !== "audio") {
                assert.equal(
                  await page.evaluate(
                    () =>
                      mediaTest.frameCanvases.size === 1 &&
                      mediaTest.frameCanvases.has(mediaTest.canvas),
                  ),
                  true,
                );
              }
              results.push({
                path: mediaPath,
                browser: browserName,
                kind,
                version: browser.version(),
                ...measured,
                disposed,
              });
              await page.close();
            });
          }
        } finally {
          await browser.close();
        }
      });
    }
    await writeFile(
      resolve(await getReportDirectory(), `${prefix}-results.json`),
      JSON.stringify(results, null, 2),
    );
    console.log(JSON.stringify(results, null, 2));
  } finally {
    await site.close();
  }
});
