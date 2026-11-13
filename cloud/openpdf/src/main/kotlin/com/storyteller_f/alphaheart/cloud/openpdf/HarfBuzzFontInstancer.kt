/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.openpdf

import com.storyteller_f.result.CustomResult
import com.sun.jna.Memory
import com.sun.jna.NativeLibrary
import com.sun.jna.Platform
import com.sun.jna.Pointer
import com.sun.jna.ptr.IntByReference

/**
 * Uses the public HarfBuzz subset ABI (8.3.1+). Calls are synchronous and own all native handles
 * until completion, including when invoked from a cancelled coroutine's blocking boundary.
 * No JDK/Skiko private library or font-registration state is used.
 */
internal class HarfBuzzFontInstancer(private val native: HarfBuzzCalls = HarfBuzzNative()) : FontInstancer {
    override val version: String get() = native.version

    override fun instantiate(
        source: ByteArray,
        faceIndex: Int,
        axes: Map<String, Float>,
    ): CustomResult<ByteArray, FontError> =
        captureFontFailure {
            val metadata = SfntFont(source, faceIndex)
            val cff2 = metadata.hasTable("CFF2")
            val staticCff = metadata.hasTable("CFF ") && !metadata.hasTable("fvar")
            require(metadata.hasTable("glyf") || staticCff || cff2) {
                "PDF instancing requires TrueType, static CFF or CFF2 outlines"
            }
            validateAxes(metadata.axes, axes)
            if (cff2) {
                check(native.invokeInt("hb_version_atleast", 13, 0, 0) != 0) {
                    "CFF2 PDF fonts require HarfBuzz 13.0.0 or newer"
                }
            }
            native.createBlob(source).use { blob ->
                native.createHandle("hb_face_create", "hb_face_destroy", blob.pointer, faceIndex).use { face ->
                    native.createHandle("hb_subset_input_create_or_fail", "hb_subset_input_destroy").use { input ->
                        native.invokeVoid("hb_subset_input_keep_everything", input.pointer)
                        if (cff2) {
                            val flags = native.invokeInt("hb_subset_input_get_flags", input.pointer)
                            // Public HB_SUBSET_FLAGS_DOWNGRADE_CFF2 (13.0+); preserve keep-everything flags.
                            native.invokeVoid("hb_subset_input_set_flags", input.pointer, flags or 0x4000)
                        }
                        if (metadata.axes.isNotEmpty()) pinAxes(input.pointer, face.pointer, axes)
                        native.createHandle(
                            "hb_subset_or_fail",
                            "hb_face_destroy",
                            face.pointer,
                            input.pointer,
                        ).use { output ->
                            native.createHandle(
                                "hb_face_reference_blob",
                                "hb_blob_destroy",
                                output.pointer,
                            ).use { result ->
                                native.readBlob(result.pointer).also { validateInstance(it, cff2) }
                            }
                        }
                    }
                }
            }
        }

    private fun validateInstance(bytes: ByteArray, cff2: Boolean) {
        val instance = SfntFont(bytes, 0)
        val static = !instance.hasTable("fvar") && !instance.hasTable("gvar")
        check(static && !instance.hasTable("CFF2")) {
            "HarfBuzz did not produce an OpenPDF-compatible static font"
        }
        if (cff2) check(instance.hasTable("CFF ")) { "Missing downgraded CFF font data" }
    }

    private fun pinAxes(input: Pointer, face: Pointer, axes: Map<String, Float>) {
        check(native.invokeInt("hb_subset_input_pin_all_axes_to_default", input, face) != 0) {
            "Cannot pin default variation axes"
        }
        axes.forEach { (tag, value) ->
            check(native.invokeInt("hb_subset_input_pin_axis_location", input, face, encodeTag(tag), value) != 0) {
                "Cannot pin variation axis $tag"
            }
        }
    }
}

internal interface FontInstancer {
    val version: String
    fun instantiate(source: ByteArray, faceIndex: Int, axes: Map<String, Float>): CustomResult<ByteArray, FontError>
}

internal fun validateAxes(available: Map<String, FontAxis>, requested: Map<String, Float>) {
    requested.forEach { (tag, value) ->
        encodeTag(tag)
        val axis = requireNotNull(available[tag]) { "Unknown variation axis $tag" }
        require(value.isFinite() && value in axis.minimum..axis.maximum) { "Variation axis $tag is outside its range" }
    }
}

/** Small JNA boundary; the process owns the library, each operation owns its handles. */
internal interface HarfBuzzCalls {
    val version: String
    fun invokeInt(name: String, vararg args: Any?): Int
    fun invokeVoid(name: String, vararg args: Any?)
    fun createHandle(name: String, destroy: String, vararg args: Any?): FontNativeHandle
    fun createBlob(bytes: ByteArray): FontNativeHandle
    fun readBlob(blob: Pointer): ByteArray
}

internal class HarfBuzzNative : HarfBuzzCalls {
    private val core by lazy {
        NativeLibrary.getInstance(System.getProperty("pdf.harfbuzz.core.library", defaultLibrary("harfbuzz")))
    }
    private val subset by lazy {
        NativeLibrary.getInstance(System.getProperty("pdf.harfbuzz.library", defaultLibrary("harfbuzz-subset")))
    }
    private fun defaultLibrary(name: String): String = if (Platform.isWindows()) "lib$name-0" else name
    private fun resolveFunction(name: String) = (if (name.startsWith("hb_subset_")) subset else core).getFunction(name)
    override val version: String by lazy {
        check(invokeInt("hb_version_atleast", 8, 3, 1) != 0) { "PDF fonts require HarfBuzz 8.3.1 or newer" }
        resolveFunction("hb_version_string").invokeString(emptyArray(), false)
    }

    override fun invokeInt(name: String, vararg args: Any?): Int = resolveFunction(name).invokeInt(args)
    override fun invokeVoid(name: String, vararg args: Any?) = resolveFunction(name).invokeVoid(args)

    override fun createHandle(name: String, destroy: String, vararg args: Any?): FontNativeHandle {
        val pointer = checkNotNull(resolveFunction(name).invokePointer(args)) { "$name failed" }
        return FontNativeHandle(pointer) { invokeVoid(destroy, it) }
    }

    override fun createBlob(bytes: ByteArray): FontNativeHandle {
        check(version.isNotEmpty())
        return Memory(bytes.size.toLong()).use { memory ->
            memory.write(0, bytes, 0, bytes.size)
            // DUPLICATE copies input synchronously; no native object borrows JNA Memory afterwards.
            createHandle("hb_blob_create", "hb_blob_destroy", memory, bytes.size, 0, Pointer.NULL, Pointer.NULL)
        }
    }

    override fun readBlob(blob: Pointer): ByteArray {
        val length = IntByReference()
        val pointer =
            checkNotNull(resolveFunction("hb_blob_get_data").invokePointer(arrayOf(blob, length))) {
                "Empty font instance"
            }
        check(length.value > 0) { "Empty font instance" }
        return pointer.getByteArray(0, length.value)
    }
}

internal class FontNativeHandle(val pointer: Pointer, private val destroy: (Pointer) -> Unit) : AutoCloseable {
    override fun close() = destroy(pointer)
}
