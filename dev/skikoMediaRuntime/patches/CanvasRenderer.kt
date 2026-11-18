/*
 * This is a private project. All rights reserved.
 */

@file:OptIn(ExperimentalSkikoApi::class)
// Modified from Skiko 0.150.2; see ../NOTICE and ../LICENSE.
package org.jetbrains.skiko

import kotlinx.browser.window
import org.jetbrains.skia.*
import org.jetbrains.skia.impl.NativePointer
import org.jetbrains.skiko.wasm.ContextAttributes
import org.w3c.dom.HTMLCanvasElement

/**
 * CanvasRenderer takes an [HTMLCanvasElement] instance and initializes
 * skiko's [Canvas] used for drawing (see [initCanvas]).
 *
 * After initialization [needRedraw] can be used to schedule a call to [drawFrame].
 * [drawFrame] has to be implemented to perform the actual drawing on [canvas].
 */
internal abstract class CanvasRenderer(
    private val contextPointer: NativePointer,
    width: Int,
    height: Int,
) {
    var width: Int = width
        private set
    var height: Int = height
        private set

    private val context: DirectContext
    private var surface: Surface? = null
    private var renderTarget: BackendRenderTarget? = null

    /**
     * An instance of skiko [Canvas] used for drawing.
     * Created in [initCanvas].
     */
    protected var canvas: Canvas? = null
        private set

    init {
        GL.makeContextCurrent(contextPointer)
        context = DirectContext.makeGL()
        initCanvas()
        mediaContexts[contextPointer] = WebGLMediaContext(contextPointer, context)
    }

    fun initCanvas() {
        disposeCanvas()

        renderTarget = BackendRenderTarget.makeGL(width, height, 1, 8, 0, 0x8058)
        surface = Surface.makeFromBackendRenderTarget(
            context,
            renderTarget!!,
            SurfaceOrigin.BOTTOM_LEFT,
            SurfaceColorFormat.RGBA_8888,
            ColorSpace.sRGB,
            SurfaceProps()
        ) ?: throw RenderException("Cannot create surface")
        canvas = surface!!.canvas
    }

    private fun disposeCanvas() {
        surface?.close()
        surface = null
        renderTarget?.close()
        renderTarget = null
    }

    /**
     * This function should implement the actual drawing on the canvas.
     *
     * @param currentTimestamp - in milliseconds
     */
    abstract fun drawFrame(currentTimestamp: Double)

    fun resize(width: Int, height: Int) {
        check(!disposed)
        if (width == this.width && height == this.height) return
        this.width = width
        this.height = height
        GL.makeContextCurrent(contextPointer)
        initCanvas()
    }

    private var disposed = false
    fun dispose() {
        if (disposed) return
        disposed = true
        GL.makeContextCurrent(contextPointer)
        disposeCanvas()
        mediaContexts.remove(contextPointer)?.isDisposed = true
        canvas = null
        context.close()
        GL.deleteContext(contextPointer)
    }

    private var redrawScheduled = false

    /**
     * Schedules a call to [drawFrame] to the appropriate moment.
     */
    fun needRedraw() {
        if (disposed || redrawScheduled) {
            return
        }
        redrawScheduled = true
        window.requestAnimationFrame { timestamp ->
            redrawScheduled = false
            if (disposed) return@requestAnimationFrame
            GL.makeContextCurrent(contextPointer)
            // `clear` and `resetMatrix` make canvas not accumulate previous effects
            canvas?.clear(Color.WHITE)
            canvas?.resetMatrix()
            drawFrame(timestamp)
            surface?.flushAndSubmit()
            context.flush()
        }
    }
}

internal external interface GLInterface {
    fun createContext(context: HTMLCanvasElement, contextAttributes: ContextAttributes): NativePointer
    fun makeContextCurrent(contextPointer: NativePointer): Boolean;
    fun deleteContext(contextPointer: NativePointer)
}

internal expect val GL: GLInterface