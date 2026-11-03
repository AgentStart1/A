import { createFontGateHost, FONT_PREFERENCE_KEY } from './fontGateHost.mjs';
import { createFontGateView } from './fontGateView.mjs';

const cjkFontFamilies = [
  'Microsoft YaHei UI',
  'Microsoft YaHei',
  'DengXian',
  'PingFang SC',
  'Hiragino Sans GB',
  'Noto Sans CJK SC',
  'Noto Sans SC',
  'Source Han Sans SC',
  'Source Han Sans CN',
  'HarmonyOS Sans SC',
  'MiSans',
  'WenQuanYi Micro Hei',
  'Droid Sans Fallback',
  'Arial Unicode MS',
  'SimHei',
  'SimSun',
];

const bytesToBase64 = (bytes) => {
  const chunkSize = 0x8000;
  let binary = '';
  for (let offset = 0; offset < bytes.length; offset += chunkSize) {
    binary += String.fromCharCode(...bytes.subarray(offset, offset + chunkSize));
  }
  return btoa(binary);
};

const isSupported = () =>
  window.isSecureContext && typeof window.queryLocalFonts === 'function';

const stylePriority = (style) => {
  const normalizedStyle = style.toLocaleLowerCase();
  if (normalizedStyle === 'regular') return 0;
  if (normalizedStyle === 'normal') return 1;
  if (normalizedStyle.includes('medium')) return 2;
  return 3;
};

const selectPreferredFont = (fonts) => {
  for (const family of cjkFontFamilies) {
    const normalizedFamily = family.toLocaleLowerCase();
    const candidates = fonts
      .filter(
        (font) =>
          font.family.toLocaleLowerCase() === normalizedFamily ||
          font.fullName.toLocaleLowerCase() === normalizedFamily,
      )
      .sort((left, right) => stylePriority(left.style) - stylePriority(right.style));
    if (candidates.length > 0) return candidates[0];
  }
  return null;
};

const loadPreferredFontJson = async () => {
  const selectedFont = selectPreferredFont(await window.queryLocalFonts());
  if (!selectedFont) return null;
  const blob = await selectedFont.blob();
  const base64 = bytesToBase64(new Uint8Array(await blob.arrayBuffer()));
  return {
    identity: selectedFont.postscriptName,
    base64,
  };
};


let activeGate = null;
let unsavedPreference = null;

const readPreference = () => {
  if (unsavedPreference !== null) return unsavedPreference;
  try {
    const value = window.localStorage.getItem(FONT_PREFERENCE_KEY);
    return value === 'true' ? true : value === 'false' ? false : null;
  } catch {
    return null;
  }
};

const permissionStatus = async () => {
  try {
    return await navigator.permissions.query({ name: 'local-fonts' });
  } catch {
    return null;
  }
};

export const startFontGate = (onSnapshot) => {
  stopFontGate();
  const platform = {
    supported: isSupported,
    readPreference,
    writePreference(value) {
      try {
        window.localStorage.setItem(FONT_PREFERENCE_KEY, String(value));
        unsavedPreference = null;
      } catch {
        // A denied storage write must not trap this visit in the onboarding gate.
        unsavedPreference = value;
      }
    },
    loadFont: loadPreferredFontJson,
    async permission() { return (await permissionStatus())?.state ?? 'unknown'; },
    async watchPermission(onChange) {
      const status = await permissionStatus();
      status?.addEventListener('change', onChange);
      return () => status?.removeEventListener('change', onChange);
    },
  };
  const view = createFontGateView(document, navigator.language, () => host.useLocal(), () => host.useDefault());
  const host = createFontGateHost(platform, (snapshot) => onSnapshot(JSON.stringify(snapshot)), view.render);
  const resume = () => { void host.resume(); };
  const storage = (event) => { if (event.key === FONT_PREFERENCE_KEY || event.key === null) resume(); };
  window.addEventListener('focus', resume);
  window.addEventListener('storage', storage);
  activeGate = { host, close() {
    window.removeEventListener('focus', resume);
    window.removeEventListener('storage', storage);
    host.close();
    view.close();
  } };
  void host.start();
};

export const confirmFont = (requestId) => activeGate?.host.confirmFont(requestId) ?? false;
export const rejectFont = (requestId) => activeGate?.host.rejectFont(requestId);
export const stopFontGate = () => {
  activeGate?.close();
  activeGate = null;
};
