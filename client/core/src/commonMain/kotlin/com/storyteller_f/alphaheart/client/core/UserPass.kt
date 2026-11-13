/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.core

import com.storyteller_f.alphaheart.api.CustomApi
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.andThen
import com.storyteller_f.result.captureFailure
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.result.mapError
import com.storyteller_f.shared.decryptDataByAES
import com.storyteller_f.shared.decryptMessage
import com.storyteller_f.shared.encryptDataByAES
import com.storyteller_f.shared.getAlgo
import com.storyteller_f.shared.model.AlgoType
import kotlinx.serialization.Serializable
import kotlin.io.encoding.ExperimentalEncodingApi

interface UserPass {
    suspend fun signature(data: String): CustomResult<String, CredentialError>

    suspend fun verify(signature: String, data: String): CustomResult<Boolean, CredentialError>

    suspend fun decrypt(encrypted: ByteArray, encryptedAesKey: ByteArray): CustomResult<String, CredentialError>

    suspend fun address(): CustomResult<String, CredentialError>

    suspend fun decryptChildAccount(
        encryptedPrivateKey: String,
        encryptedAesKey: String,
        childAlgoType: AlgoType,
        encryptedEncryptionPrivateKey: String? = null,
    ): CustomResult<Pair<String, String?>, CredentialError>

    suspend fun encryptChildAccount(
        childAlgoType: AlgoType,
    ): CustomResult<CustomApi.Accounts.ChildAccounts.AddChildAccountRequest, CredentialError>
}

@Serializable
data class RawUserPassInfo(val address: String, val authKey: AuthKey) {
    val algo: AlgoType
        get() = authKey.algo
}

data class RawUserPass(val rawUSerPass: RawUserPassInfo) : UserPass {
    override suspend fun signature(data: String): CustomResult<String, CredentialError> =
        getAlgo(
            rawUSerPass.algo,
        ).signature(
            rawUSerPass.authKey.derPrivateKey,
            data,
        ).mapError(CredentialError::fromError).mapError(CredentialError::fromError)

    override suspend fun verify(signature: String, data: String): CustomResult<Boolean, CredentialError> =
        getAlgo(
            rawUSerPass.algo,
        ).verify(
            rawUSerPass.authKey.derPublicKey,
            signature,
            data,
        ).mapError(CredentialError::fromError).mapError(CredentialError::fromError)

    override suspend fun decrypt(
        encrypted: ByteArray,
        encryptedAesKey: ByteArray,
    ): CustomResult<String, CredentialError> {
        val (epk, algo) =
            if (rawUSerPass.authKey is AuthKey.Dilithium) {
                rawUSerPass.authKey.derEncryptionPrivateKey to getAlgo(rawUSerPass.algo)
            } else {
                rawUSerPass.authKey.derPrivateKey to getAlgo(rawUSerPass.algo)
            }
        return algo.decryptMessage(
            epk,
            encrypted,
            encryptedAesKey,
        ).mapError(CredentialError::fromError).mapError(CredentialError::fromError)
    }

    override suspend fun address(): CustomResult<String, CredentialError> =
        getAlgo(
            rawUSerPass.algo,
        ).calcAddress(
            rawUSerPass.authKey.derPublicKey,
        ).mapError(CredentialError::fromError).mapError(CredentialError::fromError)

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun decryptChildAccount(
        encryptedPrivateKey: String,
        encryptedAesKey: String,
        childAlgoType: AlgoType,
        encryptedEncryptionPrivateKey: String?,
    ): CustomResult<Pair<String, String?>, CredentialError> {
        // First decrypt the AES key using user's private key
        val (epk, algo) =
            if (rawUSerPass.authKey is AuthKey.Dilithium) {
                rawUSerPass.authKey.derEncryptionPrivateKey to getAlgo(rawUSerPass.algo)
            } else {
                rawUSerPass.authKey.derPrivateKey to getAlgo(rawUSerPass.algo)
            }

        val aesKeyRes = algo.encryptionAlgo.kemDecrypt(epk, encryptedAesKey.hexToByteArray())
        return aesKeyRes.andThen(CredentialError::fromError) { aesKey ->
            captureFailure(CredentialError::from) {
                val decryptedPrivateKey =
                    decryptDataByAES(
                        encryptedPrivateKey.hexToByteArray(),
                        aesKey,
                    ).getOrThrow()
                val decryptedEncryptionPrivateKey =
                    encryptedEncryptionPrivateKey?.let { encrypted ->
                        decryptDataByAES(encrypted.hexToByteArray(), aesKey).getOrThrow()
                    }
                decryptedPrivateKey to decryptedEncryptionPrivateKey
            }
        }.mapError(CredentialError::fromError)
    }

    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun encryptChildAccount(
        childAlgoType: AlgoType,
    ): CustomResult<CustomApi.Accounts.ChildAccounts.AddChildAccountRequest, CredentialError> =
        captureFailure(CredentialError::from) {
            // Generate new key pair for child account
            val algo = getAlgo(childAlgoType)
            val (pemPrivateKey, pemPublicKey) = algo.generatePemKeyPair().getOrThrow()
            val derPrivateKey = algo.getDerPrivateKey(pemPrivateKey).getOrThrow()
            val derPublicKey = algo.getDerPublicKeyFromPem(pemPublicKey).getOrThrow()

            val (derEncryptionPrivateKey, derEncryptionPublicKey) =
                if (childAlgoType == AlgoType.DILITHIUM) {
                    val encAlgo = algo.encryptionAlgo as com.storyteller_f.shared.Type2Algo
                    val (encPriv, _) = encAlgo.generateEncryptionPemKeyPair().getOrThrow()
                    encAlgo.getDerEncryptionPrivateKeyFromPemPrivateKey(encPriv).getOrThrow() to
                        encAlgo.getDerEncryptionPublicKeyFromPemPrivateKey(encPriv).getOrThrow()
                } else {
                    null to null
                }

            // Encrypt the child's private key with AES
            val (encryptedPrivateKey, aesKey) = encryptDataByAES(derPrivateKey).getOrThrow()
            val encryptedEncryptionPrivateKey =
                derEncryptionPrivateKey?.let {
                    encryptDataByAES(it, aesKey).getOrThrow().toHexString()
                }

            // Get user's public key for encryption
            val authKey = rawUSerPass.authKey
            val parentAlgo = getAlgo(authKey.algo)
            val encryptedAesKey =
                if (authKey is AuthKey.Dilithium) {
                    parentAlgo.encryptionAlgo.kemEncrypt(authKey.derEncryptionPublicKey, aesKey)
                } else {
                    parentAlgo.encryptionAlgo.kemEncrypt(authKey.derPublicKey, aesKey)
                }.getOrThrow()

            CustomApi.Accounts.ChildAccounts.AddChildAccountRequest(
                encryptedPrivateKey = encryptedPrivateKey.toHexString(),
                encryptedAesKey = encryptedAesKey.toHexString(),
                derPublicKey = derPublicKey,
                algoType = childAlgoType,
                encryptedEncryptionPrivateKey = encryptedEncryptionPrivateKey,
                encryptionPublicKey = derEncryptionPublicKey,
            )
        }.mapError(CredentialError::fromError)
}
