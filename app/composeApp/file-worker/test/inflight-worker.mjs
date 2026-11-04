/* This is a private project. All rights reserved. */
import '../worker.mjs';
import { observeOpfsWrites } from './stream-probe.mjs';
import { cancelPendingWrite } from './cancel-pending-write.mjs';

const handleMessage = self.onmessage;
let observed = false;
observeOpfsWrites(async (write, bytes, name) => {
  const pending = write(bytes);
  if (!observed) {
    observed = true;
    let cancelled = false;
    const reportSettlement = () => self.postMessage({probe: 'native-settled', afterCancellation: cancelled});
    pending.then(reportSettlement, reportSettlement);
    await cancelPendingWrite(pending, () => {
      cancelled = true;
      // Invoke the unchanged production cancellation handler before native write completion.
      handleMessage.call(self, {data: {op: 'cancel'}});
      self.postMessage({probe: 'inflight', bytes: bytes.byteLength, name, pendingAtCancellation: true});
    });
  }
  await pending;
});
