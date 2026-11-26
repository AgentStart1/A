const messages = {
  en: {
    title: 'Choose fonts',
    choice: 'Choose which fonts to use at startup',
    permission: 'Allow local font access in your browser, then try again.',
    missing: 'Could not load a suitable local font.',
    loading: 'Loading fonts…',
    local: 'Use local fonts',
    retry: 'Try again',
    fallback: 'Use downloaded fonts',
  },
  zh: {
    title: '选择字体',
    choice: '选择启动时使用的字体',
    permission: '本地字体权限尚未就绪，请在浏览器中允许访问后重试。',
    missing: '未能加载合适的本地字体。',
    loading: '正在加载字体…',
    local: '使用本地字体',
    retry: '重试',
    fallback: '使用下载字体',
  },
};

const styles = `
  #font-gate {
    --gate-bg: #f0f3ee;
    --gate-text: #18342c;
    --gate-muted: #4d6b60;
    --gate-button: #fff;
    --gate-primary: #315e4c;
    box-sizing: border-box;
    position: fixed;
    inset: 0;
    width: 100%;
    height: 100%;
    max-width: none;
    max-height: none;
    margin: 0;
    padding: 24px;
    border: 0;
    overflow: auto;
    background: var(--gate-bg);
    color: var(--gate-text);
    font: 1rem/1.6 system-ui, sans-serif;
  }
  #font-gate[open] { display: grid; place-items: center; }
  #font-gate::backdrop { background: var(--gate-bg); }
  #font-gate .font-gate-content { width: min(100%, 520px); text-align: center; }
  #font-gate-title { margin: 0 0 40px; font-size: 1.75rem; font-weight: 500; line-height: 1.35; }
  #font-gate-message { min-height: 48px; display: grid; place-items: center; margin: 0 0 40px; color: var(--gate-muted); }
  #font-gate .font-gate-actions { display: flex; flex-wrap: wrap; justify-content: center; gap: 12px; }
  #font-gate button {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    min-height: 44px;
    padding: 8px 14px;
    border: 0;
    border-radius: 8px;
    background: var(--gate-button);
    color: inherit;
    font: inherit;
    cursor: pointer;
  }
  #font-gate #font-gate-default { background: var(--gate-primary); color: #fff; }
  #font-gate button:disabled { opacity: .55; cursor: default; }
  #font-gate button:focus-visible { outline: 2px solid var(--gate-text); outline-offset: 3px; }
  #font-gate svg { width: 17px; height: 17px; flex: none; }
  @media (max-height: 400px) {
    #font-gate-title, #font-gate-message { margin-bottom: 16px; }
  }
  @media (prefers-color-scheme: dark) {
    #font-gate { --gate-bg: #18241e; --gate-text: #e4eee6; --gate-muted: #b4c7bb; --gate-button: #283b31; --gate-primary: #386d55; }
  }
`;

const iconPaths = {
  type: 'M4 7V4h16v3 M9 20h6 M12 4v16',
  download: 'M12 3v12 m-4-4 4 4 4-4 M4 16v4a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1v-4',
};

const createIcon = (document, name) => {
  const namespace = 'http://www.w3.org/2000/svg';
  const icon = document.createElementNS(namespace, 'svg');
  icon.setAttribute('viewBox', '0 0 24 24');
  icon.setAttribute('fill', 'none');
  icon.setAttribute('stroke', 'currentColor');
  icon.setAttribute('stroke-width', '2');
  icon.setAttribute('stroke-linecap', 'round');
  icon.setAttribute('stroke-linejoin', 'round');
  icon.setAttribute('aria-hidden', 'true');
  const path = document.createElementNS(namespace, 'path');
  path.setAttribute('d', iconPaths[name]);
  icon.append(path);
  return icon;
};

/** Native text keeps onboarding readable before Compose has loaded any fallback fonts. */
export const createFontGateView = (document, language, useLocal, useDefault) => {
  const copy = messages[language.toLowerCase().startsWith('zh') ? 'zh' : 'en'];
  const dialog = document.createElement('dialog');
  const style = document.createElement('style');
  style.textContent = styles;
  dialog.id = 'font-gate';
  dialog.setAttribute('aria-labelledby', 'font-gate-title');
  dialog.setAttribute('aria-describedby', 'font-gate-message');
  const content = document.createElement('section');
  content.className = 'font-gate-content';
  const title = document.createElement('h1');
  title.id = 'font-gate-title';
  title.textContent = copy.title;
  const message = document.createElement('p');
  message.id = 'font-gate-message';
  message.setAttribute('role', 'status');
  const actions = document.createElement('div');
  actions.className = 'font-gate-actions';
  const button = (id, icon, text, action) => {
    const element = document.createElement('button');
    element.id = id;
    element.type = 'button';
    const label = document.createElement('span');
    label.textContent = text;
    element.append(createIcon(document, icon), label);
    element.addEventListener('click', action);
    return { element, label };
  };
  const local = button('font-gate-local', 'type', copy.local, useLocal);
  const fallback = button('font-gate-default', 'download', copy.fallback, useDefault);
  actions.append(local.element, fallback.element);
  content.append(title, message, actions);
  dialog.append(content);
  dialog.addEventListener('cancel', (event) => event.preventDefault());
  document.body.append(style, dialog);
  return {
    render(state) {
      if (state.kind === 'ready') {
        dialog.close();
        return;
      }
      const busy = state.kind === 'loading' || state.kind === 'font';
      message.textContent = busy ? copy.loading : copy[state.kind];
      local.label.textContent = busy || state.kind === 'choice' ? copy.local : copy.retry;
      local.element.disabled = busy;
      // Switching to downloaded fonts remains available even during a slow OS/font read.
      if (!dialog.open) dialog.showModal();
    },
    close() { dialog.remove(); style.remove(); },
  };
};
