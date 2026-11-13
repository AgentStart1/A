/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel.pages

import com.storyteller_f.alphaheart.client.core.AuthKey
import com.storyteller_f.alphaheart.client.core.getAuthKey
import com.storyteller_f.shared.getAlgo
import com.storyteller_f.shared.model.AlgoType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class PanelInputHost(
    parentScope: CoroutineScope,
    private val coordination: CoroutineDispatcher,
    private val worker: CoroutineDispatcher,
) {
    private val scope =
        CoroutineScope(
            parentScope.coroutineContext + SupervisorJob(parentScope.coroutineContext[Job]) + coordination,
        )
    private val mutablePrivateKey = MutableStateFlow("")
    private val mutableEncryptionPrivateKey = MutableStateFlow("")
    private val mutableAlgo = MutableStateFlow(AlgoType.P256)

    val privateKey = mutablePrivateKey.asStateFlow()
    val encryptionPrivateKey = mutableEncryptionPrivateKey.asStateFlow()
    val algo = mutableAlgo.asStateFlow()
    val address =
        combine(privateKey, algo) { key, selectedAlgo ->
            if (key.isBlank()) {
                null
            } else {
                val implementation = getAlgo(selectedAlgo)
                implementation.getDerPublicKeyFromPrivateKey(key).getOrNull()?.let {
                    implementation.calcAddress(it).getOrNull()
                }
            }
        }.flowOn(worker).stateIn(scope, SharingStarted.Eagerly, null)

    fun updatePrivateKey(value: String) {
        scope.launch { mutablePrivateKey.value = value }
    }

    fun updateEncryptionPrivateKey(value: String) {
        scope.launch { mutableEncryptionPrivateKey.value = value }
    }

    fun updateAlgo(value: AlgoType) {
        scope.launch { mutableAlgo.value = value }
    }

    suspend fun createAuthKey(): AuthKey =
        withContext(coordination) {
            val selectedAlgo = algo.value
            val signingKey = privateKey.value
            val encryptionKey = encryptionPrivateKey.value
            withContext(worker) { getAuthKey(selectedAlgo, signingKey, encryptionKey) }
        }

    fun close() {
        scope.cancel()
    }
}
