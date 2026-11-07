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
await runCase("verify-rgba-failure", async () => {
  const site = ownSite(await startSite());
  const results = [];
  try {
    for (const [name, executablePath] of Object.entries(chromiumBinaries)) {
      await runCase(`browser ${name}`, async () => {
        const browser = await launchBrowser(chromium, {
          executablePath,
          headless: true,
        });
        try {
          for (const [phase, rejection] of [
            ["preview", "false"],
            ["playing", "false"],
            ["preview", "throw"],
            ["playing", "throw"],
          ]) {
            await runCase(`RGBA ${phase} ${rejection}`, async () => {
              const page = await browser.newPage({
                viewport: { width: 900, height: 800 },
              });
              const errors = [];
              page.on("pageerror", (error) => errors.push(error.message));
              try {
                await installMediaDiagnostics(page);
                await page.addInitScript(
                  ({ phase, rejection }) => {
                    window.pixelFailure = phase === "preview";
                    window.rejectedPixels = 0;
                    window.mediaStates = [];
                    const freeze = Object.freeze;
                    Object.freeze = function (value) {
                      if (
                        value?.path === "rgba-copy" &&
                        typeof value.acceptedFrames === "number"
                      )
                        mediaStates.push({
                          status: value.status,
                          error: value.error,
                        });
                      return freeze(value);
                    };
                    const symbol =
                      "org_jetbrains_skia_Bitmap__1nInstallPixelsFromPointer";
                    const wrap = (instance) => {
                      const install = instance.exports[symbol];
                      if (!install) return instance;
                      return {
                        exports: {
                          ...instance.exports,
                          [symbol]: (...args) => {
                            if (pixelFailure) {
                              pixelFailure = false;
                              rejectedPixels++;
                              if (rejection === "throw")
                                throw new Error(
                                  "Synthetic bitmap import rejection",
                                );
                              return 0;
                            }
                            return install(...args);
                          },
                        },
                      };
                    };
                    for (const method of [
                      "instantiate",
                      "instantiateStreaming",
                    ]) {
                      const instantiate = WebAssembly[method];
                      WebAssembly[method] = async function (...args) {
                        const result = await instantiate.apply(this, args);
                        return result.instance
                          ? { ...result, instance: wrap(result.instance) }
                          : wrap(result);
                      };
                    }
                  },
                  { phase, rejection },
                );
                await page.goto(`${site.origin}/compose/?mediaPath=rgba-copy`);
                if (phase === "playing") {
                  await page.waitForFunction(
                    () => mediaTest.decoded.length > 0,
                  );
                  await toggleMediaPlayback(page);
                  await page.waitForFunction(
                    () =>
                      mediaTest.decoded.length > 6 &&
                      mediaTest.contexts.some((c) => c.state === "running"),
                  );
                  await page.evaluate(() => {
                    pixelFailure = true;
                  });
                }
                await page.waitForFunction(
                  () =>
                    rejectedPixels === 1 &&
                    mediaStates.at(-1)?.status === "error" &&
                    mediaTest.workers.every((w) => w.closed) &&
                    mediaTest.contexts.every((c) => c.state === "closed") &&
                    mediaTest.connected.size === 0,
                );
                assert.ok(
                  await page.evaluate(
                    () => mediaStates.at(-1).error.length > 0,
                  ),
                );
                assert.deepEqual(errors, []);
                const before = await page.evaluate(() => ({
                  workers: mediaTest.workers.length,
                  frames: mediaTest.decoded.length,
                }));
                // Native controls must still work after failure, and a fresh Host must recover.
                await page.mouse.click(90, 64);
                await page.evaluate(
                  () =>
                    new Promise((resolve) =>
                      requestAnimationFrame(() =>
                        requestAnimationFrame(resolve),
                      ),
                    ),
                );
                await page.mouse.click(90, 64);
                await page.waitForFunction(
                  (before) =>
                    mediaTest.workers.length > before.workers &&
                    mediaTest.decoded.length > before.frames &&
                    mediaStates.at(-1)?.status === "paused",
                  before,
                );
                await page.evaluate(
                  () =>
                    new Promise((resolve) =>
                      requestAnimationFrame(() =>
                        requestAnimationFrame(resolve),
                      ),
                    ),
                );
                await toggleMediaPlayback(page);
                await page.waitForFunction(
                  (before) =>
                    mediaTest.decoded.length > before.frames + 6 &&
                    mediaTest.contexts.some((c) => c.state === "running"),
                  before,
                );
                await page.mouse.click(90, 64);
                await page.waitForFunction(
                  () =>
                    mediaTest.workers.every((w) => w.closed) &&
                    mediaTest.contexts.every((c) => c.state === "closed") &&
                    mediaTest.connected.size === 0,
                );
                assert.deepEqual(errors, []);
                results.push({
                  browser: name,
                  version: browser.version(),
                  phase,
                  rejection,
                  passed: true,
                });
              } finally {
                await page.close();
              }
            });
          }
        } finally {
          await browser.close();
        }
      });
    }
    await writeFile(
      resolve(await getReportDirectory(), "rgba-failure-results.json"),
      JSON.stringify(results, null, 2),
    );
    console.info(JSON.stringify(results, null, 2));
  } finally {
    await site.close();
  }
});
