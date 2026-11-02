export const FONT_PREFERENCE_KEY = 'a.preferSystemFonts';

/** Owns the gate; stored intent never substitutes for permission or usable font bytes. */
export const createFontGateHost = (platform, publish, render) => {
  let state = { kind: 'choice' };
  let generation = 0;
  let closed = false;
  let stopPermission = () => {};
  const show = (next) => {
    state = next;
    render(next);
  };
  const block = (kind) => {
    generation += 1;
    show({ kind });
    publish({ type: 'blocked' });
  };
  const useDefault = (persist = true) => {
    generation += 1;
    if (persist) platform.writePreference(false);
    show({ kind: 'ready', source: 'default' });
    publish({ type: 'default' });
  };
  const load = () => {
    const requestId = ++generation;
    show({ kind: 'loading' });
    publish({ type: 'blocked' });
    // Start inside the actual button event, before any asynchronous work loses user activation.
    let request;
    try {
      request = platform.loadFont();
    } catch (error) {
      request = Promise.reject(error);
    }
    return Promise.resolve(request).then(async (font) => {
      const permission = await platform.permission();
      if (closed || requestId !== generation) return;
      if (permission === 'denied' || permission === 'prompt') {
        block('permission');
        return;
      }
      if (!font) {
        block('missing');
        return;
      }
      show({ kind: 'font', requestId });
      publish({ type: 'font', requestId, ...font });
    }).catch(() => {
      if (!closed && requestId === generation) block('permission');
    });
  };
  const recheckPermission = async () => {
    const current = generation;
    const permission = await platform.permission();
    if (closed || current !== generation || platform.readPreference() !== true) return;
    const authorized = (state.kind === 'ready' && state.source === 'local') || state.kind === 'font';
    if ((authorized && permission !== 'granted' && permission !== 'unknown') ||
        (state.kind === 'loading' && permission === 'denied')) {
      block('permission');
    }
  };
  return {
    async start() {
      if (!platform.supported()) {
        useDefault(false);
        return;
      }
      const preference = platform.readPreference();
      if (preference === false) useDefault(false);
      else block(preference === true ? 'permission' : 'choice');
      const current = generation;
      stopPermission = await platform.watchPermission(recheckPermission);
      if (closed) {
        stopPermission();
        return;
      }
      if (current === generation && preference === true && await platform.permission() === 'granted') {
        if (!closed && current === generation) await load();
      }
    },
    useDefault,
    useLocal() {
      platform.writePreference(true);
      return load();
    },
    confirmFont(requestId) {
      if (closed || state.kind !== 'font' || generation !== requestId) return false;
      show({ kind: 'ready', source: 'local' });
      return true;
    },
    rejectFont(requestId) {
      if (!closed && state.kind === 'font' && generation === requestId) block('missing');
    },
    async resume() {
      const preference = platform.readPreference();
      if (!platform.supported()) return;
      if (preference === null) block('choice');
      else if (preference === false && !(state.kind === 'ready' && state.source === 'default')) useDefault(false);
      else if (preference !== false && state.kind === 'ready' && state.source === 'default') block(preference === true ? 'permission' : 'choice');
      else await recheckPermission();
    },
    close() {
      closed = true;
      generation += 1;
      stopPermission();
    },
  };
};
