/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.jetbrains.skia.Bitmap

/** Owns published frames, including those not yet committed to the Compose scene. */
internal class WasmBitmapOwner {
    private val owned = mutableSetOf<Bitmap>()
    private var current by mutableStateOf<Bitmap?>(null)
    private var closed = false

    val bitmap: Bitmap?
        get() = current

    /** Always consumes [frame]; a late publication after disposal closes immediately. */
    fun accept(frame: Bitmap?) {
        if (closed) {
            frame?.close()
            return
        }
        frame?.let(owned::add)
        current = frame
    }

    /** Retire old frames after commit, retaining the displayed and newest pending frame. */
    fun retain(displayed: Bitmap?) {
        val retired = owned.filter { it !== displayed && it !== current }
        retired.forEach {
            owned.remove(it)
            it.close()
        }
    }

    fun close() {
        if (closed) return
        closed = true
        owned.forEach(Bitmap::close)
        owned.clear()
        current = null
    }
}
