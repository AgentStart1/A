/* This is a private project. All rights reserved. */
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { archiveName, cacheParts } from './paths.mjs';

test('archive names reject traversal and absolute paths', () => {
  for (const name of ['../font.ttf', '/font.ttf', './font.ttf', 'a//b', 'C:\\font.ttf', 'a\0b']) {
    assert.throws(() => archiveName(name), /Unsafe archive path/);
  }
  assert.equal(archiveName('family\\Bold.ttf'), 'family/Bold.ttf');
});

test('OPFS paths remain inside the download cache', () => {
  for (const path of ['elsewhere/file', 'downloads/../file', 'downloads//file', 'downloads/a:b']) {
    assert.throws(() => cacheParts(path), /Invalid download-cache location/);
  }
  assert.deepEqual(cacheParts('downloads/account/source'), ['downloads', 'account', 'source']);
});
