/* This is a private project. All rights reserved. */
import { BlobReader, ZipReader, configure } from '@zip.js/zip.js/lib/zip-core-native.js';
import { archiveName, cacheFile, cacheParts } from './paths.mjs';

configure({useWebWorkers: false, chunkSize: 64 * 1024});
const MAX_SOURCE_BYTES = 64 * 1024 * 1024;
const MAX_EXPANDED_BYTES = 80 * 1024 * 1024;
const MAX_ENTRIES = 2048;

// The caller holds the file lock and saves database completion only after this returns.
export async function extractArchive(path, directory, signal) {
  cacheParts(path);
  cacheParts(directory);
  if (directory !== path + '.extracted') throw new Error('Invalid extraction directory');
  signal.throwIfAborted();
  const source = await (await cacheFile(path)).getFile();
  if (source.size > MAX_SOURCE_BYTES) throw new Error('Archive exceeds the source size limit');
  const reader = new ZipReader(new BlobReader(source), {
    useWebWorkers: false, checkSignature: true, strictness: 'strict',
  });
  try {
    const entries = [];
    const names = new Set();
    let total = 0;
    for await (const entry of reader.getEntriesGenerator()) {
      signal.throwIfAborted();
      if (entry.directory) continue;
      if (entries.length >= MAX_ENTRIES) throw new Error('Too many archive entries');
      const name = archiveName(entry.filename);
      const key = name.toLowerCase();
      if (names.has(key)) throw new Error('Duplicate archive paths');
      names.add(key);
      if (entry.encrypted) throw new Error('Encrypted archives are unsupported');
      if (!Number.isSafeInteger(entry.uncompressedSize) || entry.uncompressedSize < 0) {
        throw new Error('Invalid archive entry size');
      }
      total += entry.uncompressedSize;
      if (total > MAX_EXPANDED_BYTES) throw new Error('Archive exceeds the decoded size limit');
      entries.push({entry, path: directory + '/' + name});
    }
    const output = [];
    let remaining = MAX_EXPANDED_BYTES;
    for (const {entry, path: target} of entries) {
      signal.throwIfAborted();
      const handle = await cacheFile(target, true);
      const stream = await handle.createWritable();
      let size = 0;
      try {
        // Bound actual output as well as metadata before any bytes reach OPFS.
        const sink = new WritableStream({
          async write(bytes) {
            signal.throwIfAborted();
            if (bytes.byteLength > remaining || size + bytes.byteLength > entry.uncompressedSize) {
              throw new Error('Archive exceeds the decoded size limit');
            }
            remaining -= bytes.byteLength;
            size += bytes.byteLength;
            await stream.write(bytes);
          },
        });
        await entry.getData(sink, {signal, checkSignature: true, useWebWorkers: false});
        signal.throwIfAborted();
        if (size !== entry.uncompressedSize) throw new Error('Archive entry size mismatch');
        await stream.close();
      } catch (failure) {
        try { await stream.abort(); } catch (cleanup) {
          throw new AggregateError([failure, cleanup], 'Archive extraction and cleanup failed');
        }
        throw failure;
      }
      output.push({path: target, size});
    }
    signal.throwIfAborted();
    return output;
  } finally {
    await reader.close();
  }
}
