/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server

import com.storyteller_f.alphaheart.backend.core.CustomKeyStore
import com.storyteller_f.alphaheart.backend.core.MergedEnv
import com.storyteller_f.shared.CryptoJvm
import java.io.File
import java.security.KeyStore
import java.util.Base64

internal fun loadSnapshotKeyStore(env: MergedEnv): CustomKeyStore? {
    val encoded = env["SNAPSHOT_KEYSTORE_BASE64"]?.takeIf { it.isNotBlank() }
    val path = env["SNAPSHOT_KEYSTORE_PATH"]?.takeIf { it.isNotBlank() }
    if (encoded == null && path == null) return null
    val password =
        requireNotNull(env["SNAPSHOT_KEYSTORE_PASS"]?.takeIf { it.isNotBlank() }) {
            "SNAPSHOT_KEYSTORE_PASS is required for snapshot signing"
        }
    val bytes =
        if (encoded != null) {
            Base64.getDecoder().decode(encoded)
        } else {
            val file = File(requireNotNull(path))
            if (!file.exists()) CryptoJvm.createKeystore(password.toCharArray(), file.path)
            file.readBytes()
        }
    try {
        val keyStore = KeyStore.getInstance("PKCS12")
        bytes.inputStream().buffered().use { keyStore.load(it, password.toCharArray()) }
        require(
            keyStore.aliases().asSequence().any {
                keyStore.isKeyEntry(it) && keyStore.getKey(it, password.toCharArray()) is java.security.PrivateKey &&
                    !keyStore.getCertificateChain(it).isNullOrEmpty()
            },
        ) {
            "Snapshot keystore must contain a private key"
        }
        return CustomKeyStore(Base64.getEncoder().encodeToString(bytes), password)
    } finally {
        bytes.fill(0)
    }
}
