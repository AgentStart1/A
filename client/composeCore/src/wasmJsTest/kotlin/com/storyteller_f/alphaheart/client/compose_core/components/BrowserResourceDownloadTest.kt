/*
 * This is a private project. All rights reserved.
 */

@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.storyteller_f.alphaheart.client.compose_core.components

import com.storyteller_f.alphaheart.client.compose_core.media.ResourceDownloadRequest
import com.storyteller_f.alphaheart.client.compose_core.media.ResourceSaveOutcome
import com.storyteller_f.alphaheart.client.compose_core.media.downloadBrowserResource
import com.storyteller_f.alphaheart.client.compose_core.tasks.CustomGlobalDialogController
import com.storyteller_f.alphaheart.client.compose_core.tasks.GlobalDialogState
import com.storyteller_f.alphaheart.client.compose_core.tasks.request
import com.storyteller_f.result.CustomResult
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.await
import kotlinx.coroutines.test.runTest
import kotlin.js.JsString
import kotlin.js.Promise
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

internal class BrowserResourceDownloadTest {
    @Test
    fun playableMimeRequestsANamedBlobDownloadWithExactBytesAndReleasesItsUrl() =
        runTest {
            installDownloadProbe("success")
            try {
                val request = ResourceDownloadRequest("https://fixture.invalid/video?signature=private", "片段 原始.mp4", 6)
                assertEquals(ResourceSaveOutcome.HANDED_OFF, downloadBrowserResource(request))
                assertEquals("0,1,127,128,254,255", downloadBytes().await().toString())
                assertEquals("片段 原始.mp4", downloadProbeName())
                assertTrue(downloadProbeUsesBlob())
                assertEquals(1, downloadProbeRequests())
                assertEquals(1, downloadProbeCreated())
                assertEquals(1, downloadProbeRevoked())
                assertEquals(0, downloadProbeRemainingAnchors())
            } finally {
                restoreDownloadProbe()
            }
        }

    @Test
    fun failureUsesTheExistingOperationDialogWithoutOpeningOrRetryingADownload() =
        runTest {
            installDownloadProbe("http")
            try {
                val controller = CustomGlobalDialogController(backgroundScope, Unit)
                val result =
                    controller.useResult {
                        CustomResult.success(
                            downloadBrowserResource(
                                ResourceDownloadRequest("https://fixture.invalid/private", "video.mp4"),
                            ),
                        )
                    }
                assertIs<CustomResult.Failure<*>>(result)
                val state = assertIs<GlobalDialogState.Error>(controller.state.value.single())
                assertEquals("Resource download HTTP 503", state.throwable.message)
                assertFalse(state.throwable.toString().contains("fixture.invalid"))
                assertEquals(1, downloadProbeRequests())
                assertEquals(0, downloadProbeCreated())
                assertEquals(0, downloadProbeRemainingAnchors())
            } finally {
                restoreDownloadProbe()
            }
        }

    @Test
    fun cancellationAbortsTheSingleFetchAndDoesNotBecomeAnOperationError() =
        runTest {
            installDownloadProbe("pending")
            try {
                val controller = CustomGlobalDialogController(backgroundScope, Unit)
                val operation =
                    async(start = CoroutineStart.UNDISPATCHED) {
                        controller.useResult {
                            CustomResult.success(
                                downloadBrowserResource(
                                    ResourceDownloadRequest("https://fixture.invalid/private", "video.mp4"),
                                ),
                            )
                        }
                    }
                assertIs<GlobalDialogState.Loading>(controller.state.value.single())
                operation.cancel()
                operation.join()
                assertTrue(operation.isCancelled)
                assertTrue(controller.state.value.isEmpty())
                assertTrue(downloadProbeAborted())
                assertEquals(1, downloadProbeRequests())
                assertEquals(0, downloadProbeCreated())
                assertEquals(0, downloadProbeRemainingAnchors())
            } finally {
                restoreDownloadProbe()
            }
        }

