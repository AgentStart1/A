/* This is a private project. All rights reserved. */
import { spawn } from "node:child_process";
import { mkdir, readFile, writeFile } from "node:fs/promises";
import { basename, resolve } from "node:path";
import { constants } from "node:os";
import { randomUUID } from "node:crypto";
import { setTimeout as delay } from "node:timers/promises";

const escapeXml = (value) =>
  String(value).replace(
    /[<>&"']/g,
    (c) =>
      ({
        "<": "&lt;",
        ">": "&gt;",
        "&": "&amp;",
        '"': "&quot;",
        "'": "&apos;",
      })[c],
  );

export async function superviseVerification(
  file,
  directory,
  environment,
  {
    executionTimeoutMs = 300000,
    exitTimeoutMs = 5000,
    killTimeoutMs = 5000,
  } = {},
) {
  await mkdir(directory, { recursive: true });
  const report = resolve(directory, "junit.xml.part");
  const stdout = [],
    stderr = [];
  const token = randomUUID();
  const statusFile = resolve(directory, "container.jsonl");
  const completionFile = resolve(directory, "completed.json");
  const python =
    environment.PYTHON_COMMAND ??
    (process.platform === "win32" ? "python" : "python3");
  const child = spawn(
    python,
    [
      resolve(import.meta.dirname, "verification-container.py"),
      "--status-file",
      statusFile,
      "--token",
      token,
      "--timeout-ms",
      String(killTimeoutMs),
      "--parent-pid",
      String(process.pid),
      "--command",
      process.execPath,
      "--test-reporter=spec",
      "--test-reporter-destination=stdout",
      "--test-reporter=junit",
      `--test-reporter-destination=${report}`,
      resolve(import.meta.dirname, "verification-worker.mjs"),
      file,
    ],
    {
      env: {
        ...environment,
        MEDIA_VERIFY_SUITE: basename(file, ".mjs"),
        MEDIA_WORKER_TOKEN: token,
        MEDIA_WORKER_COMPLETION_FILE: completionFile,
      },
      windowsHide: true,
      stdio: ["pipe", "pipe", "pipe"],
    },
  );
  child.stdout.on("data", (data) => {
    stdout.push(data);
    process.stdout.write(data);
  });
  child.stderr.on("data", (data) => {
    stderr.push(data);
    process.stderr.write(data);
  });
  child.stdin.on("error", () => {}); // State acknowledgement, not pipe delivery, proves release.
  let startupError;
  child.once("error", (error) => {
    startupError = error.message;
  });
  let closed;
  const exited = new Promise((resolve) =>
    child.once("close", (code, signal) => {
      closed = { code, signal };
      resolve(closed);
    }),
  );
  const started = performance.now();
  let finishDeadline;
  let releaseDeadline;
  let termination;
  let state;
  let result;
  const readState = async (path, journal = false) => {
    try {
      let text = await readFile(path, "utf8");
      if (journal) {
        const last = text.lastIndexOf("\n");
        if (last < 0) return undefined;
        text = text.slice(0, last).split("\n").at(-1);
      }
      const value = JSON.parse(text);
      return value.token === token ? value : undefined;
    } catch (error) {
      if (error.code === "ENOENT") return undefined;
      throw error;
    }
  };
  const waitForClose = async () => {
    let timer;
    try {
      return await Promise.race([
        exited,
        new Promise((_, reject) => {
          timer = setTimeout(
            () =>
              reject(
                new Error("Container released but output pipes did not close"),
              ),
            killTimeoutMs,
          );
        }),
      ]);
    } finally {
      clearTimeout(timer);
    }
  };
  try {
    while (true) {
      state = await readState(statusFile, true);
      if (state?.phase === "error") throw new Error(state.error);
      if (state?.phase === "released") {
        const ownerExit = await waitForClose();
        if (ownerExit.code !== 0)
          throw new Error(
            `Container owner exited ${ownerExit.code} after release`,
          );
        if (state.root_code == null)
          throw new Error("Released container did not report root exit");
        result = {
          code: state.root_code >= 0 ? state.root_code : null,
          signal:
            state.root_code < 0
              ? (Object.keys(constants.signals).find(
                  (name) => constants.signals[name] === -state.root_code,
                ) ?? `signal-${-state.root_code}`)
              : null,
        };
        if (termination) result.termination = termination;
        break;
      }
      if (closed) {
        // The last read may have raced the owner's final append. After close,
        // all records are final; re-read before declaring acknowledgement lost.
        const final = await readState(statusFile, true);
        if (final?.phase === "released") continue;
        if (final?.phase === "error") throw new Error(final.error);
        if (startupError) {
          result = { ...closed, error: startupError };
          break;
        }
        throw new Error(
          `Container owner exited without confirming resource release (${closed.code})`,
        );
      }
      const now = performance.now();
      const flowFinished = await readState(completionFile);
      if (
        finishDeadline === undefined &&
        (flowFinished || state?.root_code != null)
      )
        finishDeadline = now + exitTimeoutMs;
      const expired =
        finishDeadline === undefined
          ? now - started >= executionTimeoutMs
          : now >= finishDeadline;
      if (!termination && expired) {
        termination =
          finishDeadline === undefined ? "execution-timeout" : "exit-timeout";
        // The owner remains alive after root exit and kills its job/adopted
        // descendants. Never infer container release from a root PID or a pipe.
        child.stdin.write("terminate\n");
        releaseDeadline = now + killTimeoutMs + 250;
      }
      if (releaseDeadline !== undefined && now >= releaseDeadline)
        throw new Error("Container did not acknowledge resource release");
      await delay(20);
    }
  } catch (error) {
    try {
      const signal = AbortSignal.timeout(killTimeoutMs);
      await Promise.allSettled([
        writeFile(
          resolve(directory, "supervisor-error.json"),
          JSON.stringify(
            {
              ownerPid: child.pid,
              rootPid: state?.root_pid,
              termination,
              state,
              error: error.stack,
            },
            null,
            2,
          ),
          { signal },
        ),
        writeFile(resolve(directory, "stdout.log"), Buffer.concat(stdout), {
          signal,
        }),
        writeFile(resolve(directory, "stderr.log"), Buffer.concat(stderr), {
          signal,
        }),
      ]);
    } finally {
      // EOF asks the surviving owner to release its container. If release cannot
      // be confirmed, abort the whole run; never start another module.
      child.stdin.destroy();
      child.stdout.destroy();
      child.stderr.destroy();
      child.unref();
    }
    throw new Error(
      `Cannot confirm release of verification container ${child.pid}; evidence: ${directory}`,
      { cause: error },
    );
  } finally {
    child.stdin.end();
  }
  if (startupError) result.error = startupError;
  await writeFile(resolve(directory, "stdout.log"), Buffer.concat(stdout));
  await writeFile(resolve(directory, "stderr.log"), Buffer.concat(stderr));
  let xml = "";
  try {
    xml = await readFile(report, "utf8");
  } catch (error) {
    if (error.code !== "ENOENT") throw error;
  }
  const completeReport = xml.trimEnd().endsWith("</testsuites>");
  const exitFailure = result.signal
    ? `Worker exited with signal ${result.signal}`
    : result.code !== 0
      ? `Worker exited with code ${result.code}`
      : undefined;
  const failure =
    result.termination ??
    startupError ??
    exitFailure ??
    (completeReport ? undefined : "Worker did not finish its JUnit report");
  if (failure) {
    // Process status is an independent contract. Preserve completed test cases
    // and append its failure, even when the test reporter already finished.
    const completedSuites = completeReport ? reportSuites(xml) : "";
    const lifecycle = resolve(directory, "lifecycle.xml.part");
    await writeFile(
      lifecycle,
      `<?xml version="1.0"?><testsuites>${completedSuites}<testsuite name="worker lifecycle" tests="1" failures="1"><testcase name="${escapeXml(file)}"><failure message="${escapeXml(failure)}">${escapeXml(failure)}; original output and partial JUnit are retained alongside this report.</failure></testcase></testsuite></testsuites>`,
    );
    result = { ...result, error: failure, report: lifecycle };
  } else result.report = report;
  return result;
}

export function reportSuites(xml) {
  const match = xml.match(
    /^\s*(?:<\?xml[^?]*\?>\s*)?<testsuites[^>]*>([\s\S]*)<\/testsuites>\s*$/,
  );
  if (!match) throw new Error("Invalid worker JUnit envelope");
  return match[1];
}
