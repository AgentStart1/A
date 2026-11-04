const messages = {
  en: {
    title: 'Choose your fonts',
    choice: 'Use fonts installed on this device? Access requires browser permission. Otherwise, missing characters use fonts downloaded on demand.',
    permission: 'Local fonts are not ready. Allow font access in your browser, then try again, or continue without system fonts.',
    missing: 'A suitable local font could not be loaded. Try again or continue without system fonts.',
    loading: 'Loading system fonts…',
    local: 'Use system fonts',
    retry: 'Load system fonts and continue',
    fallback: 'Continue without system fonts',
  },
  zh: {
    title: '选择字体方式',
    choice: '是否优先使用本机字体？这需要浏览器授权。不使用时，缺少的字符将通过按需下载的字体显示。',
    permission: '本机字体尚未就绪。请允许浏览器访问字体后重试，也可以选择不使用系统字体并继续。若已拒绝，请先在浏览器的网站设置中修改权限。',
    missing: '未能加载合适的本机字体。可以重试，或选择不使用系统字体并继续。',
    loading: '正在加载系统字体…',
    local: '使用系统字体',
    retry: '加载系统字体并继续',
    fallback: '不使用系统字体并继续',
  },
};

/** Native text keeps onboarding readable before Compose has loaded any fallback fonts. */
export const createFontGateView = (document, language, useLocal, useDefault) => {
  const copy = messages[language.toLowerCase().startsWith('zh') ? 'zh' : 'en'];
  const dialog = document.createElement('dialog');
  const style = document.createElement('style');
  style.textContent = '#font-gate::backdrop { background: #f3f5ec; }';
  dialog.id = 'font-gate';
  dialog.setAttribute('aria-labelledby', 'font-gate-title');
  dialog.setAttribute('aria-describedby', 'font-gate-message');
  dialog.style.cssText = 'box-sizing:border-box;max-width:32rem;width:calc(100% - 2rem);padding:2rem;border:1px solid #bcc5b0;border-radius:1rem;background:#fafbf3;color:#20251d;font:1rem/1.6 system-ui,sans-serif;';
  const title = document.createElement('h1');
  title.id = 'font-gate-title';
  title.textContent = copy.title;
  title.style.cssText = 'font-size:1.5rem;margin:0 0 1rem;';
  const message = document.createElement('p');
  message.id = 'font-gate-message';
  message.setAttribute('role', 'status');
  const button = (id, text, action) => {
    const element = document.createElement('button');
    element.id = id;
    element.type = 'button';
    element.textContent = text;
    element.style.cssText = 'display:block;width:100%;padding:.75rem;margin-top:.75rem;border:1px solid #6a765f;border-radius:.5rem;background:#e2eacb;color:inherit;font:inherit;cursor:pointer;';
    element.addEventListener('click', action);
    return element;
  };
  const local = button('font-gate-local', copy.local, useLocal);
  const fallback = button('font-gate-default', copy.fallback, useDefault);
  dialog.append(title, message, local, fallback);
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
      local.textContent = state.kind === 'choice' ? copy.local : copy.retry;
      local.disabled = busy;
      // Switching to downloaded fonts remains available even during a slow OS/font read.
      if (!dialog.open) dialog.showModal();
    },
    close() { dialog.remove(); style.remove(); },
  };
};
