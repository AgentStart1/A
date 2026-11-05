/* This is a private project. All rights reserved. */
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createExtractionJob } from './host.mjs';

function worker() {
  return {
    messages: [], terminated: false,
    postMessage(data) { this.messages.push(data); },
    terminate() { this.terminated = true; },
  };
}

test('completion releases the worker and returns the extraction manifest', async () => {
  const target = worker();
  const job = createExtractionJob(target, 'downloads/test/source', 'downloads/test/source.extracted');
  assert.equal(target.messages[0].op, 'extract');
  target.onmessage({data: {result: '[{"path":"entry","size":3}]'}});
  assert.equal(await job.result(), '[{"path":"entry","size":3}]');
  await job.close();
  assert.equal(target.terminated, true);
  assert.equal(target.onmessage, null);
  assert.equal(target.messages.length, 1);
});

test('cancellation waits for the worker to acknowledge closed file handles', async () => {
  const target = worker();
  const job = createExtractionJob(target, 'source', 'directory');
  let closed = false;
  const closing = job.close().then(() => { closed = true; });
  await Promise.resolve();
  assert.equal(target.messages.at(-1).op, 'cancel');
  assert.equal(closed, false);
  assert.equal(target.terminated, false);
  target.onmessage({data: {error: 'Cancelled'}});
  await assert.rejects(job.result(), /Cancelled/);
  await closing;
  assert.equal(closed, true);
  assert.equal(target.terminated, true);
});

test('worker startup failure is propagated and terminates the worker', async () => {
  const target = worker();
  const job = createExtractionJob(target, 'source', 'directory');
  let prevented = false;
  target.onerror({message: 'Worker script failed', preventDefault() { prevented = true; }});
  await assert.rejects(job.result(), /Worker script failed/);
  await job.close();
  assert.equal(prevented, true);
  assert.equal(target.terminated, true);
});

test('invalid worker messages fail rather than leaving a pending operation', async () => {
  const target = worker();
  const job = createExtractionJob(target, 'source', 'directory');
  target.onmessageerror();
  await assert.rejects(job.result(), /Invalid file worker message/);
  await job.close();
  assert.equal(target.terminated, true);
});

test('synchronous postMessage failure releases the worker', async () => {
  const target = worker();
  target.postMessage = () => { throw new Error('Message could not be cloned'); };
  const job = createExtractionJob(target, 'source', 'directory');
  await assert.rejects(job.result(), /Message could not be cloned/);
  await job.close();
  assert.equal(target.terminated, true);
});

test('malformed response data cannot leave a pending operation', async () => {
  const target = worker();
  const job = createExtractionJob(target, 'source', 'directory');
  target.onmessage({data: null});
  await assert.rejects(job.result(), /Invalid file worker response/);
  await job.close();
  assert.equal(target.terminated, true);
});

test('empty worker error messages are still failures', async () => {
  const target = worker();
  const job = createExtractionJob(target, 'source', 'directory');
  target.onmessage({data: {error: ''}});
  await assert.rejects(job.result(), /File worker failed/);
  await job.close();
  assert.equal(target.terminated, true);
});
