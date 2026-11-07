/* This is a private project. All rights reserved. */
import { pathToFileURL } from "node:url";
import {
  superviseVerification,
  reportSuites,
} from "./verification-supervisor.mjs";
import { mkdir, readFile, writeFile } from "node:fs/promises";
import { resolve } from "node:path";
import {
  createReportRun,
  collectReportRun,
  writeRunManifest,
} from "./verification-reports.mjs";

const source = import.meta.dirname;
const batches = [
  [
    "host",
    [
      "browser",
      "firefox",
      "worker-regressions",
      "rgba-failure",
      "bitmap-ownership",
      "gpu-lifecycle",
      "player-expansion",
    ],
    {},
  ],
  ...["rgba-copy", "gpu-texture"].flatMap((path) => [
    [
      `compose-${path}`,
      ["compose", "compose-firefox"],
      { MEDIA_RENDER_PATH: path },
    ],
    ...[0, 200].map((delay) => [
      `sync-${path}-${delay}`,
      ["av-sync"],
      {
        MEDIA_RENDER_PATH: path,
        MEDIA_SYNC_OUTPUT_DELAY_MS: String(delay),
      },
    ]),
  ]),
];
export async function runBrowserBatches(
  batches,
  { sourceDirectory = source, raw, destination, workerBudgets } = {},
) {
  const run = await createReportRun(raw);
  const results = [];
  let failed = false;
  for (const [name, scripts, environment] of batches) {
    const reports = resolve(run, name);
    await mkdir(reports, { recursive: true });
    const childEnvironment = {
      ...process.env,
      ...environment,
      MEDIA_REPORT_DIR: reports,
    };
    delete childEnvironment.NODE_TEST_CONTEXT;
    const suites = [];
    const xml = [];
    for (const script of scripts) {
      const result = await superviseVerification(
        resolve(sourceDirectory, `verify-${script}.mjs`),
        resolve(reports, "workers", script),
        childEnvironment,
        workerBudgets,
      );
      const { report, ...outcome } = result;
      suites.push({ script, ...outcome });
      xml.push(reportSuites(await readFile(report, "utf8")));
    }
    await writeFile(
      resolve(reports, "junit.xml"),
      `<?xml version="1.0"?><testsuites>${xml.join("\n")}</testsuites>`,
    );
    const result = {
      name,
      code: suites.some(
        (suite) => suite.code !== 0 || suite.error || suite.termination,
      )
        ? 1
        : 0,
      suites,
    };
    results.push(result);
    if (result.code !== 0) failed = true;
    await writeRunManifest(run, results);
  }
  const collected = await collectReportRun(run, destination);
  return { failed, raw: run, collected, batches: results };
}

if (
  process.argv[1] &&
  import.meta.url === pathToFileURL(process.argv[1]).href
) {
  const requested = new Set(process.argv.slice(2));
  const known = new Set(batches.flatMap((batch) => batch[1]));
  for (const name of requested)
    if (!known.has(name))
      throw new Error(`Unknown verification suite: ${name}`);
  const selected = batches
    .map(([name, scripts, environment]) => [
      name,
      scripts.filter((script) => requested.size === 0 || requested.has(script)),
      environment,
    ])
    .filter((batch) => batch[1].length > 0);
  const result = await runBrowserBatches(selected);
  console.info(`Raw reports: ${result.raw}`);
  console.info(`Collected reports: ${result.collected}`);
  process.exitCode = result.failed ? 1 : 0;
}
