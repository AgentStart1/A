/* This is a private project. All rights reserved. */
// The isolated Compose fixture explicitly enables the test-only semantics bridge.
const control = (labels) =>
  labels
    .map((label) => `[data-appium-action="true"][data-appium-text="${label}"]`)
    .join(",");

export async function toggleMediaPlayback(page) {
  await page.locator(control(["Play", "Pause"])).click();
}

export async function toggleMediaMute(page) {
  await page.locator(control(["Volume"])).click();
  await page.locator(control(["Mute", "Unmute"])).click();
}

async function firefoxControl(driver, labels) {
  const element = await driver.wait(
    async () => (await driver.findElements({ css: control(labels) }))[0],
    30000,
  );
  await element.click();
}

export async function toggleFirefoxPlayback(driver) {
  await firefoxControl(driver, ["Play", "Pause"]);
}

export async function toggleFirefoxMute(driver) {
  await firefoxControl(driver, ["Volume"]);
  await firefoxControl(driver, ["Mute", "Unmute"]);
}

export async function seekMediaMidpoint(page) {
  const bounds = await page
    .locator('[data-testid="media-position"]')
    .boundingBox();
  if (!bounds) throw new Error("Playback position is not visible");
  await page.mouse.click(
    bounds.x + bounds.width / 2,
    bounds.y + bounds.height / 2,
  );
}

export async function seekFirefoxMidpoint(driver) {
  const element = await driver.findElement({
    css: '[data-testid="media-position"]',
  });
  const bounds = await element.getRect();
  await driver
    .actions()
    .move({
      origin: "viewport",
      x: Math.round(bounds.x + bounds.width / 2),
      y: Math.round(bounds.y + bounds.height / 2),
    })
    .click()
    .perform();
}
