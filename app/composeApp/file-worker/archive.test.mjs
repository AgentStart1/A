/* This is a private project. All rights reserved. */
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { ZipWriter, Uint8ArrayReader, Uint8ArrayWriter } from '@zip.js/zip.js/lib/zip-core-native.js';
import { extractArchive } from './archive.mjs';

async function zip(entries) {
  const writer = new ZipWriter(new Uint8ArrayWriter(), {useWebWorkers: false});
  for (const [name, bytes] of entries) {
    await writer.add(name, new Uint8ArrayReader(bytes), {level: 0});
  }
  return writer.close();
}

function storage(source, onWrite = () => {}) {
  const files = new Map([['downloads/test/source', source]]);
  let open = 0;
  const directory = prefix => ({
    async getDirectoryHandle(name) { return directory(prefix + name + '/'); },
    async getFileHandle(name) {
      const path = prefix + name;
      return {
        async getFile() { return new File([files.get(path)], name); },
        async createWritable() {
          open++;
          const chunks = [];
          let finished = false;
          const finish = () => { if (!finished) { finished = true; open--; } };
          return {
            async write(bytes) { chunks.push(bytes.slice()); onWrite(); },
            async close() { files.set(path, new Uint8Array(await new Blob(chunks).arrayBuffer())); finish(); },
            async abort() { finish(); },
          };
        },
      };
    },
  });
  Object.defineProperty(globalThis, 'navigator', {
    value: {storage: {getDirectory: async () => directory('')}}, configurable: true,
  });
  return {files, get open() { return open; }};
}
const source = 'downloads/test/source';
const generation = source + '.extract-test';

test('archive extraction returns only closed OPFS files with exact bytes', async () => {
  const data = new Uint8Array([1, 2, 3]);
  const target = storage(await zip([['family/Regular.ttf', data]]));
  const entries = await extractArchive(source, generation, new AbortController().signal);
  assert.deepEqual(entries, [{path: generation + '/family/Regular.ttf', size: 3}]);
  assert.deepEqual(target.files.get(entries[0].path), data);
  assert.equal(target.open, 0);
});

test('CRC corruption is rejected before an extraction can be published', async () => {
  const bytes = await zip([['Regular.ttf', new Uint8Array([1, 2, 3])]]);
  const header = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
  const offset = 30 + header.getUint16(26, true) + header.getUint16(28, true);
  bytes[offset] ^= 0xff;
  const target = storage(bytes);
  await assert.rejects(extractArchive(source, generation, new AbortController().signal));
  assert.equal(target.open, 0);
});

test('case insensitive duplicate paths are rejected before writing', async () => {
  const target = storage(await zip([
    ['Regular.ttf', new Uint8Array([1])], ['regular.ttf', new Uint8Array([2])],
  ]));
  await assert.rejects(extractArchive(source, generation, new AbortController().signal), /Duplicate archive paths/);
  assert.equal(target.files.size, 1);
});

test('cancellation closes an open OPFS writer before rejecting', async () => {
  const controller = new AbortController();
  const target = storage(await zip([['Regular.ttf', new Uint8Array(128 * 1024)]]), () => controller.abort());
  await assert.rejects(extractArchive(source, generation, controller.signal), {name: 'AbortError'});
  assert.equal(target.open, 0);
  assert.equal(target.files.has(generation + '/Regular.ttf'), false);
});

test('oversized entry metadata is rejected before allocating an output file', async () => {
  const bytes = await zip([['Regular.ttf', new Uint8Array([1])]]);
  const view = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
  for (let offset = 0; offset < bytes.length - 46; offset++) {
    if (view.getUint32(offset, true) === 0x02014b50) view.setUint32(offset + 24, 64 * 1024 * 1024 + 1, true);
  }
  const target = storage(bytes);
  await assert.rejects(extractArchive(source, generation, new AbortController().signal), /decoded size limit/);
  assert.equal(target.files.size, 1);
});
