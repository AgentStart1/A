/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.cli

import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.CryptoJvm
import com.storyteller_f.shared.decryptMessage
import com.storyteller_f.shared.encryptDataByAES
import com.storyteller_f.shared.getAlgo
import com.storyteller_f.shared.loadCryptoLibIfNeed
import com.storyteller_f.shared.model.AlgoType
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SignatureTest {
    @Test
    fun generatedKeySignsAndVerifies() =
        runTest {
            initializeCrypto()
            val algorithm = getAlgo(AlgoType.P256)
            val (privatePem, publicPem) = algorithm.generatePemKeyPair().getOrThrow()
            val privateKey = algorithm.getDerPrivateKey(privatePem).getOrThrow()
            val publicKey = algorithm.getDerPublicKeyFromPem(publicPem).getOrThrow()

            val signature = algorithm.signature(privateKey, "hello").getOrThrow()

            assertTrue(algorithm.verify(publicKey, signature, "hello").getOrThrow())
        }

    @Test
    fun sharedEncryptionRoundTrips() =
        runTest {
            initializeCrypto()
            val algorithm = getAlgo(AlgoType.P256)
            val (privatePem, publicPem) = algorithm.generatePemKeyPair().getOrThrow()
            val privateKey = algorithm.getDerPrivateKey(privatePem).getOrThrow()
            val publicKey = algorithm.getDerPublicKeyFromPem(publicPem).getOrThrow()
            val (encrypted, aesKey) = encryptDataByAES("hello").getOrThrow()
            val encryptedKey = algorithm.encryptionAlgo.kemEncrypt(publicKey, aesKey).getOrThrow()

            assertEquals("hello", algorithm.decryptMessage(privateKey, encrypted, encryptedKey).getOrThrow())
        }

    @Test
    fun legacyJvmEncryptionRoundTrips() =
        runTest {
            initializeCrypto()
            val algorithm = getAlgo(AlgoType.P256)
            val (privatePem, publicPem) = algorithm.generatePemKeyPair().getOrThrow()
            val privateKey = algorithm.getDerPrivateKey(privatePem).getOrThrow()
            val publicKey = algorithm.getDerPublicKeyFromPem(publicPem).getOrThrow()
            val (encrypted, aesKey) = CryptoJvm.encrypt("hello").getOrThrow()
            val encryptedKey = CryptoJvm.encryptAesKey(publicKey, aesKey).getOrThrow()

            assertEquals("hello", CryptoJvm.decrypt(privateKey, encrypted, encryptedKey).getOrThrow())
        }
    private fun initializeCrypto() {
        System.setProperty("LOG_PATH", File("build/test/crypto-logs").canonicalPath)
        loadCryptoLibIfNeed()
    }
}