    @Test
    fun wrongSizeNetworkOrBrowserHandoffFailureReleasesResourcesWithoutExposingTheUrl() =
        runTest {
            for (mode in listOf("success", "network", "click")) {
                installDownloadProbe(mode)
                try {
                    val controller = CustomGlobalDialogController(backgroundScope, Unit)
                    controller.useResult {
                        CustomResult.success(
                            downloadBrowserResource(
                                ResourceDownloadRequest(
                                    "https://fixture.invalid/video?signature=private",
                                    "video.mp4",
                                    if (mode == "success") 999 else 6,
                                ),
                            ),
                        )
                    }
                    val state = assertIs<GlobalDialogState.Error>(controller.state.value.single())
                    assertFalse(state.throwable.toString().contains("signature"))
                    assertFalse(state.throwable.toString().contains("fixture.invalid"))
                    assertEquals(1, downloadProbeRequests())
                    assertEquals(if (mode == "click") 1 else 0, downloadProbeCreated())
                    assertEquals(if (mode == "click") 1 else 0, downloadProbeRevoked())
                    assertEquals(0, downloadProbeRemainingAnchors())
                } finally {
                    restoreDownloadProbe()
                }
            }
        }
}

@JsFun(
    """(mode) => {
        const originalFetch = globalThis.fetch;
        const create = URL.createObjectURL;
        const revoke = URL.revokeObjectURL;
        const originalClick = HTMLAnchorElement.prototype.click;
        if (mode === 'click') HTMLAnchorElement.prototype.click = () => { throw new Error('Download blocked'); };
        const probe = {requests:0, created:0, revoked:0, aborted:false, name:'', href:'', bytes:Promise.resolve('')};
        probe.listener = event => {
            const anchor = event.target;
            if (!(anchor instanceof HTMLAnchorElement) || !anchor.download) return;
            event.preventDefault();
            probe.name = anchor.download;
            probe.href = anchor.href;
            probe.bytes = originalFetch(anchor.href).then(r => r.arrayBuffer())
                .then(b => Array.from(new Uint8Array(b)).join(','));
        };
        document.addEventListener('click', probe.listener, true);
        globalThis.fetch = (url, options) => {
            const target = typeof url === 'string' ? url : url.url;
            if (!target.startsWith('https://fixture.invalid/')) return originalFetch(url, options);
            probe.requests++;
            if (mode === 'network') return Promise.reject(new TypeError('Private URL ' + url));
            if (mode === 'http') return Promise.resolve(new Response('failure',{status:503}));
            if (mode === 'pending') return new Promise((resolve,reject) => {
                options.signal.addEventListener('abort', () => {
                    probe.aborted = true;
                    reject(new DOMException('Cancelled','AbortError'));
                },{once:true});
            });
            return Promise.resolve(new Response(new Uint8Array([0,1,127,128,254,255]),{
                status:200, headers:{'Content-Type':'video/mp4'}
            }));
        };
        URL.createObjectURL = blob => { probe.created++; return create.call(URL,blob); };
        URL.revokeObjectURL = url => { probe.revoked++; revoke.call(URL,url); };
        probe.restore = () => {
            globalThis.fetch = originalFetch;
            URL.createObjectURL = create;
            URL.revokeObjectURL = revoke;
            HTMLAnchorElement.prototype.click = originalClick;
            document.removeEventListener('click',probe.listener,true);
        };
        globalThis.resourceDownloadProbe = probe;
    }""",
)
private external fun installDownloadProbe(mode: String)

@JsFun("() => { globalThis.resourceDownloadProbe.restore(); delete globalThis.resourceDownloadProbe; }")
private external fun restoreDownloadProbe()

@JsFun("() => globalThis.resourceDownloadProbe.bytes")
private external fun downloadBytes(): Promise<JsString>

@JsFun("() => globalThis.resourceDownloadProbe.name")
private external fun downloadProbeName(): String

@JsFun("() => globalThis.resourceDownloadProbe.href.startsWith('blob:')")
private external fun downloadProbeUsesBlob(): Boolean

@JsFun("() => globalThis.resourceDownloadProbe.requests")
private external fun downloadProbeRequests(): Int

@JsFun("() => globalThis.resourceDownloadProbe.created")
private external fun downloadProbeCreated(): Int

@JsFun("() => globalThis.resourceDownloadProbe.revoked")
private external fun downloadProbeRevoked(): Int

@JsFun("() => globalThis.resourceDownloadProbe.aborted")
private external fun downloadProbeAborted(): Boolean

@JsFun("() => document.querySelectorAll('a[download][href^=\"blob:\"]').length")
private external fun downloadProbeRemainingAnchors(): Int
