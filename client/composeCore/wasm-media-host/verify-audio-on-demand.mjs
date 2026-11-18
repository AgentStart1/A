/* This is a private project. All rights reserved. */
import assert from "node:assert/strict";
import { resolve } from "node:path";
import { pathToFileURL } from "node:url";
import { toggleMediaPlayback } from "./media-controls.mjs";
import { installMediaDiagnostics } from "./verification-instrumentation.mjs";
import { launchBrowser, ownSite, runCase } from "./verification-runner.mjs";
import { chromiumBinaries, startSite, work } from "./verification-site.mjs";

const { chromium } = await import(
  pathToFileURL(resolve(work, "browser/node_modules/playwright/index.mjs"))
);

await runCase("on-demand audio in a lazy topic list", async () => {
  const site = ownSite(await startSite());
  try {
    for (const [name, executablePath] of Object.entries(chromiumBinaries)) {
      await runCase(name, async () => {
        const browser = await launchBrowser(chromium, { executablePath, headless: true });
        try {
          const page = await browser.newPage({ viewport: { width: 900, height: 800 }, locale: "en-US" });
          await installMediaDiagnostics(page);
          const media = "**/fixture.wav";
          let requests = 0;
          let blocked;
          await page.route(media, (route) => {
            requests++;
            blocked = route;
          });
          await page.goto(site.origin + "/compose/?loadOnPlay=true");
          const play = page.locator('[data-appium-action="true"][data-appium-text="Play"]');
          const pause = page.locator('[data-appium-action="true"][data-appium-text="Pause"]');
          await play.waitFor();
          await page.mouse.move(300, 350);
          await page.mouse.wheel(0, 2600);
          await play.waitFor({ state: "detached" });
          await page.mouse.wheel(0, -3000);
          await play.waitFor();
          assert.equal(requests, 0);
          assert.equal(await page.evaluate(() => mediaTest.workers.length), 0);

          const requested = page.waitForRequest(media);
          await toggleMediaPlayback(page);
          await requested;
          await page.mouse.wheel(0, 2600);
          await page.waitForFunction(() => mediaTest.workers[0]?.closed);
          await page.waitForFunction(() => mediaTest.contexts[0]?.state === "closed");
          await page.unroute(media);
          await blocked?.abort().catch(() => {});
          const cancelledRequests = requests;
          page.on("request", (request) => {
            if (request.url().endsWith("/fixture.wav")) requests++;
          });
          await page.mouse.wheel(0, -3000);
          await play.waitFor();
          assert.equal(requests, cancelledRequests);
          assert.equal(await page.evaluate(() => mediaTest.workers.length), 1);

          await toggleMediaPlayback(page);
          await pause.waitFor();
          await page.waitForFunction(() =>
            mediaTest.contexts[1]?.state === "running" && mediaTest.connected.size > 0
          );
          assert.ok(requests > cancelledRequests);
        } finally {
          await browser.close();
        }
      });
    }
  } finally {
    await site.close();
  }
});
