/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.openpdf

import com.sun.jna.Callback
import com.sun.jna.NativeLibrary
import com.sun.jna.Platform
import com.sun.jna.Pointer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import java.lang.ref.Reference

/** A test-only table-backed face gates a real subset call inside a synchronous C -> JVM callback. */
internal class GatedHarfBuzzCalls(private val native: HarfBuzzCalls = HarfBuzzNative()) : HarfBuzzCalls by native {
    val entered = CompletableDeferred<Unit>()
    val release = CompletableDeferred<Unit>()
    val inSubset = MutableStateFlow(false)
    val allocated = MutableStateFlow<List<String>>(emptyList())
    val releasing = MutableStateFlow<List<Int>>(emptyList())
    val released = MutableStateFlow<List<Int>>(emptyList())
    private val core by lazy {
        val name = if (Platform.isWindows()) "libharfbuzz-0" else "harfbuzz"
        NativeLibrary.getInstance(System.getProperty("pdf.harfbuzz.core.library", name))
    }

    override fun createBlob(bytes: ByteArray): FontNativeHandle = track("blob", native.createBlob(bytes))

    override fun createHandle(name: String, destroy: String, vararg args: Any?): FontNativeHandle =
        when (name) {
            "hb_face_create" -> createTableFace(destroy, args)

            "hb_subset_or_fail" -> {
                inSubset.value = true
                try {
                    track(name, native.createHandle(name, destroy, *args))
                } finally {
                    inSubset.value = false
                }
            }

            else -> track(name, native.createHandle(name, destroy, *args))
        }

    private fun createTableFace(destroy: String, args: Array<out Any?>): FontNativeHandle {
        val base = track("base face", native.createHandle("hb_face_create", destroy, *args))
        return try {
            val tables =
                FontTableReference { _, tag, _ ->
                    gateNativeCall()
                    core.getFunction("hb_face_reference_table").invokePointer(arrayOf(base.pointer, tag))
                }
            val tags =
                FontTableTags { _, start, count, output, _ ->
                    gateNativeCall()
                    core.getFunction("hb_face_get_table_tags").invokeInt(arrayOf(base.pointer, start, count, output))
                }
            val pointer =
                checkNotNull(
                    core.getFunction("hb_face_create_for_tables").invokePointer(arrayOf(tables, null, null)),
                )
            val proxy =
                FontNativeHandle(pointer) {
                    try {
                        core.getFunction("hb_face_destroy").invokeVoid(arrayOf(it))
                    } finally {
                        Reference.reachabilityFence(tables)
                        Reference.reachabilityFence(tags)
                    }
                }
            try {
                core.getFunction("hb_face_set_get_table_tags_func").invokeVoid(arrayOf(pointer, tags, null, null))
                val owned =
                    FontNativeHandle(pointer) {
                        try {
                            proxy.close()
                        } finally {
                            base.close()
                        }
                    }
                track("table face", owned)
            } catch (error: Throwable) {
                proxy.use { throw error }
            }
        } catch (error: Throwable) {
            base.use { throw error }
        }
    }

    private fun gateNativeCall() {
        if (inSubset.value && entered.complete(Unit)) {
            // A C callback cannot suspend. Its independent bridge outlives cancellation of the owner Job.
            runBlocking { release.await() }
        }
    }

    private fun track(name: String, handle: FontNativeHandle): FontNativeHandle {
        val id = allocated.value.size
        allocated.update { it + name }
        return FontNativeHandle(handle.pointer) {
            releasing.update { it + id }
            handle.close()
            released.update { it + id }
        }
    }
}

internal fun interface FontTableReference : Callback {
    fun invoke(face: Pointer, tag: Int, userData: Pointer?): Pointer?
}

internal fun interface FontTableTags : Callback {
    fun invoke(face: Pointer, start: Int, count: Pointer?, tags: Pointer?, userData: Pointer?): Int
}
