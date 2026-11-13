/*
 * This is a private project. All rights reserved.
 */

@file:OptIn(ExperimentalWasmJsInterop::class)

package com.storyteller_f.alphaheart.client.compose_core.utils

import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import coil3.size.Dimension
import coil3.size.Precision
import coil3.size.Scale
import kotlinx.coroutines.suspendCancellableCoroutine
import okio.BufferedSource
import okio.ByteString.Companion.encodeUtf8
import okio.use
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorInfo
import org.jetbrains.skia.ColorSpace
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.webext.installPixelsFromArrayBuffer
import org.jetbrains.skiko.ExperimentalSkikoApi
import org.jetbrains.skiko.wasm.onWasmReady
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.toInt8Array
import org.w3c.dom.ErrorEvent
import org.w3c.dom.MessageEvent
import org.w3c.dom.Worker
import org.w3c.dom.events.Event
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.JsArray
import kotlin.js.js
import kotlin.js.set
import kotlin.js.toJsString
import kotlin.js.unsafeCast
import kotlin.uuid.Uuid

actual fun ImageLoader.Builder.addPlatformImageDecoders(): ImageLoader.Builder {
    val configuredBuilder =
        components {
            add(AvifDecoder.Factory())
        }
    return configuredBuilder
}

private class AvifDecoder(private val source: ImageSource, private val options: Options) : Decoder {
    override suspend fun decode(): DecodeResult {
        val bytes = source.source().use { it.readByteArray() }
        val response = decodeAvif(bytes, options)
        val bitmap = response.toBitmap()
        return DecodeResult(
            image = bitmap.asImage(),
            isSampled = response.width < response.sourceWidth || response.height < response.sourceHeight,
        )
    }

    class Factory : Decoder.Factory {
        override fun create(result: SourceFetchResult, options: Options, imageLoader: ImageLoader): Decoder? {
            val isAvifMimeType = result.mimeType?.substringBefore(';') == AVIF_MIME_TYPE
            return if (isAvifMimeType || result.source.source().hasAvifHeader()) {
                AvifDecoder(result.source, options)
            } else {
                null
            }
        }
    }
}

internal fun BufferedSource.hasAvifHeader(): Boolean {
    val isAvifBrand = rangeEquals(MAJOR_BRAND_OFFSET, AvifBytes) || rangeEquals(MAJOR_BRAND_OFFSET, AvisBytes)
    return rangeEquals(FILE_TYPE_OFFSET, FtypBytes) && isAvifBrand
}

private suspend fun decodeAvif(bytes: ByteArray, options: Options): AvifWorkerResponse =
    suspendCancellableCoroutine { continuation ->
        val id = Uuid.random().toString()
        var responseListener: ((Event) -> Unit)? = null
        var errorListener: ((Event) -> Unit)? = null
        val removeListeners = {
            avifWorker.removeEventListener("message", responseListener)
            avifWorker.removeEventListener("error", errorListener)
        }
        responseListener = { event ->
            val response = (event as? MessageEvent)?.data?.unsafeCast<AvifWorkerResponse>()
            if (response != null && response.id == id && continuation.isActive) {
                removeListeners()
                if (response.kind == WORKER_RESULT_KIND) {
                    continuation.resume(response)
                } else {
                    continuation.resumeWithException(IllegalArgumentException(response.message))
                }
            }
        }
        errorListener = { event ->
            if (continuation.isActive) {
                removeListeners()
                val message = event.unsafeCast<ErrorEvent>().message
                continuation.resumeWithException(IllegalStateException("AVIF worker failed: $message"))
            }
        }
        avifWorker.addEventListener("message", responseListener)
        avifWorker.addEventListener("error", errorListener)

        val buffer = bytes.toInt8Array().buffer
        val transfer = JsArray<JsAny>().apply { set(0, buffer) }
        avifWorker.postMessage(
            createWorkerRequest(id, buffer, options),
            transfer,
        )
        continuation.invokeOnCancellation {
            removeListeners()
        }
    }

@OptIn(ExperimentalSkikoApi::class)
private suspend fun AvifWorkerResponse.toBitmap(): Bitmap {
    awaitSkikoReady()
    val bitmap = Bitmap()
    try {
        val colorInfo =
            ColorInfo(
                ColorType.RGBA_8888,
                ColorAlphaType.UNPREMUL,
                ColorSpace.sRGB,
            )
        val imageInfo = ImageInfo(colorInfo, width, height)
        check(bitmap.installPixelsFromArrayBuffer(imageInfo, buffer, imageInfo.minRowBytes)) {
            "Failed to install decoded AVIF pixels"
        }
        return bitmap.setImmutable()
    } catch (exception: Throwable) {
        bitmap.close()
        throw exception
    }
}

