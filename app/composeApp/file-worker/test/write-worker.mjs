/* This is a private project. All rights reserved. */
import '../worker.mjs';
import { observeOpfsWrites } from './stream-probe.mjs';

// Test entry only: retain the real OPFS writer after a completed native write.
let resumeWrite;
const paused = new Promise(resolve => { resumeWrite = resolve; });
let observed = false;
const handleMessage = self.onmessage;
self.onmessage = event => {
  const handling = handleMessage.call(self, event);
  // Deliver cancellation to the real AbortController before unblocking the write.
  if (event.data.op === 'cancel') resumeWrite();
  return handling;
};
observeOpfsWrites(async (write, bytes, name) => {
  await write(bytes);
  if (!observed) {
    observed = true;
    self.postMessage({probe: 'written', bytes: bytes.byteLength, name});
    await paused;
  }
});
