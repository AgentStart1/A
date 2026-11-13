/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storyteller_f.result.onFailure
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.getAlgo
import com.storyteller_f.shared.model.AlgoType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InputPrivateKeyViewModel : ViewModel() {
    val privateKey = MutableStateFlow("")
    val encryptionPrivateKey = MutableStateFlow("")
    val algo = MutableStateFlow(AlgoType.P256)
    val privateKeyError = MutableStateFlow<String?>(null)

    val publicKey =
        privateKey.combine(algo) { privateKey, algo ->
            if (privateKey.isEmpty()) {
                null
            } else {
                getAlgo(algo).run {
                    getDerPublicKeyFromPrivateKey(privateKey)
                        .onSuccess { privateKeyError.value = null }
                        .onFailure { throwable ->
                            privateKeyError.value = throwable.cause.message ?: throwable.cause::class.simpleName
                        }
                        .getOrNull()
                }
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val address =
        publicKey.combine(algo) { publicKey, algo ->
            if (publicKey == null) {
                null
            } else {
                getAlgo(algo).run {
                    calcAddress(publicKey)
                        .onSuccess { privateKeyError.value = null }
                        .onFailure { throwable ->
                            privateKeyError.value = throwable.cause.message ?: throwable.cause::class.simpleName
                        }
                        .getOrNull()
                }
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun updatePrivateKey(privateKey: String) {
        this.privateKey.value = privateKey
    }

    fun updateEncryptionPrivateKey(encryptionPrivateKey: String) {
        this.encryptionPrivateKey.value = encryptionPrivateKey
    }

    fun updateAlgo(algo: AlgoType) {
        this.algo.value = algo
    }

    fun autoGeneratePrivateKey() {
        viewModelScope.launch {
            val algo1 = getAlgo(algo.value)
            algo1.generatePemKeyPair().onSuccess { (privateKey, _) ->
                updatePrivateKey(privateKey)
            }
            if (algo.value == AlgoType.DILITHIUM) {
                val algoImpl = algo1.encryptionAlgo as com.storyteller_f.shared.Type2Algo
                algoImpl.generateEncryptionPemKeyPair().onSuccess { (encPrivateKey, _) ->
                    updateEncryptionPrivateKey(encPrivateKey)
                }
            }
        }
    }
}
