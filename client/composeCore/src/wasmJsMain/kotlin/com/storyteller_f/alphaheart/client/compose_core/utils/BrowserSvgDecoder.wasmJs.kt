/*
 * This is a private project. All rights reserved.
 */

@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.storyteller_f.alphaheart.client.compose_core.utils

import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DecodeResult
import coil3.decode.DecodeUtils
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.request.maxBitmapSize
import coil3.size.Dimension
import coil3.size.Precision
import coil3.size.Scale
import coil3.svg.isSvg
import kotlinx.coroutines.await
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import okio.use
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.toInt8Array
import kotlin.js.JsAny
import kotlin.js.JsException
import kotlin.js.Promise
import kotlin.js.js

// SVGDOM(Data) omits a font manager: https://youtrack.jetbrains.com/issue/CMP-8578
// Decode with the browser, then retain the existing Compose bitmap rendering path.
internal class BrowserSvgDecoder(private val source: ImageSource, private val options: Options) : Decoder {
    override suspend fun decode(): DecodeResult {
        val bytes = source.source().use { it.readByteArray() }
        val task = startSvgDecode(bytes.toInt8Array().buffer, createSvgDecodeOptions(options))
        val response =
            try {
                task.result.await()
            } catch (failure: JsException) {
                currentCoroutineContext().ensureActive()
                // DOM errors may lack a stack; do not retain an unsafe JavaScript cause.
                throw BrowserSvgFailure(failure)
            } finally {
                task.cancel()
            }
        return DecodeResult(
            image = installBrowserImagePixels(response.buffer, response.width, response.height).asImage(),
            isSampled = response.width < response.sourceWidth || response.height < response.sourceHeight,
        )
    }

    class Factory : Decoder.Factory {
        override fun create(result: SourceFetchResult, options: Options, imageLoader: ImageLoader): Decoder? =
            if (result.mimeType?.substringBefore(';') == "image/svg+xml" || DecodeUtils.isSvg(result.source.source())) {
                BrowserSvgDecoder(result.source, options)
            } else {
                null
            }
    }
}

private class BrowserSvgFailure(failure: JsException) :
    IllegalArgumentException("Browser SVG decoding failed: ${failure.message}")

private fun createSvgDecodeOptions(options: Options): SvgDecodeOptions {
    val request = newSvgDecodeOptions()
    request.targetWidth = (options.size.width as? Dimension.Pixels)?.px ?: 0
    request.targetHeight = (options.size.height as? Dimension.Pixels)?.px ?: 0
    request.fill = options.scale == Scale.FILL
    request.allowUpscale = options.precision == Precision.EXACT
    request.maxWidth = (options.maxBitmapSize.width as? Dimension.Pixels)?.px ?: Int.MAX_VALUE
    request.maxHeight = (options.maxBitmapSize.height as? Dimension.Pixels)?.px ?: Int.MAX_VALUE
    return request
}

private fun newSvgDecodeOptions(): SvgDecodeOptions = js("({})")

private external interface SvgDecodeOptions : JsAny {
    var targetWidth: Int
    var targetHeight: Int
    var fill: Boolean
    var allowUpscale: Boolean
    var maxWidth: Int
    var maxHeight: Int
}

private external interface SvgDecodeTask : JsAny {
    val result: Promise<SvgDecodedPixels>
    fun cancel()
}

private external interface SvgDecodedPixels : JsAny {
    val buffer: ArrayBuffer
    val width: Int
    val height: Int
    val sourceWidth: Int
    val sourceHeight: Int
}

@JsFun(
    """(bytes, options) => {
        const image = new Image();
        const url = URL.createObjectURL(new Blob([bytes], {type:'image/svg+xml'}));
        let settled = false;
        let rejectPending;
        const release = () => {
            image.onload = null;
            image.onerror = null;
            image.src = '';
            URL.revokeObjectURL(url);
        };
        const result = new Promise((resolve, reject) => {
            rejectPending = reject;
            image.onload = () => {
                if (settled) return;
                settled = true;
                try {
                    const sourceWidth = image.naturalWidth;
                    const sourceHeight = image.naturalHeight;
                    if (!(sourceWidth > 0 && sourceHeight > 0)) throw new Error('SVG has no intrinsic size');
                    const sx = options.targetWidth > 0 ? options.targetWidth / sourceWidth : null;
                    const sy = options.targetHeight > 0 ? options.targetHeight / sourceHeight : null;
                    let scale = sx === null ? (sy === null ? 1 : sy)
                        : sy === null ? sx : options.fill ? Math.max(sx, sy) : Math.min(sx, sy);
                    if (!options.allowUpscale) scale = Math.min(scale, 1);
                    scale = Math.min(scale, options.maxWidth / sourceWidth, options.maxHeight / sourceHeight);
                    const width = Math.max(1, Math.round(sourceWidth * scale));
                    const height = Math.max(1, Math.round(sourceHeight * scale));
                    const canvas = new OffscreenCanvas(width, height);
                    const context = canvas.getContext('2d', {willReadFrequently:true});
                    if (!context) throw new Error('2D image context unavailable');
                    context.drawImage(image, 0, 0, width, height);
                    const buffer = context.getImageData(0, 0, width, height).data.buffer;
                    resolve({buffer, width, height, sourceWidth, sourceHeight});
                } catch (error) {
                    reject(error);
                } finally {
                    release();
                }
            };
            image.onerror = () => {
                if (settled) return;
                settled = true;
                release();
                reject(new Error('Invalid SVG image'));
            };
            image.src = url;
        });
        return {result, cancel() {
            if (settled) return;
            settled = true;
            release();
            rejectPending(new DOMException('SVG decode cancelled', 'AbortError'));
        }};
    }""",
)
private external fun startSvgDecode(bytes: ArrayBuffer, options: SvgDecodeOptions): SvgDecodeTask
