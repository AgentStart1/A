/* This is a private project. All rights reserved. */
// No timers/task yield: native filesystem completion is delivered on a storage task.
// The race and settlement observer reject already-completed writes instead of skipping.
export async function cancelPendingWrite(pending, cancel) {
  let settled = false;
  pending.then(() => { settled = true; }, () => { settled = true; });
  const waiting = {};
  const state = await Promise.race([pending, Promise.resolve(waiting)]);
  if (state !== waiting || settled) throw new Error('Native write settled before cancellation point');
  cancel();
}
