/* This is a private project. All rights reserved. */
import { getDocument, PDFWorker, TextLayer } from "pdfjs-dist/build/pdf.mjs";

// Webpack resolves every font/CMap/decoder asset from the same exact npm distribution as the worker.
class BundledPdfAssets {
  async fetch({ kind, filename }) {
    let url;
    switch (kind) {
      case "cMapUrl": url = new URL(`pdfjs-dist/cmaps/${filename}`, import.meta.url); break;
      case "standardFontDataUrl": url = new URL(`pdfjs-dist/standard_fonts/${filename}`, import.meta.url); break;
      case "iccUrl": url = new URL(`pdfjs-dist/iccs/${filename}`, import.meta.url); break;
      case "wasmUrl": url = new URL(`pdfjs-dist/wasm/${filename}`, import.meta.url); break;
      default: throw new Error("Unsupported PDF asset kind");
    }
    const response = await fetch(url);
    if (!response.ok) throw new Error("Unable to load bundled PDF asset");
    return new Uint8Array(await response.arrayBuffer());
  }
}

export function createPdfPreview(container, url, onFailure) {
  const controller = new AbortController();
  const worker = new Worker(new URL("./pdf-worker.mjs", import.meta.url), { type: "module" });
  const pdfWorker = new PDFWorker({ port: worker });
  const root = document.createElement("div");
  root.className = "alpha-pdf-document";
  root.setAttribute("role", "document");
  root.setAttribute("aria-label", "PDF");
  root.tabIndex = 0;
  root.style.cssText = "position:absolute;inset:0;overflow:auto;overflow-anchor:none;overscroll-behavior:contain;padding:16px;box-sizing:border-box;";
  const style = document.createElement("style");
  style.textContent = `
.alpha-pdf-page{position:relative;margin:0 auto 16px;background:white;overflow:hidden;flex:none}
.alpha-pdf-page:last-child{margin-bottom:0}
.alpha-pdf-text[data-main-rotation="90"]{transform:rotate(90deg) translateY(-100%)}
.alpha-pdf-text[data-main-rotation="180"]{transform:rotate(180deg) translate(-100%,-100%)}
.alpha-pdf-text[data-main-rotation="270"]{transform:rotate(270deg) translateX(-100%)}
.alpha-pdf-page canvas{display:block;width:100%;height:100%}
.alpha-pdf-text{position:absolute;text-align:initial;inset:0;overflow:clip;line-height:1;letter-spacing:normal;word-spacing:normal;transform-origin:0 0;forced-color-adjust:none;--min-font-size:1;--text-scale-factor:calc(var(--total-scale-factor)*var(--min-font-size));--min-font-size-inv:calc(1/var(--min-font-size))}
.alpha-pdf-text :is(span,br){color:transparent;position:absolute;white-space:pre;cursor:text;transform-origin:0 0;user-select:text}
.alpha-pdf-text>:not(.markedContent),.alpha-pdf-text .markedContent span:not(.markedContent){z-index:1;--font-height:0;font-size:calc(var(--text-scale-factor)*var(--font-height));--scale-x:1;--rotate:0deg;transform:rotate(var(--rotate)) scaleX(var(--scale-x)) scale(var(--min-font-size-inv))}
.alpha-pdf-text .markedContent{display:contents}
.alpha-pdf-text ::selection{background:rgba(0,100,200,.3);color:transparent}
`;
  root.append(style);
  container.append(root);
  let closed = false, closePromise = null, loading = null, pdf = null, generation = 0, active = null;
  let width = 0, pendingSize = null, draining = false;
  const pages = [], queued = new Set(), visible = new Set();
  const first = Promise.withResolvers();
  first.promise.catch(() => {});
  const abortError = () => new DOMException("PDF preview closed", "AbortError");
  const report = error => {
    if (closed || error?.name === "RenderingCancelledException" || error?.name === "AbortError") return;
    root.dataset.pdfState = "failed";
    first.reject(error);
    onFailure("Unable to render PDF (" + (error?.name || "Error") + ")");
  };
  const cancelActive = () => {
    active?.render?.cancel();
    active?.text?.cancel();
  };
  const setPageSize = page => {
    const scale = width / page.base.width;
    page.viewport = page.source.getViewport({ scale });
    page.element.style.width = `${page.viewport.width}px`;
    page.element.style.height = `${page.viewport.height}px`;
    page.element.style.setProperty("--scale-factor", scale);
    page.element.style.setProperty("--total-scale-factor", scale * page.viewport.userUnit);
    page.element.style.setProperty("--scale-round-x", "1px");
    page.element.style.setProperty("--scale-round-y", "1px");
  };
  const queue = page => {
    if (!closed && !page.rendered && !queued.has(page)) { queued.add(page); void drain(); }
  };
  async function draw(page, revision) {
    const canvas = document.createElement("canvas");
    canvas.setAttribute("aria-hidden", "true");
    const ratio = Math.max(1, window.devicePixelRatio || 1);
    canvas.width = Math.ceil(page.viewport.width * ratio);
    canvas.height = Math.ceil(page.viewport.height * ratio);
    const text = document.createElement("div");
    text.className = "alpha-pdf-text";
    const staging = document.createElement("div");
    staging.style.cssText = "position:absolute;inset:0;visibility:hidden";
    staging.setAttribute("aria-hidden", "true");
    staging.append(canvas, text);
    page.element.append(staging);
    const context = canvas.getContext("2d", { alpha: false });
    const render = page.source.render({ canvasContext: context, viewport: page.viewport, transform: [ratio, 0, 0, ratio, 0, 0] });
    active = { render, text: null };
    try {
      await render.promise;
      if (closed || revision !== generation) throw abortError();
      const content = await page.source.getTextContent();
      if (closed || revision !== generation) throw abortError();
      const layer = new TextLayer({ textContentSource: content, container: text, viewport: page.viewport });
      active.text = layer;
      await layer.render();
      if (closed || revision !== generation || !visible.has(page)) throw abortError();
      for (const previous of page.element.querySelectorAll("canvas")) {
        if (previous !== canvas) previous.width = previous.height = 0;
      }
      page.element.replaceChildren(canvas, text);
      page.rendered = true;
      page.element.dataset.pdfRendered = "true";
      root.dataset.pdfState = "ready";
      first.resolve(url);
    } finally {
      active = null;
      staging.remove();
      if (!page.rendered) canvas.width = canvas.height = 0;
    }
  }
  async function drain() {
    if (draining || !pdf || !width) return;
    draining = true;
    try {
      while (queued.size && !closed) {
        const page = queued.values().next().value;
        queued.delete(page);
        if (page.rendered || !visible.has(page)) continue;
        try { await draw(page, generation); } catch (error) { report(error); }
      }
    } finally { draining = false; }
  }
  const observer = new IntersectionObserver(entries => {
    for (const entry of entries) {
      const page = pages[Number(entry.target.dataset.pdfPage) - 1];
      if (entry.isIntersecting) { visible.add(page); queue(page); }
      else {
        visible.delete(page); queued.delete(page);
        if (page.rendered) {
          page.element.querySelector("canvas")?.remove();
          page.rendered = false;
          delete page.element.dataset.pdfRendered;
        }
      }
    }
  }, { root, rootMargin: "100% 0px" });
  let resizeFrame = null;
  const applyResize = () => {
    resizeFrame = null;
    const next = Math.max(0, root.clientWidth - 32);
    if (!next || next === width || closed) return;
    const anchor = pages.find(page => page.element.offsetTop + page.element.offsetHeight > root.scrollTop);
    const fraction = anchor ? (root.scrollTop - anchor.element.offsetTop) / anchor.element.offsetHeight : 0;
    width = next; generation++;
    cancelActive();
    for (const page of pages) {
      page.rendered = false;
      delete page.element.dataset.pdfRendered;
      setPageSize(page);
    }
    if (anchor) root.scrollTop = anchor.element.offsetTop + fraction * anchor.element.offsetHeight;
    pendingSize?.resolve(); pendingSize = null;
    for (const page of visible) queue(page);
  };
  // A resize notification must not synchronously mutate the geometry it is observing.
  const resize = new ResizeObserver(() => {
    if (!closed && resizeFrame === null) resizeFrame = requestAnimationFrame(applyResize);
  });
  resize.observe(root);
  void (async () => {
    root.dataset.pdfState = "loading";
    const response = await fetch(url, { credentials: "omit", signal: controller.signal });
    if (!response.ok) throw new Error("PDF request failed: " + response.status);
    const data = new Uint8Array(await response.arrayBuffer());
    if (closed) throw abortError();
    const digest = new Uint8Array(await crypto.subtle.digest("SHA-256", data));
    if (closed) throw abortError();
    root.dataset.pdfSha256 = Array.from(digest, value => value.toString(16).padStart(2, "0")).join("");
    root.dataset.pdfByteLength = String(data.byteLength);
    loading = getDocument({ data, worker: pdfWorker, BinaryDataFactory: BundledPdfAssets, useWorkerFetch: false, disableWorkerRendering: true });
    pdf = await loading.promise;
    if (closed) throw abortError();
    if (!width) { pendingSize = Promise.withResolvers(); await pendingSize.promise; }
    for (let number = 1; number <= pdf.numPages; number++) {
      const source = await pdf.getPage(number);
      if (closed) throw abortError();
      const element = document.createElement("div");
      element.className = "alpha-pdf-page";
      element.dataset.pdfPage = String(number);
      const page = { source, base: source.getViewport({ scale: 1 }), element, rendered: false, viewport: null };
      pages.push(page); setPageSize(page); root.append(element);
    }
    root.dataset.pdfPages = String(pages.length);
    if (!pages.length) throw new Error("PDF has no pages");
    visible.add(pages[0]); queue(pages[0]);
    for (const page of pages) observer.observe(page.element);
  })().catch(report);
  return {
    ready: first.promise,
    license: new URL("./LICENSE-pdfjs", import.meta.url).href,
    notice: new URL("./NOTICE", import.meta.url).href,
    close() {
      if (closed) return closePromise;
      closed = true; generation++;
      if (resizeFrame !== null) cancelAnimationFrame(resizeFrame);
      resizeFrame = null;
      controller.abort(); cancelActive(); observer.disconnect(); resize.disconnect();
      pendingSize?.reject(abortError()); pendingSize = null;
      first.reject(abortError()); queued.clear(); visible.clear();
      for (const page of pages) for (const canvas of page.element.querySelectorAll("canvas")) canvas.width = canvas.height = 0;
      root.remove();
      const cleanup = loading ? loading.destroy() : Promise.resolve();
      closePromise = cleanup.finally(() => { pdfWorker.destroy(); worker.terminate(); });
      return closePromise;
    }
  };
}

// Top-level Kotlin/Wasm @JsModule functions bind to the module's default export.
export default createPdfPreview;
