/* This is a private project. All rights reserved. */
import { createExtractionJob } from '../host.mjs';

export default function startPausedExtraction(path, directory) {
  const worker = new Worker(new URL('./write-worker.mjs', import.meta.url), {type: 'module'});
  return createWriteProbe(worker, path, directory);
}

export function createWriteProbe(worker, path, directory) {
  let onmessage, written, closed, rejectWritten, rejectClosed;
  let streamClosed = false, pendingAtCancellation = false, nativeSettledAfterCancellation = false;
  const writing = new Promise((resolve, reject) => { written = resolve; rejectWritten = reject; });
  const closing = new Promise((resolve, reject) => { closed = resolve; rejectClosed = reject; });
  writing.catch(() => {});
  closing.catch(() => {});
  worker.onmessage = event => {
    if (event.data?.probe === 'written' || event.data?.probe === 'inflight') {
      pendingAtCancellation = event.data.pendingAtCancellation === true;
      written(event.data);
    }
    else if (event.data?.probe === 'native-settled') {
      nativeSettledAfterCancellation = event.data.afterCancellation === true;
    }
    else if (event.data?.probe === 'closed') {
      streamClosed = true;
      closed(event.data.kind);
    }
    else onmessage?.(event);
  };
  // The production protocol still handles all terminal messages and cancellation.
  const bridge = {
    set onmessage(handler) { onmessage = handler; },
    set onerror(handler) { worker.onerror = handler; },
    set onmessageerror(handler) { worker.onmessageerror = handler; },
    postMessage: data => worker.postMessage(data),
    terminate() {
      worker.terminate();
      rejectWritten(new Error('Worker stopped before the write probe completed'));
      rejectClosed(new Error('Worker stopped before the close probe completed'));
    },
  };
  const job = createExtractionJob(bridge, path, directory);
  return {
    ...job, writeObserved: () => writing, closed: () => closing, isClosed: () => streamClosed,
    pendingAtCancellation: () => pendingAtCancellation,
    nativeSettledAfterCancellation: () => nativeSettledAfterCancellation,
  };
}
