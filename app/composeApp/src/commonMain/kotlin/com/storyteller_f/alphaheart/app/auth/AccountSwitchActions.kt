/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.auth

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.storyteller_f.alphaheart.app.AppGlobalDialogController
import com.storyteller_f.alphaheart.app.IAccountInstance
import com.storyteller_f.alphaheart.app.UIViewModel
import com.storyteller_f.alphaheart.client.compose_core.utils.supportsChildAccountSwitching
import com.storyteller_f.alphaheart.client.core.RawUserPass
import com.storyteller_f.alphaheart.client.core.RawUserPassInfo
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.Type2Algo
import com.storyteller_f.shared.algoRunCatching
import com.storyteller_f.shared.getAlgo
import com.storyteller_f.shared.model.AlgoType
import com.storyteller_f.shared.model.ChildAccountInfo

class AccountSwitcher(val state: MutableState<Boolean> = mutableStateOf(false)) {
    fun switch() {
        state.value = true
    }
}

suspend fun AppGlobalDialogController.switchUser(childAccountInfo: ChildAccountInfo, uiViewModel: UIViewModel) {
    check(supportsChildAccountSwitching) { "Browser child account switching is unsupported" }
    val mainInstance =
        when (val instance = uiViewModel.instance.value) {
            is IAccountInstance.Child -> instance.main
            is IAccountInstance.None -> return
            is IAccountInstance.Regular -> instance
        }
    useResult {
        algoRunCatching {
            val currentUserPass = mainInstance.sessionManager.passHolder.currentUserPass
            val userPass = checkNotNull(currentUserPass) { "user not login" }
            val (decrypted, decryptedEnc) =
                userPass.decryptChildAccount(
                    childAccountInfo.encryptedPrivateKey,
                    childAccountInfo.encryptedAesKey,
                    childAccountInfo.algoType,
                    childAccountInfo.encryptedEncryptionPrivateKey,
                ).getOrThrow()
            val algoImpl = getAlgo(childAccountInfo.algoType)
            val pem = algoImpl.getPemPrivateKeyFromDer(decrypted).getOrThrow()
            val publicKey = algoImpl.getDerPublicKeyFromPrivateKey(pem).getOrThrow()

            val address = algoImpl.calcAddress(publicKey).getOrThrow()
            val authKey =
                if (childAccountInfo.algoType == com.storyteller_f.shared.model.AlgoType.DILITHIUM) {
                    checkNotNull(decryptedEnc) { "decryptedEnc is null" }
                    val type2Algo = algoImpl.encryptionAlgo as Type2Algo
                    val pemPrivateKey =
                        type2Algo.getPemEncryptionPrivateKeyFromDerPrivateKey(decryptedEnc)
                            .getOrThrow()
                    val publicEnc = type2Algo.getDerEncryptionPublicKeyFromPemPrivateKey(pemPrivateKey).getOrThrow()
                    com.storyteller_f.alphaheart.client.core.AuthKey.Dilithium(
                        pemPrivateKey = pem,
                        derPrivateKey = decrypted,
                        derPublicKey = publicKey,
                        pemEncryptionPrivateKey = pemPrivateKey, // Not implemented for child accounts
                        derEncryptionPrivateKey = decryptedEnc,
                        derEncryptionPublicKey = publicEnc,
                    )
                } else {
                    com.storyteller_f.alphaheart.client.core.AuthKey.P256(
                        pemPrivateKey = pem,
                        derPrivateKey = decrypted,
                        derPublicKey = publicKey,
                    )
                }
            uiViewModel.switchUser(mainInstance, RawUserPass(RawUserPassInfo(address, authKey)))
        }
    }
}
