/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.utils

import com.sun.jna.Platform
import com.sun.jna.platform.win32.Crypt32Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.Locale
import kotlin.io.encoding.Base64

actual val supportsChildAccountSwitching = true

internal actual suspend fun readSigninCookies(key: String): String? = readProtectedValue(key)

internal actual suspend fun readProtectedValue(key: String): String? =
    withContext(Dispatchers.IO) {
        val name = credentialName(key)
        when {
            Platform.isWindows() ->
                protectedFile(name).takeIf { Files.exists(it) }?.let {
                    Crypt32Util.cryptUnprotectData(Files.readAllBytes(it)).decodeToString()
                }

            Platform.isMac() ->
                MacKeychain.read("A-signin", name)
                    ?.let { Base64.decode(it.trim()).decodeToString() }

            else -> LinuxSecretStore.read(name)
        }
    }

internal actual suspend fun writeSigninCookies(key: String, value: String) = writeProtectedValue(key, value)

internal actual suspend fun writeProtectedValue(key: String, value: String) =
    withContext(Dispatchers.IO) {
        val name = credentialName(key)
        when {
            Platform.isWindows() ->
                saveProtectedFile(name, Crypt32Util.cryptProtectData(value.encodeToByteArray()))

            Platform.isMac() ->
                MacKeychain.write("A-signin", name, Base64.encode(value.encodeToByteArray()))

            else -> LinuxSecretStore.write(name, value)
        }
    }

private fun protectedFile(name: String): Path =
    System.getProperty("app.protected.storage.dir")?.let { Path.of(it, name) }
        ?: Path.of(System.getProperty("user.home"), ".a", "protected-storage", name)

private fun saveProtectedFile(name: String, encrypted: ByteArray) {
    val target = protectedFile(name)
    Files.createDirectories(target.parent)
    val temporary = Files.createTempFile(target.parent, "protected-", ".tmp")
    try {
        Files.write(temporary, encrypted)
        Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
    } finally {
        Files.deleteIfExists(temporary)
    }
}

private fun credentialName(key: String): String {
    val scope = System.getProperty("app.protected.storage.dir").takeIf { Platform.isMac() || Platform.isLinux() }
    val scopedKey = if (scope == null) key else "$scope\u0000$key"
    return MessageDigest.getInstance("SHA-256").digest(scopedKey.encodeToByteArray())
        .joinToString("") { "%02x".format(Locale.ROOT, it) }
}
