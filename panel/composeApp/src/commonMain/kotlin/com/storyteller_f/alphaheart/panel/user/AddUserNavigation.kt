/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel.user

import androidx.navigation3.runtime.NavKey
import com.storyteller_f.alphaheart.api.NewUser
import com.storyteller_f.alphaheart.client.compose_core.tasks.CustomGlobalDialogController
import com.storyteller_f.alphaheart.client.compose_core.tasks.GlobalDialogContext
import com.storyteller_f.alphaheart.client.core.addUser
import com.storyteller_f.alphaheart.panel.CustomPanelSessionManager
import com.storyteller_f.alphaheart.panel.common.AddUserViewModel
import com.storyteller_f.alphaheart.panel.common.OnUserAdded
import com.storyteller_f.alphaheart.panel.common.submitPanelRequest
import com.storyteller_f.alphaheart.panel.nickname
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.AlgoType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@SerialName("com.storyteller_f.alphaheart.panel.pages.AddUserStartScreen")
@Serializable
internal data object AddUserStartScreen : NavKey

@SerialName("com.storyteller_f.alphaheart.panel.pages.AddUserPrivateKeyScreen")
@Serializable
internal data object AddUserPrivateKeyScreen : NavKey

internal fun CustomGlobalDialogController<GlobalDialogContext<CustomPanelSessionManager>>.addUser(
    addUserViewModel: AddUserViewModel,
    showFailure: (String) -> Unit,
    dismiss: () -> Unit,
    requiredPrivateKeyMessage: String,
) {
    val nickname = addUserViewModel.nickname.value.takeIf { it.isNotBlank() }
    val aid = addUserViewModel.aid.value.takeIf { it.isNotBlank() }
    val publicKey = addUserViewModel.publicKey.value?.takeIf { it.isNotBlank() }
    if (publicKey == null) {
        showFailure(requiredPrivateKeyMessage)
        return
    }
    val algoType = addUserViewModel.algoType.value
    val authKey =
        if (algoType == AlgoType.DILITHIUM) {
            val encryptionPublicKey = addUserViewModel.encryptionPublicKey.value
            if (encryptionPublicKey == null) {
                showFailure(requiredPrivateKeyMessage)
                return
            }
            com.storyteller_f.alphaheart.api.TransferAuthKey.Dilithium(
                derPublicKey = publicKey,
                derEncryptionPublicKey = encryptionPublicKey,
            )
        } else {
            com.storyteller_f.alphaheart.api.TransferAuthKey.P256(
                derPublicKey = publicKey,
            )
        }
    val newUser = NewUser(nickname, aid, authKey)
    submitPanelRequest(
        request = {
            addUser(newUser).onSuccess {
                this@addUser.context.emitEvent(OnUserAdded(it))
            }
        },
        onSuccess = { dismiss() },
    )
}
