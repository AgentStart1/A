/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.utils

import com.sun.jna.Memory
import com.sun.jna.NativeLibrary
import com.sun.jna.NativeLong
import com.sun.jna.Pointer
import com.sun.jna.ptr.NativeLongByReference
import com.sun.jna.ptr.PointerByReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException

/** Uses libsecret directly: secret-tool's stdin reader silently truncates values at 8192 bytes. */
internal object LinuxSecretStore {
    private val api by lazy {
        try {
            NativeSecretApi(secretLibraryName())
        } catch (failure: UnsatisfiedLinkError) {
            throw IOException("System credential store unavailable", failure)
        }
    }

    suspend fun read(name: String): String? = call { native, cancellation -> native.read(name, cancellation) }

    suspend fun write(name: String, value: String) =
        call { native, cancellation ->
            native.write(name, value, cancellation)
        }

    private suspend fun <T> call(operation: (NativeSecretApi, Pointer) -> T): T =
        supervisorScope {
            val native = api
            val cancellation = native.newCancellation()
            val work = async(Dispatchers.IO) { operation(native, cancellation) }
            try {
                val completed =
                    withTimeoutOrNull(10_000) { work.await() to Unit }
                        ?: throw IOException("System credential store timed out")
                completed.first
            } finally {
                // Cancellation must reach the native call before its arguments and GCancellable are released.
                withContext(NonCancellable) {
                    native.cancel(cancellation)
                    work.join()
                    native.release(cancellation)
                }
            }
        }
}

private fun secretLibraryName(): String =
    if (System.getProperty("appium.session.file") != null) {
        System.getProperty("appium.protected.storage.library") ?: "secret-1"
    } else {
        "secret-1"
    }

private fun loadSecretLibrary(name: String): NativeLibrary =
    try {
        NativeLibrary.getInstance(name)
    } catch (failure: UnsatisfiedLinkError) {
        throw IOException("System credential store unavailable: $name", failure)
    }

private class NativeSecretApi(library: String) {
    private val secret = loadSecretLibrary(library)
    private val glib = loadSecretLibrary("glib-2.0")
    private val gio = loadSecretLibrary("gio-2.0")
    private val objects = loadSecretLibrary("gobject-2.0")
    private val lookup = secret.getFunction("secret_password_lookupv_binary_sync")
    private val store = secret.getFunction("secret_password_storev_binary_sync")
    private val newValue = secret.getFunction("secret_value_new")
    private val getValue = secret.getFunction("secret_value_get")
    private val releaseValue = secret.getFunction("secret_value_unref")
    private val newTable = glib.getFunction("g_hash_table_new_full")
    private val insert = glib.getFunction("g_hash_table_insert")
    private val releaseTable = glib.getFunction("g_hash_table_unref")
    private val hash = glib.getFunction("g_str_hash")
    private val equal = glib.getFunction("g_str_equal")
    private val releaseError = glib.getFunction("g_error_free")
    private val cancellationNew = gio.getFunction("g_cancellable_new")
    private val cancellationCancel = gio.getFunction("g_cancellable_cancel")
    private val releaseObject = objects.getFunction("g_object_unref")

    fun newCancellation(): Pointer = checkNotNull(cancellationNew.invokePointer(emptyArray()))

    fun cancel(cancellation: Pointer) = cancellationCancel.invokeVoid(arrayOf(cancellation))

    fun release(cancellation: Pointer) = releaseObject.invokeVoid(arrayOf(cancellation))

    fun read(name: String, cancellation: Pointer): String? =
        withAttributes(name) { attributes ->
            val error = PointerByReference()
            val value = lookup.invokePointer(arrayOf(null, attributes, cancellation, error))
            AutoCloseable { if (value != null) releaseValue.invokeVoid(arrayOf(value)) }.use {
                checkError(error)
                if (value == null) return@withAttributes null
                val length = NativeLongByReference()
                val data = getValue.invokePointer(arrayOf(value, length))
                val size = length.value.toLong()
                check(size in 0..Int.MAX_VALUE.toLong()) { "System credential value exceeds supported size" }
                if (size == 0L) return@withAttributes ""
                val bytes = checkNotNull(data).getByteArray(0, size.toInt())
                try {
                    bytes.decodeToString(throwOnInvalidSequence = true)
                } finally {
                    bytes.fill(0)
                }
            }
        }

    fun write(name: String, text: String, cancellation: Pointer) =
        withAttributes(name) { attributes ->
            val bytes = text.encodeToByteArray()
            try {
                Memory(maxOf(1, bytes.size).toLong()).use { data ->
                    try {
                        data.write(0, bytes, 0, bytes.size)
                        val value =
                            checkNotNull(
                                newValue.invokePointer(arrayOf(data, NativeLong(bytes.size.toLong()), "text/plain")),
                            )
                        AutoCloseable { releaseValue.invokeVoid(arrayOf(value)) }.use {
                            val error = PointerByReference()
                            val stored =
                                store.invokeInt(
                                    arrayOf(null, attributes, null, "A sign-in", value, cancellation, error),
                                )
                            checkError(error)
                            check(stored != 0) { "System credential store unavailable" }
                        }
                    } finally {
                        data.clear()
                    }
                }
            } finally {
                bytes.fill(0)
            }
        }

    private fun <T> withAttributes(name: String, block: (Pointer) -> T): T {
        val strings = mutableListOf<Memory>()
        val attributes = checkNotNull(newTable.invokePointer(arrayOf(hash, equal, null, null)))
        AutoCloseable {
            releaseTable.invokeVoid(arrayOf(attributes))
            strings.forEach(Memory::close)
        }.use {
            listOf("application" to "A-signin", "account" to name).forEach { (key, value) ->
                insert.invokeInt(arrayOf(attributes, strings.utf8(key), strings.utf8(value)))
            }
            return block(attributes)
        }
    }

    private fun checkError(reference: PointerByReference) {
        val error = reference.value ?: return
        val message =
            AutoCloseable { releaseError.invokeVoid(arrayOf(error)) }.use {
                // GError starts with two 32-bit integers, followed by its UTF-8 message pointer.
                error.getPointer(8).getString(0, "UTF-8")
            }
        throw IOException("System credential store unavailable: $message")
    }
}

private fun MutableList<Memory>.utf8(value: String): Memory {
    val bytes = value.encodeToByteArray()
    val memory = Memory(bytes.size.toLong() + 1)
    add(memory)
    memory.write(0, bytes, 0, bytes.size)
    memory.setByte(bytes.size.toLong(), 0)
    return memory
}
