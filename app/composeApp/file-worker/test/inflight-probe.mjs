/* This is a private project. All rights reserved. */
import { createWriteProbe } from './write-probe.mjs';

export default function startInflightExtraction(path, directory) {
  return createWriteProbe(
    new Worker(new URL('./inflight-worker.mjs', import.meta.url), {type: 'module'}),
    path,
    directory,
  );
}
