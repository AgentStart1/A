/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel.pages

import com.storyteller_f.alphaheart.client.core.getAuthKey
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.getAlgo
import com.storyteller_f.shared.loadCryptoLibIfNeed
import com.storyteller_f.shared.model.AlgoType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
internal class PanelInputHostTest {
    @Test
    internal fun p256AccountProducesMatchingAddress() =
        runTest {
            verifyAlgorithm(AlgoType.P256)
        }

    @Test
    internal fun mlDsaAccountProducesMatchingAddress() =
        runTest {
            verifyAlgorithm(AlgoType.DILITHIUM)
        }

    @Test
    internal fun closeCancelsPendingInputChanges() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val host = PanelInputHost(backgroundScope, dispatcher, dispatcher)
            host.updatePrivateKey("pending input")
            host.close()
            runCurrent()
            assertEquals("", host.privateKey.value)
        }

    private suspend fun TestScope.verifyAlgorithm(algo: AlgoType) {
        loadCryptoLibIfNeed()
        val key = getAuthKey(algo)
        val dispatcher = StandardTestDispatcher(testScheduler)
        val host = PanelInputHost(backgroundScope, dispatcher, dispatcher)
        try {
            host.updateAlgo(algo)
            host.updatePrivateKey(key.pemPrivateKey)
            if (key is com.storyteller_f.alphaheart.client.core.AuthKey.Dilithium) {
                host.updateEncryptionPrivateKey(key.pemEncryptionPrivateKey)
            }
            runCurrent()
            val parsed = host.createAuthKey()
            assertEquals(key.derPublicKey, parsed.derPublicKey)
            assertEquals(algo, parsed.algo)
            assertEquals(getAlgo(algo).calcAddress(key.derPublicKey).getOrThrow(), host.address.value)
        } finally {
            host.close()
        }
    }
}
