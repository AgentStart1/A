import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createFontGateHost } from './fontGateHost.mjs';

const fixture = (preference = null, supported = true) => {
  const state = { preference, permission: 'prompt', loads: 0, stopped: 0 };
  const snapshots = [];
  const views = [];
  const font = { identity: 'Synthetic', base64: 'AA==' };
  const platform = {
    supported: () => supported,
    readPreference: () => state.preference,
    writePreference: (value) => { state.preference = value; },
    permission: async () => state.permission,
    watchPermission: async (listener) => { state.changed = listener; return () => { state.stopped += 1; }; },
    loadFont() { state.loads += 1; return state.request ? state.request() : Promise.resolve(font); },
  };
  const host = createFontGateHost(platform, (s) => snapshots.push(s), (s) => views.push(s));
  return { host, state, snapshots, views, latest: () => views.at(-1) };
};

test('missing preference blocks without requesting fonts', async () => {
  const f = fixture(); await f.host.start();
  assert.equal(f.latest().kind, 'choice'); assert.equal(f.state.loads, 0);
  assert.deepEqual(f.snapshots, [{ type: 'blocked' }]);
});
test('explicit opt out is persisted and opens downloaded-font content', async () => {
  const f = fixture(); await f.host.start(); f.host.useDefault();
  assert.equal(f.state.preference, false); assert.equal(f.latest().source, 'default');
  assert.deepEqual(f.snapshots.at(-1), { type: 'default' });
});
test('stored opt out starts without a font query', async () => {
  const f = fixture(false); await f.host.start();
  assert.equal(f.latest().source, 'default'); assert.equal(f.state.loads, 0);
});
test('unsupported browsers open defaults without overwriting stored intent', async () => {
  const f = fixture(true, false); await f.host.start();
  assert.equal(f.latest().source, 'default'); assert.equal(f.state.preference, true);
  assert.equal(f.state.loads, 0);
});
test('stored enabled preference does not bypass pending authorization', async () => {
  const f = fixture(true); await f.host.start();
  assert.equal(f.latest().kind, 'permission'); assert.equal(f.state.loads, 0);
});
test('font access starts synchronously from the explicit action', async () => {
  const f = fixture(); await f.host.start(); f.state.permission = 'granted';
  const request = f.host.useLocal(); assert.equal(f.state.loads, 1);
  await request;
  assert.equal(f.state.preference, true); assert.equal(f.latest().kind, 'font');
  assert.equal(f.host.confirmFont(f.snapshots.at(-1).requestId), true);
  assert.equal(f.latest().source, 'local');
});
test('stored grant still loads and waits for Compose preload acceptance', async () => {
  const f = fixture(true); f.state.permission = 'granted'; await f.host.start();
  assert.equal(f.state.loads, 1); assert.equal(f.latest().kind, 'font');
  f.host.rejectFont(f.snapshots.at(-1).requestId); assert.equal(f.latest().kind, 'missing');
});
test('denied access remains blocked with an explicit opt-out exit', async () => {
  const f = fixture(); await f.host.start(); f.state.permission = 'denied';
  f.state.request = () => Promise.reject(new Error('NotAllowedError'));
  await f.host.useLocal(); assert.equal(f.latest().kind, 'permission');
  assert.equal(f.state.preference, true);
  f.host.useDefault(); assert.equal(f.latest().source, 'default');
});
test('empty font selection remains blocked', async () => {
  const f = fixture(); await f.host.start(); f.state.permission = 'granted';
  f.state.request = async () => null; await f.host.useLocal();
  assert.equal(f.latest().kind, 'missing');
});
test('late font reads cannot replace a newer opt out', async () => {
  const f = fixture(); await f.host.start(); f.state.permission = 'granted';
  let complete; f.state.request = () => new Promise((resolve) => { complete = resolve; });
  const pending = f.host.useLocal(); f.host.useDefault();
  complete({ identity: 'Late', base64: 'AA==' }); await pending;
  assert.equal(f.latest().source, 'default'); assert.equal(f.snapshots.at(-1).type, 'default');
});
test('permission revocation removes ready content', async () => {
  const f = fixture(); await f.host.start(); f.state.permission = 'granted'; await f.host.useLocal();
  f.host.confirmFont(f.snapshots.at(-1).requestId); f.state.permission = 'denied';
  await f.state.changed(); assert.equal(f.latest().kind, 'permission');
  assert.equal(f.snapshots.at(-1).type, 'blocked');
});
test('revocation during a font read invalidates its completion', async () => {
  const f = fixture(); await f.host.start(); f.state.permission = 'granted';
  let complete; f.state.request = () => new Promise((resolve) => { complete = resolve; });
  const pending = f.host.useLocal(); f.state.permission = 'denied'; await f.state.changed();
  complete({ identity: 'Late', base64: 'AA==' }); await pending;
  assert.equal(f.latest().kind, 'permission'); assert.equal(f.snapshots.at(-1).type, 'blocked');
});
test('focus returning from a pending browser prompt does not cancel authorization', async () => {
  const f = fixture(); await f.host.start();
  let complete; f.state.request = () => new Promise((resolve) => { complete = resolve; });
  const pending = f.host.useLocal(); await f.host.resume();
  f.state.permission = 'granted'; complete({ identity: 'Allowed', base64: 'AA==' }); await pending;
  assert.equal(f.latest().kind, 'font');
});
test('clearing preference in another tab returns to the choice gate', async () => {
  const f = fixture(false); await f.host.start(); f.state.preference = null; await f.host.resume();
  assert.equal(f.latest().kind, 'choice');
});
test('closing removes permission observation and ignores late reads', async () => {
  const f = fixture(); await f.host.start(); f.state.permission = 'granted';
  let complete; f.state.request = () => new Promise((resolve) => { complete = resolve; });
  const pending = f.host.useLocal(); const count = f.snapshots.length; f.host.close();
  complete({ identity: 'Late', base64: 'AA==' }); await pending;
  assert.equal(f.snapshots.length, count); assert.equal(f.state.stopped, 1);
});

for (const permission of ['prompt', 'denied']) {
  test(`permission reset to ${permission} invalidates a font awaiting confirmation`, async () => {
    const f = fixture(); await f.host.start(); f.state.permission = 'granted'; await f.host.useLocal();
    const requestId = f.snapshots.at(-1).requestId;
    f.state.permission = permission; await f.state.changed();
    assert.equal(f.latest().kind, 'permission');
    assert.equal(f.snapshots.at(-1).type, 'blocked');
    assert.equal(f.host.confirmFont(requestId), false);
    assert.equal(f.latest().kind, 'permission');
  });
}

test('focus recheck rejects a pending font after permission is reset', async () => {
  const f = fixture(); await f.host.start(); f.state.permission = 'granted'; await f.host.useLocal();
  const requestId = f.snapshots.at(-1).requestId;
  f.state.permission = 'prompt'; await f.host.resume();
  assert.equal(f.snapshots.at(-1).type, 'blocked');
  assert.equal(f.host.confirmFont(requestId), false);
  assert.equal(f.latest().kind, 'permission');
});
