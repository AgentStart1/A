/*
 * This is a private project. All rights reserved.
 */

@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.runtime.SideEffect
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.compose.LocalPlatformContext
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.size.Size
import com.storyteller_f.alphaheart.client.compose_core.utils.addPlatformImageDecoders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.js.JsAny
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
internal class SvgDecoderLifecycleTest {
    @Test
    fun malformedSvgReturnsTheBrowserDecoderError() =
        runComposeUiTest {
            val context = capturePlatformContext()
            val imageLoader = ImageLoader.Builder(context).addPlatformImageDecoders().build()
            try {
                val result =
                    imageLoader.execute(
                        ImageRequest.Builder(context)
                            .data(
                                (
                                    "<svg xmlns='http://www.w3.org/2000/svg' width='200' height='60'>" +
                                        "<text x='10' y='40'>Malformed"
                                    ).encodeToByteArray(),
                            )
                            .size(Size.ORIGINAL)
                            .build(),
                    )

                val error = assertIs<ErrorResult>(result)
                assertTrue(
                    error.throwable.message.orEmpty().contains("Invalid SVG image"),
                    "Expected invalid-SVG decode error, got ${error.throwable}",
                )
            } finally {
                imageLoader.shutdown()
            }
        }

    @Test
    fun cancellingSvgRequestClearsImageHandlersAndRevokesOwnedBlobUrl() =
        runComposeUiTest {
            val context = capturePlatformContext()
            val imageLoader = ImageLoader.Builder(context).addPlatformImageDecoders().build()
            val probe = installSvgLifecycleProbe()
            try {
                withContext(Dispatchers.Default) {
                    coroutineScope {
                        val request =
                            async {
                                imageLoader.execute(
                                    ImageRequest.Builder(context)
                                        .data(
                                            """
                                            <svg xmlns="http://www.w3.org/2000/svg" width="200" height="60">
                                              <text x="10" y="40" font-family="Roboto" font-size="24">Alpha</text>
                                            </svg>
                                            """.trimIndent().encodeToByteArray(),
                                        )
                                        .size(Size.ORIGINAL)
                                        .build(),
                                )
                            }
                        withTimeout(5_000) {
                            while (probe.startedImageCount == 0 || probe.createdBlobCount == 0) delay(1)
                        }
                        request.cancelAndJoin()
                    }
                }

                assertTrue(probe.cleaned, "SVG cancellation did not release its browser resources: ${probe.details}")
            } finally {
                probe.restore()
                imageLoader.shutdown()
            }
        }
}

@OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
private suspend fun androidx.compose.ui.test.ComposeUiTest.capturePlatformContext(): PlatformContext {
    var platformContext: PlatformContext? = null
    setContent {
        val context = LocalPlatformContext.current
        SideEffect { platformContext = context }
    }
    awaitIdle()
    return requireNotNull(platformContext)
}

private external interface SvgLifecycleProbe : JsAny {
    val startedImageCount: Int
    val createdBlobCount: Int
    val cleaned: Boolean
    val details: String
    fun restore()
}

@JsFun(
    """() => {
        const originalCreate = URL.createObjectURL;
        const originalRevoke = URL.revokeObjectURL;
        const imagePrototype = HTMLImageElement.prototype;
        const originalSrc = Object.getOwnPropertyDescriptor(imagePrototype, 'src');
        const createdUrls = [];
        const revokedUrls = [];
        const startedImages = [];
        URL.createObjectURL = function(blob) {
            const url = originalCreate.call(URL, blob);
            if (blob.type === 'image/svg+xml') createdUrls.push(url);
            return url;
        };
        URL.revokeObjectURL = function(url) {
            revokedUrls.push(url);
            return originalRevoke.call(URL, url);
        };
        Object.defineProperty(imagePrototype, 'src', {
            configurable: true,
            get() { return originalSrc.get.call(this); },
            set(value) {
                const source = String(value);
                if (createdUrls.includes(source)) {
                    startedImages.push({image: this, url: source, cleared: false});
                    return;
                }
                if (source === '') {
                    const entry = [...startedImages].reverse().find(item => item.image === this && !item.cleared);
                    if (entry) entry.cleared = true;
                }
                originalSrc.set.call(this, value);
            },
        });
        return {
            get startedImageCount() { return startedImages.length; },
            get createdBlobCount() { return createdUrls.length; },
            get cleaned() {
                return startedImages.length > 0 && createdUrls.length > 0 &&
                    startedImages.every(item => item.cleared && item.image.onload === null && item.image.onerror === null) &&
                    createdUrls.every(url => revokedUrls.includes(url));
            },
            get details() {
                return JSON.stringify({
                    startedImages: startedImages.map(item => ({url: item.url, cleared: item.cleared,
                        onload: item.image.onload !== null, onerror: item.image.onerror !== null})),
                    createdUrls, revokedUrls,
                });
            },
            restore() {
                URL.createObjectURL = originalCreate;
                URL.revokeObjectURL = originalRevoke;
                Object.defineProperty(imagePrototype, 'src', originalSrc);
            },
        };
    }""",
)
private external fun installSvgLifecycleProbe(): SvgLifecycleProbe
