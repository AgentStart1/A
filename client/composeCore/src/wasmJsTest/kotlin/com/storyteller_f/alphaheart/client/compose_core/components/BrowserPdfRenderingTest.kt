/*
 * This is a private project. All rights reserved.
 */

@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.storyteller_f.alphaheart.client.compose_core.components

import com.storyteller_f.alphaheart.client.compose_core.media.createPdfPreview
import kotlinx.coroutines.await
import kotlinx.coroutines.test.runTest
import org.w3c.dom.HTMLElement
import kotlin.js.JsAny
import kotlin.js.Promise
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class BrowserPdfRenderingTest {
    @Test
    fun nativeViewerDisabledStillRendersPdfPixelsSelectableTextAndBothViewportWidths() =
        runTest {
            val environment = installPdfTestEnvironment()
            val container = createPdfTestContainer(390)
            val url = createPdfTestDocument(false)
            val failures = mutableListOf<String>()
            var displayedPage: Pair<Int, Int>? = null
            val renderer =
                createPdfPreview(container, url, { failures.add(it) }) { index, count ->
                    displayedPage = index to count
                }
            try {
                renderer.ready.await()
                awaitPdfPageWidth(container, 1, 1.0).await()
                assertTrue(pdfPageHasBlackInkAndWhitePaper(container, 1))
                assertEquals("First page text", pdfSelectedText(container, 1))
                assertEquals(2, pdfPageCount(container))
                assertEquals(0 to 2, displayedPage)
                assertEquals(1, activePdfWorkers())
                assertTrue(pdfPageMatchesScrollViewport(container, 1))
                val narrowWidth = pdfCanvasWidth(container, 1)
                resizePdfTestContainer(container, 800)
                awaitPdfPageWidth(container, 1, 1.0).await()
                assertTrue(pdfCanvasWidth(container, 1) > narrowWidth)
                assertTrue(pdfPageMatchesScrollViewport(container, 1))
                assertTrue(pdfPageHasBlackInkAndWhitePaper(container, 1))
                renderer.goToPage(1)
                awaitPdfPageWidth(container, 2, 1.0).await()
                assertEquals("Second page text", pdfSelectedText(container, 2))
                assertEquals(1 to 2, displayedPage)
                assertTrue(pdfPageHasBlackInkAndWhitePaper(container, 2))
                renderer.setZoom(1.25)
                awaitPdfPageWidth(container, 2, 1.25).await()
                assertTrue(pdfPageHasBlackInkAndWhitePaper(container, 2))
                assertEquals(emptyList(), failures)
            } finally {
                renderer.close().await()
                assertEquals(0, pdfChildCount(container))
                assertEquals(0, activePdfWorkers())
                removePdfTestEnvironment(environment, container, url)
            }
        }

    @Test
    fun malformedPdfFailsInsteadOfBecomingReadyAndReleasesItsWorker() =
        runTest {
            val environment = installPdfTestEnvironment()
            val container = createPdfTestContainer(390)
            val url = createPdfTestDocument(true)
            val failures = mutableListOf<String>()
            val renderer = createPdfPreview(container, url, { failures.add(it) }, null)
            try {
                val result = runCatching { renderer.ready.await() }
                assertTrue(result.isFailure)
                assertEquals("failed", pdfRenderState(container))
                assertTrue(failures.isNotEmpty())
                assertFalse(pdfHasRenderedPages(container))
            } finally {
                renderer.close().await()
                assertEquals(0, activePdfWorkers())
                removePdfTestEnvironment(environment, container, url)
            }
        }

    @Test
    fun closeDuringLoadingRejectsReadinessAndRemovesDomAndWorker() =
        runTest {
            val environment = installPdfTestEnvironment()
            val container = createPdfTestContainer(390)
            val url = createPdfTestDocument(false)
            val failures = mutableListOf<String>()
            val renderer = createPdfPreview(container, url, { failures.add(it) }, null)
            try {
                renderer.close().await()
                assertTrue(runCatching { renderer.ready.await() }.isFailure)
                assertEquals(0, pdfChildCount(container))
                assertEquals(0, activePdfWorkers())
                assertEquals(emptyList(), failures)
            } finally {
                renderer.close().await()
                removePdfTestEnvironment(environment, container, url)
            }
        }
}

@JsFun(
    """
    () => {
        const native = Object.getOwnPropertyDescriptor(navigator, 'pdfViewerEnabled');
        Object.defineProperty(navigator, 'pdfViewerEnabled', {configurable:true, value:false});
        const OriginalWorker = globalThis.Worker;
        let active = 0;
        globalThis.Worker = class extends OriginalWorker {
            constructor(...args) { super(...args); active++; this.pdfClosed = false; }
            terminate() { if (!this.pdfClosed) { this.pdfClosed = true; active--; } return super.terminate(); }
        };
        globalThis.__activePdfWorkers = () => active;
        return () => {
            globalThis.Worker = OriginalWorker;
            delete globalThis.__activePdfWorkers;
            if (native) Object.defineProperty(navigator,'pdfViewerEnabled',native);
            else delete navigator.pdfViewerEnabled;
        };
    }
    """,
)
private external fun installPdfTestEnvironment(): JsAny

@JsFun(
    """
    (restore, container, url) => {
        container.remove(); URL.revokeObjectURL(url); getSelection().removeAllRanges(); restore();
    }
    """,
)
private external fun removePdfTestEnvironment(restore: JsAny, container: HTMLElement, url: String)

