/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.utils

import com.sun.jna.NativeLibrary
import com.sun.jna.Pointer
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.PointerByReference
import java.io.IOException

/** Preserves existing generic-password items without security's bounded interactive command parser. */
internal object MacKeychain {
    private const val NOT_FOUND = -25_300
    private const val DUPLICATE = -25_299
    private val security by lazy { NativeLibrary.getInstance("Security") }
    private val core by lazy { NativeLibrary.getInstance("CoreFoundation") }

    fun read(service: String, account: String): String? {
        val length = IntByReference()
        val data = PointerByReference()
        val item = find(service, account, length, data) ?: return null
        try {
            return if (length.value == 0) {
                ""
            } else {
                checkNotNull(
                    data.value,
                ).getByteArray(0, length.value).decodeToString()
            }
        } finally {
            data.value?.let { call("SecKeychainItemFreeContent", null, it) }
            release(item)
        }
    }

    fun write(service: String, account: String, value: String) {
        val bytes = value.encodeToByteArray()
        val item =
            find(service, account) ?: run {
                val serviceBytes = service.encodeToByteArray()
                val accountBytes = account.encodeToByteArray()
                val status =
                    call(
                        "SecKeychainAddGenericPassword", null, serviceBytes.size, serviceBytes,
                        accountBytes.size, accountBytes, bytes.size, bytes, null,
                    )
                if (status == 0) return
                if (status != DUPLICATE) checkStatus(status, "create")
                // Another writer may create the same item between lookup and insertion.
                find(service, account) ?: throw IOException("Keychain item disappeared during update")
            }
        try {
            checkStatus(call("SecKeychainItemModifyAttributesAndData", item, null, bytes.size, bytes), "update")
        } finally {
            release(item)
        }
    }

    fun delete(service: String, account: String) {
        val item = find(service, account) ?: return
        try {
            val status = call("SecKeychainItemDelete", item)
            if (status != NOT_FOUND) checkStatus(status, "delete")
        } finally {
            release(item)
        }
    }

    private fun find(
        service: String,
        account: String,
        length: IntByReference? = null,
        data: PointerByReference? = null,
    ): Pointer? {
        val serviceBytes = service.encodeToByteArray()
        val accountBytes = account.encodeToByteArray()
        val item = PointerByReference()
        val status =
            call(
                "SecKeychainFindGenericPassword", null, serviceBytes.size, serviceBytes,
                accountBytes.size, accountBytes, length, data, item,
            )
        if (status == NOT_FOUND) return null
        checkStatus(status, "read")
        return checkNotNull(item.value) { "Keychain lookup returned no item" }
    }

    private fun call(name: String, vararg arguments: Any?): Int = security.getFunction(name).invokeInt(arguments)

    private fun release(item: Pointer) = core.getFunction("CFRelease").invokeVoid(arrayOf(item))

    private fun checkStatus(status: Int, operation: String) {
        if (status != 0) throw IOException("Keychain $operation failed (OSStatus $status)")
    }
}
