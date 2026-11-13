/*
 * This is a private project. All rights reserved.
 */

@file:OptIn(ExperimentalWasmJsInterop::class, ExperimentalSkikoApi::class)

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import io.github.aakira.napier.Napier
import org.jetbrains.skia.BackendTexture
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.Image
import org.jetbrains.skia.Rect
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.SurfaceOrigin
import org.jetbrains.skiko.ExperimentalSkikoApi
import org.jetbrains.skiko.WebGLMediaContext
import org.jetbrains.skiko.currentWebGLMediaContext
import org.jetbrains.skiko.pushTexture
import org.jetbrains.skiko.unregisterTexture
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny

@JsModule("wasm-media-host/gpu-bridge.mjs")
private external fun createGpuBridge(onFailure: () -> Unit): BrowserTextureBridge

private external interface BrowserTextureBridge : JsAny {
    val legacySynchronization: Boolean
    val profiling: Boolean
    fun beginStage(name: String)
    fun endStage(name: String)
    val error: String
    val hasPending: Boolean
    fun offer(frame: PixelFrame): Boolean
    fun upload(gl: JsAny): TextureTransfer?
    fun deleteUnadopted(texture: JsAny)
    fun close()
}

private external interface TextureTransfer : JsAny {
    val texture: JsAny
    val width: Int
    val height: Int
    val rotation: Int
    val flip: Boolean
}

/** Owns adopted Images; the renderer context is borrowed and never closed here. */
internal class WasmTextureRenderer {
    private var failureHandler: () -> Unit = {}
    private val bridge =
        createGpuBridge {
            close()
            failureHandler()
        }
    private val legacySynchronization = bridge.legacySynchronization
    private val profiling = bridge.profiling
    private var owner: WebGLMediaContext? = null
    private var image: Image? = null
    private var revision by mutableIntStateOf(0)
    private var closed = false
    private var rotation = 0
    private var flipped = false

    fun setFailureHandler(handler: () -> Unit) {
        failureHandler = handler
    }

    fun accept(frame: PixelFrame): Boolean {
        if (!bridge.offer(frame)) return false
        revision++
        return true
    }

    fun draw(canvas: Canvas, size: Size, onFailure: () -> Unit) {
        if (closed || revision == 0) return
        try {
            val active = currentWebGLMediaContext()
            check(owner == null || owner === active) { "Media crossed renderer contexts" }
            owner = active
            if (legacySynchronization) {
                active.use { gl, context -> renderFrame(canvas, size, gl, context) }
            } else {
                active.useCurrent { gl, context -> renderFrame(canvas, size, gl, context) }
            }
        } catch (failure: Exception) {
            Napier.e("Wasm media texture rendering failed", throwable = failure)
            close()
            onFailure()
        }
    }

    private fun renderFrame(canvas: Canvas, size: Size, gl: JsAny, context: DirectContext) {
        if (bridge.hasPending) {
            if (legacySynchronization) profileStage("flush-submit") { context.flush().submit(false) }
            try {
                bridge.upload(gl)?.let { texture -> profileStage("skia-import") { replaceImage(texture, context) } }
            } finally {
                // The guarded import restores every GL state it changes and uses a fresh
                // texture. Skia's existing state cache remains accurate; no reset/flush is needed.
                if (legacySynchronization) profileStage("reset-all") { context.resetGLAll() }
            }
        }
        check(bridge.error.isEmpty()) { "Media texture import failed" }
        image?.let { drawImage(canvas, it, size) }
    }

    private inline fun <T> profileStage(name: String, block: () -> T): T {
        if (!profiling) return block()
        bridge.beginStage(name)
        return try {
            block()
        } finally {
            bridge.endStage(name)
        }
    }

    private fun replaceImage(texture: TextureTransfer, context: DirectContext) {
        val id =
            try {
                pushTexture(texture.texture)
            } catch (failure: Exception) {
                bridge.deleteUnadopted(texture.texture)
                throw failure
            }
        val imported =
            try {
                val backend = BackendTexture.makeGL(texture.width, texture.height, false, id, 0x0DE1, 0x8058)
                try {
                    Image.adoptTextureFrom(context, backend, SurfaceOrigin.TOP_LEFT, ColorType.RGBA_8888)
                } finally {
                    backend.close()
                }
            } catch (failure: Exception) {
                unregisterTexture(id)
                bridge.deleteUnadopted(texture.texture)
                throw failure
            }
        val previous = image
        image = imported
        rotation = texture.rotation
        flipped = texture.flip
        previous?.close()
    }

    private fun drawImage(canvas: Canvas, frame: Image, size: Size) {
        val rotated = rotation % 180 != 0
        val displayWidth = if (rotated) frame.height else frame.width
        val displayHeight = if (rotated) frame.width else frame.height
        val scale = minOf(size.width / displayWidth, size.height / displayHeight)
        val width = frame.width * scale
        val height = frame.height * scale
        canvas.save()
        try {
            canvas.translate(size.width / 2f, size.height / 2f)
            canvas.scale(if (flipped) -1f else 1f, 1f)
            canvas.rotate(rotation.toFloat())
            canvas.drawImageRect(
                frame,
                Rect.makeWH(frame.width.toFloat(), frame.height.toFloat()),
                Rect.makeXYWH(-width / 2f, -height / 2f, width, height),
                SamplingMode.LINEAR,
                null,
                true,
            )
        } finally {
            canvas.restore()
        }
    }

    fun close() {
        if (closed) return
        closed = true
        bridge.close()
        val context = owner
        if (context != null && !context.isDisposed) {
            context.use { _, direct ->
                image?.close()
                direct.flush().submit(false)
            }
        } else {
            image?.close()
        }
        image = null
    }
}