@JsFun(
    """
    (width) => {
        const node = document.createElement('div');
        node.style.cssText = 'position:relative;width:'+width+'px;height:600px';
        document.body.append(node); return node;
    }
    """,
)
private external fun createPdfTestContainer(width: Int): HTMLElement

@JsFun(
    """
    (malformed) => {
        if (malformed) return URL.createObjectURL(new Blob(['%PDF-invalid'],{type:'application/pdf'}));
        const stream = text => 'q 0 0 0 rg 40 40 120 60 re f Q\nBT /F1 16 Tf 40 320 Td ('+text+') Tj ET';
        const objects = ['<< /Type /Catalog /Pages 2 0 R >>','<< /Type /Pages /Kids [3 0 R 5 0 R] /Count 2 >>',
            ...['First page text','Second page text'].flatMap((text,index) => [
                '<< /Type /Page /Parent 2 0 R /MediaBox [0 0 300 400] /Resources << /Font << /F1 7 0 R >> >> /Contents '+(4+index*2)+' 0 R >>',
                '<< /Length '+stream(text).length+' >>\nstream\n'+stream(text)+'\nendstream'
            ]), '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>'];
        let pdf = '%PDF-1.4\n', offsets = [0];
        for(let i=0;i<objects.length;i++){offsets.push(pdf.length);pdf+=(i+1)+' 0 obj\n'+objects[i]+'\nendobj\n';}
        const xref = pdf.length;
        pdf += 'xref\n0 8\n0000000000 65535 f \n';
        for(const offset of offsets.slice(1)) pdf += String(offset).padStart(10,'0')+' 00000 n \n';
        pdf += 'trailer\n<< /Size 8 /Root 1 0 R >>\nstartxref\n'+xref+'\n%%EOF\n';
        return URL.createObjectURL(new Blob([pdf],{type:'application/pdf'}));
    }
    """,
)
private external fun createPdfTestDocument(malformed: Boolean): String

@JsFun("(container, width) => { container.style.width = width+'px'; }")
private external fun resizePdfTestContainer(container: HTMLElement, width: Int)

@JsFun(
    """
    (container, page, zoom) => new Promise(resolve => {
        const check = () => {
            const sheet = container.querySelector('[data-pdf-page="'+page+'"]');
            const canvas = sheet?.querySelector(':scope > canvas');
            const root = container.querySelector('.alpha-pdf-document');
            const target = Math.ceil(Math.floor(root.clientWidth*zoom)*Math.max(1,devicePixelRatio));
            if (sheet?.dataset.pdfRendered==='true' && canvas?.width===target) {
                observer.disconnect(); resolve(null);
            }
        };
        const observer = new MutationObserver(check);
        observer.observe(container,{subtree:true,attributes:true,childList:true}); check();
    })
    """,
)
private external fun awaitPdfPageWidth(container: HTMLElement, page: Int, zoom: Double): Promise<JsAny?>

@JsFun(
    """
    (container, page) => {
        const root = container.querySelector('.alpha-pdf-document');
        const paper = root.querySelector('[data-pdf-page="' + page + '"]').getBoundingClientRect();
        const left = root.getBoundingClientRect().left;
        return Math.abs(paper.left - left) < 1 && Math.abs(paper.right - left - root.clientWidth) < 1;
    }
    """,
)
private external fun pdfPageMatchesScrollViewport(container: HTMLElement, page: Int): Boolean

@JsFun("(container, page) => container.querySelector('[data-pdf-page=\"'+page+'\"] > canvas').width")
private external fun pdfCanvasWidth(container: HTMLElement, page: Int): Int

@JsFun(
    """
    (container, page) => {
        const canvas = container.querySelector('[data-pdf-page="'+page+'"] > canvas');
        const ctx = canvas.getContext('2d');
        const black = ctx.getImageData(Math.floor(canvas.width*.2),Math.floor(canvas.height*.8),1,1).data;
        const white = ctx.getImageData(2,2,1,1).data;
        return black[0]<20 && black[1]<20 && black[2]<20 && white[0]>240 && white[1]>240 && white[2]>240;
    }
    """,
)
private external fun pdfPageHasBlackInkAndWhitePaper(container: HTMLElement, page: Int): Boolean

@JsFun(
    """
    (container, page) => {
        const layer = container.querySelector('[data-pdf-page="'+page+'"] .alpha-pdf-text');
        const range = document.createRange(); range.selectNodeContents(layer);
        const selection = getSelection(); selection.removeAllRanges(); selection.addRange(range);
        return selection.toString();
    }
    """,
)
private external fun pdfSelectedText(container: HTMLElement, page: Int): String

@JsFun("(container) => container.querySelectorAll('.alpha-pdf-page').length")
private external fun pdfPageCount(container: HTMLElement): Int

@JsFun("(container) => container.childElementCount")
private external fun pdfChildCount(container: HTMLElement): Int

@JsFun("() => globalThis.__activePdfWorkers()")
private external fun activePdfWorkers(): Int

@JsFun("(container) => container.querySelector('.alpha-pdf-document').dataset.pdfState")
private external fun pdfRenderState(container: HTMLElement): String

@JsFun("(container) => !!container.querySelector('[data-pdf-rendered=\"true\"]')")
private external fun pdfHasRenderedPages(container: HTMLElement): Boolean
