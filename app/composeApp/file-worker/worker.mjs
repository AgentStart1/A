/* This is a private project. All rights reserved. */
import { extractArchive } from './archive.mjs';

const controller = new AbortController();
let running = false;
self.onmessage = async ({data}) => {
  if (data.op === 'cancel') {
    controller.abort();
    return;
  }
  if (data.op !== 'extract' || running) return;
  running = true;
  try {
    if (!(self instanceof DedicatedWorkerGlobalScope)) throw new Error('File processing requires a worker');
    const entries = await extractArchive(data.path, data.directory, controller.signal);
    self.postMessage({result: JSON.stringify(entries)});
  } catch (failure) {
    self.postMessage({error: failure?.message ?? String(failure)});
  }
};
