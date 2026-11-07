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
const { PNG } = await import(
  pathToFileURL(resolve(work, "browser/node_modules/pngjs/lib/png.js"))
);
await runCase("verify-gpu-lifecycle", async () => {
  const site = ownSite(await startSite()),
    results = [];
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
          const open = async (query = "", before) => {
            const page = await browser.newPage({
              viewport: { width: 900, height: 800 },
              locale: "en-US",
            });
            const errors = [];
            page.on("pageerror", (e) => errors.push(e.message));
            await installMediaDiagnostics(page);
            await before?.(page);
            await page.goto(
              `${site.origin}/compose/?mediaPath=gpu-texture${query}`,
            );
            await page.waitForFunction(() => mediaTest.decoded.length > 0);
            await page
              .locator("canvas")
              .evaluate((c) => (mediaTest.canvas = c));
            await page.evaluate(
              () =>
                new Promise((r) =>
                  requestAnimationFrame(() => requestAnimationFrame(r)),
                ),
            );
            return { page, errors };
          };
          await runCase("resize playback", async () => {
            const { page, errors } = await open();
            try {
              await toggleMediaPlayback(page);
              await page.waitForFunction(() => mediaTest.decoded.length > 10);
              for (const [width, height] of [
                [1000, 850],
                [900, 800],
              ]) {
                const before = await page.evaluate(
                  () => mediaTest.decoded.length,
                );
                await page.setViewportSize({ width, height });
                await page.waitForFunction(
                  ({ width, height, before }) =>
                    mediaTest.canvas.width === width &&
                    mediaTest.canvas.height === height &&
                    mediaTest.decoded.length > before + 6,
                  { width, height, before },
                );
              }
              assert.equal(
                await page.evaluate(() => mediaTest.frameCanvases.size),
                1,
              );
              await page.mouse.click(90, 64);
              await page.waitForFunction(
                () =>
                  mediaTest.frameTextures.size === 0 &&
                  mediaTest.transferredFrames.size === 0 &&
                  mediaTest.workers.every((w) => w.closed),
              );
              assert.deepEqual(errors, []);
              results.push({
                browser: browserName,
                case: "resize preserves the renderer and playback",
                passed: true,
              });
            } finally {
              await page.close();
            }
          });
          await runCase("remount ownership", async () => {
            const { page, errors } = await open();
            try {
              for (let i = 0; i < 3; i++) {
                await page.mouse.click(90, 64);
                await page.waitForFunction(
                  () =>
                    mediaTest.frameTextures.size === 0 &&
                    mediaTest.transferredFrames.size === 0 &&
                    mediaTest.workers.every((w) => w.closed),
                );
                const count = await page.evaluate(
                  () => mediaTest.workers.length,
                );
                await page.mouse.click(90, 64);
                await page.waitForFunction(
                  (count) =>
                    mediaTest.workers.length > count &&
                    mediaTest.frameTextures.size === 1,
                  count,
                );
              }
              await page.mouse.click(90, 64);
              await page.waitForFunction(
                () =>
                  mediaTest.frameTextures.size === 0 &&
                  mediaTest.transferredFrames.size === 0,
              );
              assert.deepEqual(errors, []);
              results.push({
                browser: browserName,
                case: "repeated remount releases every adopted texture",
                passed: true,
              });
            } finally {
              await page.close();
            }
          });
          await runCase("context loss cleanup", async () => {
            const { page, errors } = await open();
            try {
              await toggleMediaPlayback(page);
              await page.waitForFunction(() => mediaTest.decoded.length > 10);
              await page.evaluate(() =>
                mediaTest.canvas
                  .getContext("webgl2")
                  .getExtension("WEBGL_lose_context")
                  .loseContext(),
              );
              await page.waitForFunction(
                () =>
                  mediaTest.workers.every((w) => w.closed) &&
                  mediaTest.transferredFrames.size === 0 &&
                  mediaTest.connected.size === 0 &&
                  mediaTest.contexts.every((c) => c.state === "closed"),
              );
              assert.deepEqual(errors, []);
              results.push({
                browser: browserName,
                case: "context loss terminates decoding/audio and closes transferred frames",
                passed: true,
              });
            } finally {
              await page.close();
            }
          });
          await runCase("upload rejection cleanup", async () => {
            const { page, errors } = await open("", async (page) =>
              page.addInitScript(() => {
                const upload = WebGL2RenderingContext.prototype.texImage2D;
                WebGL2RenderingContext.prototype.texImage2D = function (
                  ...args
                ) {
                  if (args.at(-1) instanceof VideoFrame)
                    throw new Error("Synthetic upload rejection");
                  return upload.apply(this, args);
                };
              }),
            );
            try {
              await page.waitForFunction(
                () =>
                  mediaTest.workers.every((w) => w.closed) &&
                  mediaTest.transferredFrames.size === 0 &&
                  mediaTest.frameTextures.size === 0,
              );
              assert.deepEqual(errors, []);
              results.push({
                browser: browserName,
                case: "upload rejection cancels the host without a leaked frame/texture or uncaught error",
                passed: true,
              });
            } finally {
              await page.close();
            }
          });
          await runCase("rotated GPU agrees with RGBA", async () => {
            let reference;
            for (const path of ["rgba-copy", "gpu-texture"]) {
              const page = await browser.newPage({
                viewport: { width: 900, height: 800 },
              });
              try {
                await installMediaDiagnostics(page);
                await page.goto(
                  `${site.origin}/compose/?media=/rotated.mp4&mediaPath=${path}`,
                );
                await page.waitForFunction(() => mediaTest.decoded.length > 0);
                await page.evaluate(
                  () =>
                    new Promise((r) =>
                      requestAnimationFrame(() => requestAnimationFrame(r)),
                    ),
                );
                const bytes = await page.screenshot();
                await writeFile(
                  resolve(
                    await getReportDirectory(),
                    `rotated-${browserName}-${path}.png`,
                  ),
                  bytes,
                );
                const png = PNG.sync.read(bytes);
                if (path === "rgba-copy") reference = png;
                else {
                  let total = 0,
                    count = 0;
                  for (let y = 90; y < 430; y++)
                    for (let x = 18; x < 622; x++)
                      for (let c = 0; c < 3; c++) {
                        total += Math.abs(
                          png.data[(y * png.width + x) * 4 + c] -
                            reference.data[(y * png.width + x) * 4 + c],
                        );
                        count++;
                      }
                  const error = total / count;
                  assert.ok(error < 5, `Rotated frame mismatch ${error}`);
                  results.push({
                    browser: browserName,
                    case: "rotated video agrees with RGBA reference",
                    meanAbsoluteChannelError: error,
                    passed: true,
                  });
                }
              } finally {
                await page.close();
              }
            }
          });
        } finally {
          await browser.close();
        }
      });
    }
    await writeFile(
      resolve(await getReportDirectory(), "gpu-lifecycle-results.json"),
      JSON.stringify(results, null, 2),
    );
    console.info(JSON.stringify(results, null, 2));
  } finally {
    await site.close();
  }
});
