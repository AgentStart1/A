/* This is a private project. All rights reserved. */
import {
  toggleFirefoxPlayback,
  toggleFirefoxMute,
  seekFirefoxMidpoint,
} from "./media-controls.mjs";
import {
  runCase,
  launchFirefoxDriver,
  ownSite,
} from "./verification-runner.mjs";
import { getReportDirectory } from "./verification-reports.mjs";
import assert from "node:assert/strict";
import { resolve } from "node:path";
import { pathToFileURL } from "node:url";
import { mkdir, writeFile } from "node:fs/promises";
import { createHash } from "node:crypto";
import { startSite, work } from "./verification-site.mjs";
import { installMediaDiagnostics } from "./verification-instrumentation.mjs";
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
const { PNG } = await import(
  pathToFileURL(resolve(work, "browser/node_modules/pngjs/lib/png.js"))
);
assert.ok(
  process.env.MEDIA_GECKODRIVER,
  "Set MEDIA_GECKODRIVER to an existing geckodriver.",
);
await runCase("verify-compose-firefox", async () => {
  const site = ownSite(await startSite());
  const profileRoot = resolve(work, "firefox-profiles");
  await mkdir(profileRoot, { recursive: true });
  let driver;
  const results = [];
  const mediaPath = process.env.MEDIA_RENDER_PATH ?? "rgba-copy";
  const prefix = mediaPath === "gpu-texture" ? "gpu" : "compose";
  const canvasScript = `function findCanvas(root=document){const c=root.querySelector('canvas');if(c)return c;for(const e of root.querySelectorAll('*')){if(e.shadowRoot){const found=findCanvas(e.shadowRoot);if(found)return found;}}return null;}`;
  try {
    driver = await launchFirefoxDriver(selenium, {
      executable: process.env.MEDIA_GECKODRIVER,
      profileRoot,
      options: new firefox.Options()
        .addArguments("-headless")
        .setPreference("intl.accept_languages", "en-US,en"),
    });
    const version = (await driver.getCapabilities()).get("browserVersion");
    const evaluate = (script) => driver.executeScript(script);
    const wait = (script) =>
      driver.wait(async () => {
        const errors = await evaluate("return window.mediaTest?.errors??[]");
        assert.deepEqual(errors, []);
        return evaluate(script);
      }, 30000);
    const settle = () =>
      driver.executeAsyncScript(
        "const done=arguments[arguments.length-1];requestAnimationFrame(()=>requestAnimationFrame(()=>done()))",
      );
    const click = (x, y) =>
      driver.actions().move({ origin: "viewport", x, y }).click().perform();
    const shot = async () =>
      Buffer.from(await driver.takeScreenshot(), "base64");
    function regionHash(bytes) {
      const image = PNG.sync.read(bytes),
        scale = image.width / 900;
      const chunks = [];
      for (let y = Math.round(100 * scale); y < Math.round(220 * scale); y++)
        chunks.push(
          image.data.subarray(
            (y * image.width + Math.round(145 * scale)) * 4,
            (y * image.width + Math.round(395 * scale)) * 4,
          ),
        );
      return createHash("sha256").update(Buffer.concat(chunks)).digest("hex");
    }
    for (const [kind, query, width, height] of [
      ["av", "", 320, 180],
      ["hd", "?media=/fixture-hd.mp4", 1280, 720],
      ["fhd", "?media=/fixture-fhd.mp4", 1920, 1080],
      ["audio", "?audio=true", 0, 0],
    ]) {
      await runCase(`Compose ${mediaPath} ${kind}`, async () => {
        await driver.get(
          site.origin +
            "/compose-bootstrap.html" +
            (query || "?") +
            (query ? "&" : "") +
            "mediaPath=" +
            mediaPath,
        );
        await driver.manage().window().setRect({ width: 900, height: 800 });
        const chrome = await evaluate(
          "return {width:outerWidth-innerWidth,height:outerHeight-innerHeight}",
        );
        await driver
          .manage()
          .window()
          .setRect({ width: 900 + chrome.width, height: 800 + chrome.height });
        assert.deepEqual(
          await evaluate("return {width:innerWidth,height:innerHeight}"),
          { width: 900, height: 800 },
        );
        await installMediaDiagnostics({
          addInitScript: (fn) => driver.executeScript(`(${fn.toString()})()`),
        });
        await evaluate(
          `mediaTest.errors=[];addEventListener('error',e=>mediaTest.errors.push(e.message));addEventListener('unhandledrejection',e=>mediaTest.errors.push(String(e.reason)));const script=document.createElement('script');script.src='/compose/wasmMediaExperiment.js';document.body.append(script);`,
        );
        await wait(
          `return ${kind === "audio" ? "mediaTest.audio" : "mediaTest.decoded"}.length>0`,
        );
        await settle();
        await evaluate(canvasScript + "mediaTest.canvas=findCanvas();");
        assert.deepEqual(
          await evaluate(
            canvasScript +
              `const r=findCanvas().getBoundingClientRect();return {width:r.width,height:r.height};`,
          ),
          { width: 900, height: 800 },
        );
        await toggleFirefoxPlayback(driver);
        await wait("return mediaTest.contexts.some(c=>c.state==='running')");
        const first = await shot();
        await new Promise((r) => setTimeout(r, 1200));
        const second = await shot();
        if (kind !== "audio")
          assert.notEqual(regionHash(first), regionHash(second));
        const measured = await driver.executeAsyncScript(
          `const done=arguments[arguments.length-1];const context=mediaTest.contexts.at(-1),gain=mediaTest.gains.at(-1),a=context.createAnalyser();gain.connect(a);setTimeout(()=>{const b=new Float32Array(a.fftSize);a.getFloatTimeDomainData(b);gain.disconnect(a);done({rms:Math.sqrt(b.reduce((s,x)=>s+x*x,0)/b.length),decoded:mediaTest.decoded.length,uploads:mediaTest.uploads.filter(u=>u.w===${width}&&u.h===${height}).length})},100);`,
        );
        assert.ok(measured.rms > 0.01);
        if (kind !== "audio") {
          assert.ok(measured.decoded > 10);
          assert.ok(measured.uploads > 10);
        }
        await toggleFirefoxMute(driver);
        assert.equal(
          await evaluate("return mediaTest.gains.at(-1).gain.value"),
          0,
        );
        await toggleFirefoxMute(driver);
        assert.equal(
          await evaluate("return mediaTest.gains.at(-1).gain.value"),
          1,
        );
        const pausedWorker = await evaluate("return mediaTest.workers.length");
        await toggleFirefoxPlayback(driver);
        await wait(
          `return mediaTest.connected.size===0&&${kind === "audio" ? "mediaTest.audio" : "mediaTest.decoded"}.at(-1)?.worker===${pausedWorker}`,
        );
        await settle();
        if (kind !== "audio") {
          const paused = await shot();
          await new Promise((r) => setTimeout(r, 180));
          assert.equal(regionHash(paused), regionHash(await shot()));
          const target = kind === "av" ? 3 : 2;
          await seekFirefoxMidpoint(driver);
          await wait(
            `return Math.abs(mediaTest.decoded.at(-1).timestamp-${target})<0.2`,
          );
          await settle();
          const before = PNG.sync.read(await shot());
          await click(545, 112);
          await new Promise((r) => setTimeout(r, 200));
          await writeFile(
            resolve(
              await getReportDirectory(),
              `${prefix}-firefox-${kind}-menu.png`,
            ),
            await shot(),
          );
          await click(545, 160);
          await new Promise((r) => setTimeout(r, 200));
          const dialog = await shot();
          await writeFile(
            resolve(
              await getReportDirectory(),
              `${prefix}-firefox-${kind}-dialog.png`,
            ),
            dialog,
          );
          assert.notEqual(before.data[12], PNG.sync.read(dialog).data[12]);
          await driver.actions().sendKeys(selenium.Key.ESCAPE).perform();
          await settle();
          await driver.wait(async () => {
            const image = PNG.sync.read(await shot()),
              scale = image.width / 900,
              o =
                (Math.round(90 * scale) * image.width +
                  Math.round(18 * scale)) *
                4;
            return (
              image.data[o] > 240 &&
              image.data[o + 1] > 240 &&
              image.data[o + 2] > 240
            );
          }, 30000);
          const clipped = PNG.sync.read(await shot()),
            scale = clipped.width / 900,
            offset =
              (Math.round(90 * scale) * clipped.width +
                Math.round(18 * scale)) *
              4;
          assert.ok(
            clipped.data[offset] > 240 &&
              clipped.data[offset + 1] > 240 &&
              clipped.data[offset + 2] > 240,
          );
        }
        await writeFile(
          resolve(await getReportDirectory(), `${prefix}-firefox-${kind}.png`),
          await shot(),
        );
        await toggleFirefoxPlayback(driver);
        await wait("return mediaTest.contexts.at(-1).state==='running'");
        const nextWorker = await evaluate("return mediaTest.workers.length");
        await click(235, 64);
        await wait(
          `return ${kind === "audio" ? "mediaTest.audio" : "mediaTest.decoded"}.at(-1)?.worker===${nextWorker}`,
        );
        await wait(
          "return mediaTest.contexts.every(c=>c.state==='closed')&&mediaTest.connected.size===0",
        );
        if (kind !== "audio") {
          assert.ok(
            await evaluate("return mediaTest.decoded.at(-1).timestamp<0.1"),
          );
          assert.equal(
            await evaluate("return mediaTest.decoded.at(-1).bytes"),
            mediaPath === "gpu-texture" ? 0 : 320 * 180 * 4,
          );
        }
        await click(90, 64);
        await wait(
          "return mediaTest.workers.every(w=>w.closed)&&mediaTest.contexts.every(c=>c.state==='closed')&&mediaTest.connected.size===0&&mediaTest.transferredFrames.size===0&&mediaTest.frameTextures.size===0",
        );
        if (mediaPath === "gpu-texture" && kind !== "audio")
          assert.equal(
            await evaluate(
              "return mediaTest.frameCanvases.size===1&&mediaTest.frameCanvases.has(mediaTest.canvas)",
            ),
            true,
          );
        results.push({
          path: mediaPath,
          browser: "firefox",
          version,
          kind,
          ...measured,
          captureMethod:
            "WebDriver PNG comparison and native Skia upload observation",
          disposed: true,
        });
      });
    }
    await writeFile(
      resolve(await getReportDirectory(), `${prefix}-firefox-results.json`),
      JSON.stringify(results, null, 2),
    );
    console.log(JSON.stringify(results, null, 2));
  } finally {
    if (driver) await driver.quit();
    await site.close();
  }
});
