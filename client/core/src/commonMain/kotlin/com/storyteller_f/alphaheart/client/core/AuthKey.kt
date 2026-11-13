/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.core

import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.AlgoDilithium
import com.storyteller_f.shared.Type2Algo
import com.storyteller_f.shared.getAlgo
import com.storyteller_f.shared.model.AlgoType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface AuthKey {
    val algo: AlgoType
    val pemPrivateKey: String
    val derPrivateKey: String
    val derPublicKey: String

    @Serializable
    @SerialName("P256")
    data class P256(
        override val pemPrivateKey: String,
        override val derPrivateKey: String,
        override val derPublicKey: String,
    ) : AuthKey {
        override val algo: AlgoType
            get() = AlgoType.P256
    }

    @Serializable
    @SerialName("Dilithium")
    data class Dilithium(
        override val pemPrivateKey: String,
        override val derPrivateKey: String,
        override val derPublicKey: String,
        val pemEncryptionPrivateKey: String,
        val derEncryptionPrivateKey: String,
        val derEncryptionPublicKey: String,
    ) : AuthKey {
        override val algo: AlgoType
            get() = AlgoType.DILITHIUM
    }
}

suspend fun getAuthKey(algo: AlgoType): AuthKey {
    val alg = getAlgo(algo)
    val priKey = alg.generatePemKeyPair().getOrThrow().first
    val derPrivateKey = alg.getDerPrivateKey(priKey).getOrThrow()
    val derPublicKey = alg.getDerPublicKeyFromPrivateKey(priKey).getOrThrow()
    return if (algo == AlgoType.DILITHIUM) {
        val encryptionAlgo = AlgoDilithium.encryptionAlgo as Type2Algo
        val pemPrivateKey = encryptionAlgo.generateEncryptionPemKeyPair().getOrThrow().first
        val derEncryptionPrivate =
            encryptionAlgo.getDerEncryptionPrivateKeyFromPemPrivateKey(pemPrivateKey).getOrThrow()
        val derEncryptionPublicKey =
            encryptionAlgo.getDerEncryptionPublicKeyFromPemPrivateKey(pemPrivateKey).getOrThrow()
        AuthKey.Dilithium(
            priKey,
            derPrivateKey,
            derPublicKey,
            pemPrivateKey,
            derEncryptionPrivate,
            derEncryptionPublicKey,
        )
    } else {
        AuthKey.P256(priKey, derPrivateKey, derPublicKey)
    }
}

suspend fun getAuthKey(algo: AlgoType, pemPrivateKey: String, pemEncryptionPrivateKey: String? = null): AuthKey {
    val alg = getAlgo(algo)
    val derPrivateKey = alg.getDerPrivateKey(pemPrivateKey).getOrThrow()
    val derPublicKey = alg.getDerPublicKeyFromPrivateKey(pemPrivateKey).getOrThrow()
    return if (algo == AlgoType.DILITHIUM) {
        checkNotNull(pemEncryptionPrivateKey) { "encryption private key is null" }
        val encryptionAlgo = AlgoDilithium.encryptionAlgo as Type2Algo
        val derEncryptionPrivate =
            encryptionAlgo.getDerEncryptionPrivateKeyFromPemPrivateKey(pemEncryptionPrivateKey).getOrThrow()
        val derEncryptionPublicKey =
            encryptionAlgo.getDerEncryptionPublicKeyFromPemPrivateKey(pemEncryptionPrivateKey).getOrThrow()
        AuthKey.Dilithium(
            pemPrivateKey,
            derPrivateKey,
            derPublicKey,
            pemEncryptionPrivateKey,
            derEncryptionPrivate,
            derEncryptionPublicKey,
        )
    } else {
        AuthKey.P256(pemPrivateKey, derPrivateKey, derPublicKey)
    }
}
