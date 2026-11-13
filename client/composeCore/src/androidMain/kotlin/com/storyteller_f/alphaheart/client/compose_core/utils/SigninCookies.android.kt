/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.utils

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.storyteller_f.shared.getAppContextRefValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.io.encoding.Base64

private const val SESSION_KEY_ALIAS = "signin-cookie-encryption"

actual val supportsChildAccountSwitching = true

internal actual suspend fun readSigninCookies(key: String): String? = readProtectedValue(key)

internal actual suspend fun readProtectedValue(key: String): String? =
    withContext(Dispatchers.IO) {
        val value = protectedPreferences().getString(key, null) ?: return@withContext null
        val encrypted = Base64.decode(value)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, sessionKey(), GCMParameterSpec(128, encrypted.copyOfRange(0, 12)))
        cipher.doFinal(encrypted.copyOfRange(12, encrypted.size)).decodeToString()
    }

internal actual suspend fun writeSigninCookies(key: String, value: String) = writeProtectedValue(key, value)

internal actual suspend fun writeProtectedValue(key: String, value: String) =
    withContext(Dispatchers.IO) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, sessionKey())
        check(
            protectedPreferences().edit()
                .putString(key, Base64.encode(cipher.iv + cipher.doFinal(value.encodeToByteArray())))
                .commit(),
        ) { "Protected storage write failed" }
    }

private fun protectedPreferences() =
    checkNotNull(getAppContextRefValue()).getSharedPreferences("protected-data", Context.MODE_PRIVATE)

private fun sessionKey(): SecretKey {
    val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    val existing = store.getKey(SESSION_KEY_ALIAS, null) as? SecretKey
    if (existing != null) return existing
    return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
        init(
            KeyGenParameterSpec.Builder(
                SESSION_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build(),
        )
    }.generateKey()
}
