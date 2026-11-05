/* This is a private project. All rights reserved. */
export function startExtraction(path, directory) {
  return createExtractionJob(
    new Worker(new URL('./worker.mjs', import.meta.url), {type: 'module'}),
    path,
    directory,
  );
}

// One worker owns one operation. Cancellation waits for open writers to close.
export function createExtractionJob(worker, path, directory) {
  let resolve, reject, settled = false;
  const result = new Promise((success, failure) => { resolve = success; reject = failure; });
  const finish = (value, error) => {
    if (settled) return;
    settled = true;
    worker.onmessage = worker.onerror = worker.onmessageerror = null;
    worker.terminate();
    if (error != null) reject(new Error(error || 'File worker failed')); else resolve(value);
  };
  worker.onmessage = ({data}) => {
    if (typeof data?.result !== 'string' && typeof data?.error !== 'string') {
      finish(null, 'Invalid file worker response');
    } else finish(data.result, data.error);
  };
  worker.onerror = event => {
    event.preventDefault();
    finish(null, event.message || 'File worker failed');
  };
  worker.onmessageerror = () => finish(null, 'Invalid file worker message');
  // Observe failure immediately, including errors before Kotlin starts awaiting.
  const completed = result.then(() => {}, () => {});
  try { worker.postMessage({op: 'extract', path, directory}); }
  catch (failure) { finish(null, failure.message || 'Unable to start file worker'); }
  return {
    result: () => result,
    async close() {
      if (!settled) worker.postMessage({op: 'cancel'});
      await completed;
    },
  };
}

export default startExtraction;
