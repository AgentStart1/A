/* This is a private project. All rights reserved. */
import assert from "node:assert/strict";
import { resolve } from "node:path";
import { pathToFileURL } from "node:url";
import { writeFile } from "node:fs/promises";
import { getReportDirectory } from "./verification-reports.mjs";
import {
  runCase,
  launchBrowser,
  launchFirefoxDriver,
  ownSite,
} from "./verification-runner.mjs";
import { startSite, work, chromiumBinaries } from "./verification-site.mjs";
import { installMediaDiagnostics } from "./verification-instrumentation.mjs";
const { chromium } = await import(
  pathToFileURL(resolve(work, "browser/node_modules/playwright/index.mjs"))
);
const selenium = await import(
  pathToFileURL(
    resolve(work, "browser/node_modules/selenium-webdriver/index.js"),
  )
);
const firefox = await import(
  pathToFileURL(
    resolve(work, "browser/node_modules/selenium-webdriver/firefox.js"),
  )
);
assert.ok(
  process.env.MEDIA_GECKODRIVER,
  "Set MEDIA_GECKODRIVER to an existing geckodriver.",
);
const output = await getReportDirectory();
const stats =
  "({workers:mediaTest.workers.length, active:mediaTest.workers.filter(w=>!w.closed).length, contexts:mediaTest.contexts.length, decoded:mediaTest.decoded.length})";
async function exercise(ui, name) {
  await ui.wait("mediaTest.decoded.length > 0");
  await ui.click("Play");
  await ui.wait("mediaTest.contexts.some(c=>c.state==='running')");
  const before = await ui.read(stats);
  await ui.click("Expand player");
  await ui.wait(
    `document.querySelector('[data-appium-text="Exit expanded player"]') !== null`,
  );
  await ui.wait(`mediaTest.decoded.length > ${before.decoded + 3}`);
  await ui.shot(resolve(output, name + ".png"));
  const expanded = await ui.read(stats);
  assert.equal(expanded.workers, before.workers);
  assert.equal(expanded.contexts, before.contexts);
  assert.equal(expanded.active, 1);
  await ui.click("Exit expanded player");
  await ui.wait(
    `document.querySelector('[data-appium-text="Expand player"]') !== null`,
  );
  const collapsed = await ui.read(stats);
  assert.equal(collapsed.workers, before.workers);
  assert.equal(collapsed.contexts, before.contexts);
  assert.equal(collapsed.active, 1);
}
await runCase("verify-player-expansion", async () => {
  const site = ownSite(await startSite());
  try {
    for (const [name, executablePath] of Object.entries(chromiumBinaries)) {
      await runCase(name, async () => {
        const browser = await launchBrowser(chromium, {
          executablePath,
          headless: true,
        });
        try {
          for (const path of ["rgba-copy", "gpu-texture"])
            await runCase(path, async () => {
              const page = await browser.newPage({
                viewport: { width: 900, height: 800 },
                locale: "en-US",
              });
              await installMediaDiagnostics(page);
              await page.goto(site.origin + "/compose/?mediaPath=" + path);
              await exercise(
                {
                  wait: (e) => page.waitForFunction(e),
                  read: (e) => page.evaluate(e),
                  click: (label) =>
                    page
                      .locator(
                        `[data-appium-action="true"][data-appium-text="${label}"]`,
                      )
                      .click(),
                  shot: (file) => page.screenshot({ path: file }),
                },
                name + "-" + path,
              );
              await page.close();
            });
        } finally {
          await browser.close();
        }
      });
    }
    await runCase("firefox", async () => {
      const driver = await launchFirefoxDriver(selenium, {
        executable: process.env.MEDIA_GECKODRIVER,
        profileRoot: resolve(work, "firefox-profiles"),
        options: new firefox.Options()
          .addArguments("-headless")
          .setPreference("intl.accept_languages", "en-US,en"),
      });
      try {
        for (const path of ["rgba-copy", "gpu-texture"])
          await runCase(path, async () => {
            await driver.get(
              site.origin + "/compose-bootstrap.html?mediaPath=" + path,
            );
            await driver.manage().window().setRect({ width: 900, height: 800 });
            await installMediaDiagnostics({
              addInitScript: (fn) =>
                driver.executeScript(`(${fn.toString()})()`),
            });
            await driver.executeScript(
              "const s=document.createElement('script');s.src='/compose/wasmMediaExperiment.js';document.body.append(s)",
            );
            await exercise(
              {
                wait: (e) =>
                  driver.wait(() => driver.executeScript("return " + e), 30000),
                read: (e) => driver.executeScript("return " + e),
                click: async (label) => {
                  const element = await driver.wait(
                    async () =>
                      (
                        await driver.findElements({
                          css: `[data-appium-action="true"][data-appium-text="${label}"]`,
                        })
                      )[0],
                    30000,
                  );
                  await element.click();
                },
                shot: async (file) => {
                  await writeFile(
                    file,
                    Buffer.from(await driver.takeScreenshot(), "base64"),
                  );
                },
              },
              "firefox-" + path,
            );
          });
      } finally {
        await driver.quit();
      }
    });
  } finally {
    await site.close();
  }
});
