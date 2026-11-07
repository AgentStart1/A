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
const settle = (page) =>
  page.evaluate(
    () =>
      new Promise((done) =>
        requestAnimationFrame(() => requestAnimationFrame(done)),
      ),
  );

async function installBitmapProbe(page, gate) {
  await installMediaDiagnostics(page);
  await page.addInitScript((gate) => {
    window.bitmapProbe = {
      live: new Map(),
      installed: 0,
      closed: [],
      peak: 0,
      holdUntil: 0,
    };
    // GC must not make a missing explicit close look like correct ownership.
    const Registry = FinalizationRegistry;
    window.deferredFinalizers = [];
    window.FinalizationRegistry = class extends Registry {
      constructor(cleanup) {
        super((held) => deferredFinalizers.push(() => cleanup(held)));
      }
    };
    const raf = requestAnimationFrame;
    window.requestAnimationFrame = (callback) =>
      raf((time) => {
        const delay = bitmapProbe.holdUntil - performance.now();
        if (delay > 0) setTimeout(() => raf(callback), delay);
        else callback(time);
      });
    const wrap = (instance) => {
      const exports = instance.exports;
      const install =
        exports.org_jetbrains_skia_Bitmap__1nInstallPixelsFromPointer;
      const release = exports.org_jetbrains_skia_impl_Managed__invokeFinalizer;
      if (!install) return instance;
      return {
        exports: {
          ...exports,
          org_jetbrains_skia_Bitmap__1nInstallPixelsFromPointer: (...args) => {
            const result = install(...args);
            if (result) {
              const id = ++bitmapProbe.installed;
              bitmapProbe.live.set(args[0], id);
              bitmapProbe.peak = Math.max(
                bitmapProbe.peak,
                bitmapProbe.live.size,
              );
              if (gate && id === 1)
                bitmapProbe.holdUntil = performance.now() + 1000;
            }
            return result;
          },
          org_jetbrains_skia_impl_Managed__invokeFinalizer: (...args) => {
            if (bitmapProbe.live.has(args[1])) {
              bitmapProbe.closed.push(bitmapProbe.live.get(args[1]));
              bitmapProbe.live.delete(args[1]);
            }
            return release(...args);
          },
        },
      };
    };
    for (const method of ["instantiate", "instantiateStreaming"]) {
      const instantiate = WebAssembly[method];
      WebAssembly[method] = async function (...args) {
        const result = await instantiate.apply(this, args);
        return result.instance
          ? { ...result, instance: wrap(result.instance) }
          : wrap(result);
      };
    }
  }, gate);
}

await runCase("verify-bitmap-ownership", async () => {
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
          for (const scenario of [
            "committed-dispose",
            "pending-dispose",
            "pending-replace",
            "playing-dispose",
          ]) {
            await runCase(`bitmap ${scenario}`, async () => {
              const page = await browser.newPage({
                viewport: { width: 900, height: 800 },
              });
              const errors = [];
              page.on("pageerror", (error) => errors.push(error.message));
              try {
                const pending = scenario.startsWith("pending-");
                await installBitmapProbe(page, pending);
                await page.goto(
                  `${site.origin}/compose/?mediaPath=rgba-copy&mediaProfile=true`,
                );
                await page.waitForFunction(
                  () =>
                    performance.getEntriesByName("media.rgba.published")
                      .length > 0,
                  null,
                  { polling: 5 },
                );
                if (pending)
                  assert.ok(
                    await page.evaluate(
                      () => performance.now() < bitmapProbe.holdUntil,
                    ),
                    "The publication-before-commit gate was missed",
                  );
                else await settle(page);
                if (scenario === "playing-dispose") {
                  await toggleMediaPlayback(page);
                  await page.waitForFunction(
                    () =>
                      bitmapProbe.installed >= 15 &&
                      bitmapProbe.closed.length >= 10,
                  );
                }
                if (scenario === "pending-replace") {
                  await page.mouse.click(235, 64);
                  await page.waitForFunction(
                    () =>
                      mediaTest.workers.length > 1 &&
                      mediaTest.workers[0].closed &&
                      bitmapProbe.installed > 1,
                  );
                  await settle(page);
                  assert.ok(
                    await page.evaluate(() => bitmapProbe.closed.includes(1)),
                    "Replacing the source must explicitly close its uncommitted bitmap",
                  );
                }
                assert.ok(
                  await page.evaluate(() => bitmapProbe.live.size > 0),
                  "The visible or pending frame must stay alive while mounted",
                );
                await page.mouse.click(90, 64);
                await page.waitForFunction(
                  () =>
                    mediaTest.workers.every((worker) => worker.closed) &&
                    mediaTest.contexts.every(
                      (context) => context.state === "closed",
                    ),
                );
                await settle(page);
                const counts = await page.evaluate(() => ({
                  installed: bitmapProbe.installed,
                  closed: bitmapProbe.closed.length,
                  live: bitmapProbe.live.size,
                  peak: bitmapProbe.peak,
                }));
                assert.ok(counts.installed > 0);
                assert.equal(
                  counts.live,
                  0,
                  JSON.stringify({ browser: name, scenario, ...counts }),
                );
                assert.equal(counts.closed, counts.installed);
                assert.ok(
                  counts.peak <= 3,
                  `Native bitmap backlog: ${JSON.stringify(counts)}`,
                );
                assert.deepEqual(errors, []);
                results.push({
                  browser: name,
                  version: browser.version(),
                  scenario,
                  ...counts,
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
      resolve(await getReportDirectory(), "bitmap-ownership-results.json"),
      JSON.stringify(results, null, 2),
    );
    console.info(JSON.stringify(results, null, 2));
  } finally {
    await site.close();
  }
});
