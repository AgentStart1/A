/* This is a private project. All rights reserved. */
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { cancelPendingWrite } from './test/cancel-pending-write.mjs';

test('already fulfilled write cannot be reported as pending cancellation', async () => {
  let cancelled = false;
  await assert.rejects(cancelPendingWrite(Promise.resolve(), () => { cancelled = true; }), /settled before cancellation/);
  assert.equal(cancelled, false);
});

test('write completing in a microtask before cancellation fails the pending guard', async () => {
  let cancelled = false;
  const pending = Promise.resolve().then(() => {});
  await assert.rejects(cancelPendingWrite(pending, () => { cancelled = true; }), /settled before cancellation/);
  assert.equal(cancelled, false);
});

test('pending write is cancelled before its completion', async () => {
  let finish;
  const pending = new Promise(resolve => { finish = resolve; });
  const order = [];
  pending.then(() => order.push('settled'));
  await cancelPendingWrite(pending, () => { order.push('cancel'); finish(); });
  await pending;
  assert.deepEqual(order, ['cancel', 'settled']);
});

test('an already rejected native write remains a failure', async () => {
  const failure = new Error('Native write failed');
  let cancelled = false;
  await assert.rejects(cancelPendingWrite(Promise.reject(failure), () => { cancelled = true; }), failure);
  assert.equal(cancelled, false);
});
