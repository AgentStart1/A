/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.utils

import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.NativeLibrary
import com.sun.jna.NativeLong
import com.sun.jna.Platform
import com.sun.jna.Pointer
import com.sun.jna.ptr.ByteByReference
import com.sun.jna.ptr.PointerByReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.createDirectories
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/** Run only on the exclusively leased macOS test account; all keychain preferences are restored. */
class MacProtectedStoreTest {
    init {
        require(Platform.isMac()) { "Run macosProtectedStoreTest on macOS" }
    }

    @Test
    fun largeUnicodeValueUsesRealKeychain() =
        runBlocking {
            withContext(Dispatchers.IO) {
                withMacKeychain {
                    val value = "synthetic 密钥 history ".repeat(3000)
                    writeProtectedValue("synthetic-session", value)
                    assertEquals(value, readProtectedValue("synthetic-session"))
                }
            }
        }

    @Test
    fun keychainValuesCanBeReplacedClearedAndDeleted() =
        runBlocking {
            withContext(Dispatchers.IO) {
                withMacKeychain {
                    val service = "desktop-keychain-test-${UUID.randomUUID()}"
                    val account = "round-trip"
                    AutoCloseable { MacKeychain.delete(service, account) }.use {
                        assertNull(MacKeychain.read(service, account))
                        val value = "账户-data-".repeat(4096)
                        MacKeychain.write(service, account, value)
                        assertEquals(value, MacKeychain.read(service, account))
                        MacKeychain.write(service, account, "replacement")
                        assertEquals("replacement", MacKeychain.read(service, account))
                        MacKeychain.write(service, account, "")
                        assertEquals("", MacKeychain.read(service, account))
                    }
                    assertNull(MacKeychain.read(service, account))
                }
            }
        }

    @Test
    fun lockedKeychainRejectsWriteAndPreservesOriginal() =
        runBlocking {
            withContext(Dispatchers.IO) {
                withMacKeychain { fixture ->
                    writeProtectedValue("synthetic-session", "original session")
                    fixture.lock()
                    assertFailsWith<IOException> { writeProtectedValue("synthetic-session", "replacement session") }
                    fixture.unlock()
                    assertEquals("original session", readProtectedValue("synthetic-session"))
                }
            }
        }
}

private class MacKeychainFixture(val keychain: Pointer, private val password: ByteArray) {
    fun lock() = KeychainTestApi.check("SecKeychainLock", keychain)
    fun unlock() = KeychainTestApi.check("SecKeychainUnlock", keychain, password.size, password, 1.toByte())
}

private suspend fun withMacKeychain(block: suspend (MacKeychainFixture) -> Unit) {
    val undo = mutableListOf<() -> Unit>()
    AutoCloseable { restoreKeychainFixture(undo) }.use {
        val root = Path.of("build", "test", "macosProtectedStoreTest").toAbsolutePath().normalize().createDirectories()
        val directory = Files.createTempDirectory(root, "protected-")
        undo.add {
            check(directory.startsWith(root))
            check(directory.toFile().deleteRecursively()) { "Cannot clean keychain fixture" }
        }
        val previousScope = System.getProperty("app.protected.storage.dir")
        undo.add {
            if (previousScope == null) {
                System.clearProperty("app.protected.storage.dir")
            } else {
                System.setProperty("app.protected.storage.dir", previousScope)
            }
        }
        System.setProperty("app.protected.storage.dir", directory.toString())
        val originalDefault = PointerByReference()
        KeychainTestApi.check("SecKeychainCopyDefault", originalDefault)
        undo.add { KeychainTestApi.release(originalDefault.value) }
        val originalSearch = PointerByReference()
        KeychainTestApi.check("SecKeychainCopySearchList", originalSearch)
        undo.add { KeychainTestApi.release(originalSearch.value) }
        val interaction = ByteByReference()
        KeychainTestApi.check("SecKeychainGetUserInteractionAllowed", interaction)
        undo.add { KeychainTestApi.check("SecKeychainSetUserInteractionAllowed", interaction.value) }
        KeychainTestApi.check("SecKeychainSetUserInteractionAllowed", 0.toByte())
        val password = "synthetic-keychain-password".encodeToByteArray()
        undo.add { password.fill(0) }
        val created = PointerByReference()
        KeychainTestApi.check(
            "SecKeychainCreate",
            directory.resolve("fixture.keychain").toString(),
            password.size,
            password,
            0.toByte(),
            null,
            created,
        )
        val keychain = checkNotNull(created.value)
        undo.add { KeychainTestApi.release(keychain) }
        undo.add { KeychainTestApi.check("SecKeychainDelete", keychain) }
        val fixture = MacKeychainFixture(keychain, password)
        fixture.unlock()
        Memory(Native.POINTER_SIZE.toLong()).use { values ->
            values.setPointer(0, keychain)
            val list = KeychainTestApi.array(values)
            try {
                undo.add { KeychainTestApi.check("SecKeychainSetSearchList", originalSearch.value) }
                KeychainTestApi.check("SecKeychainSetSearchList", list)
            } finally {
                KeychainTestApi.release(list)
            }
        }
        undo.add { KeychainTestApi.check("SecKeychainSetDefault", originalDefault.value) }
        KeychainTestApi.check("SecKeychainSetDefault", keychain)
        block(fixture)
    }
}

private fun restoreKeychainFixture(undo: List<() -> Unit>) {
    var failure: Throwable? = null
    undo.asReversed().forEach { restore ->
        try {
            restore()
        } catch (error: Throwable) {
            if (failure == null) {
                failure = error
            } else {
                failure.addSuppressed(error)
            }
        }
    }
    failure?.let { throw it }
}

private object KeychainTestApi {
    private val security by lazy { NativeLibrary.getInstance("Security") }
    private val core by lazy { NativeLibrary.getInstance("CoreFoundation") }

    fun check(name: String, vararg arguments: Any?) {
        val result = security.getFunction(name).invokeInt(arguments)
        check(result == 0) { "$name failed (OSStatus $result)" }
    }

    fun array(values: Memory): Pointer =
        checkNotNull(
            core.getFunction("CFArrayCreate").invokePointer(arrayOf(null, values, NativeLong(1), null)),
        )

    fun release(pointer: Pointer?) {
        if (pointer != null) {
            core.getFunction("CFRelease").invokeVoid(arrayOf(pointer))
        }
    }
}