private suspend fun awaitSkikoReady(): Unit =
    suspendCancellableCoroutine { continuation ->
        onWasmReady {
            if (continuation.isActive) continuation.resume(Unit)
        }
    }

private fun createWorkerRequest(id: String, buffer: ArrayBuffer, options: Options): JsAny {
    val targetWidth = (options.size.width as? Dimension.Pixels)?.px ?: 0
    val targetHeight = (options.size.height as? Dimension.Pixels)?.px ?: 0
    val scale = if (options.scale == Scale.FILL) "fill" else "fit"
    val allowUpscale = options.precision == Precision.EXACT
    return createWorkerRequest(id, buffer, targetWidth, targetHeight, scale, allowUpscale)
}

private fun createWorkerRequest(
    id: String,
    buffer: ArrayBuffer,
    targetWidth: Int,
    targetHeight: Int,
    scale: String,
    allowUpscale: Boolean,
): JsAny {
    val request = createEmptyWorkerRequest()
    request.id = id
    request.data = buffer
    request.targetWidth = targetWidth
    request.targetHeight = targetHeight
    request.scale = scale
    request.allowUpscale = allowUpscale
    return request
}

private fun createWorker(): Worker {
    val blob = createWorkerBlob(AVIF_WORKER_SCRIPT)
    val url = URL.createObjectURL(blob)
    return Worker(url).also {
        URL.revokeObjectURL(url)
    }
}

private fun createEmptyWorkerRequest(): AvifWorkerRequest = js("({})")

private fun createWorkerBlob(code: String): Blob {
    val parts = JsArray<JsAny?>().apply { set(0, code.toJsString()) }
    return Blob(parts, BlobPropertyBag(type = "application/javascript"))
}

private val avifWorker by lazy(::createWorker)

private external interface AvifWorkerResponse : JsAny {
    val id: String
    val kind: String
    val message: String
    val buffer: ArrayBuffer
    val sourceWidth: Int
    val sourceHeight: Int
    val width: Int
    val height: Int
}

private external interface AvifWorkerRequest : JsAny {
    var id: String
    var data: ArrayBuffer
    var targetWidth: Int
    var targetHeight: Int
    var scale: String
    var allowUpscale: Boolean
}

private const val AVIF_MIME_TYPE = "image/avif"
private const val WORKER_RESULT_KIND = "result"
private const val FILE_TYPE_OFFSET = 4L
private const val MAJOR_BRAND_OFFSET = 8L
private val FtypBytes = "ftyp".encodeUtf8()
private val AvifBytes = "avif".encodeUtf8()
private val AvisBytes = "avis".encodeUtf8()

private const val AVIF_WORKER_SCRIPT = """
self.onmessage = async (event) => {
    const request = event.data;
    try {
        const blob = new Blob([request.data], { type: 'image/avif' });
        const source = await createImageBitmap(blob);
        const sourceWidth = source.width;
        const sourceHeight = source.height;
        const widthScale = request.targetWidth > 0 ? request.targetWidth / sourceWidth : null;
        const heightScale = request.targetHeight > 0 ? request.targetHeight / sourceHeight : null;
        let multiplier = 1;
        if (widthScale !== null && heightScale !== null) {
            multiplier = request.scale === 'fill'
                ? Math.max(widthScale, heightScale)
                : Math.min(widthScale, heightScale);
        } else if (widthScale !== null) {
            multiplier = widthScale;
        } else if (heightScale !== null) {
            multiplier = heightScale;
        }
        if (!request.allowUpscale) multiplier = Math.min(multiplier, 1);
        const width = Math.max(1, Math.round(sourceWidth * multiplier));
        const height = Math.max(1, Math.round(sourceHeight * multiplier));
        const canvas = new OffscreenCanvas(width, height);
        const context = canvas.getContext('2d', { willReadFrequently: true });
        if (!context) throw new Error('2D canvas context is unavailable');
        context.drawImage(source, 0, 0, width, height);
        source.close();
        const buffer = context.getImageData(0, 0, width, height).data.buffer;
        self.postMessage({
            id: request.id,
            kind: 'result',
            message: '',
            buffer,
            sourceWidth,
            sourceHeight,
            width,
            height,
        }, [buffer]);
    } catch (error) {
        self.postMessage({
            id: request.id,
            kind: 'error',
            message: error?.message ?? String(error),
            buffer: null,
            sourceWidth: 0,
            sourceHeight: 0,
            width: 0,
            height: 0,
        });
    }
};
"""
