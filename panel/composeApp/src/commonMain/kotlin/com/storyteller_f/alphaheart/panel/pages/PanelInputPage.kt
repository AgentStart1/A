/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.storyteller_f.alphaheart.client.compose_core.components.PrivateKeyInput
import com.storyteller_f.alphaheart.client.compose_core.tasks.request
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.alphaheart.client.core.CredentialError
import com.storyteller_f.alphaheart.client.core.RawUserPassInfo
import com.storyteller_f.alphaheart.client.core.getPanelUserSignInPass
import com.storyteller_f.alphaheart.panel.LocalPanelGlobalDialog
import com.storyteller_f.alphaheart.panel.LocalPanelUiViewModel
import com.storyteller_f.alphaheart.panel.Res
import com.storyteller_f.alphaheart.panel.admin_auth_submit
import com.storyteller_f.alphaheart.panel.admin_auth_title
import com.storyteller_f.alphaheart.panel.input.PanelInputViewModel
import com.storyteller_f.result.captureFailure
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.AlgoType
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingButton as Button

@Composable
fun PanelInputPage() {
    val viewModel: PanelInputViewModel = viewModel { PanelInputViewModel() }
    val dialogController = LocalPanelGlobalDialog.current
    val panelUiViewModel = LocalPanelUiViewModel.current
    com.storyteller_f.alphaheart.client.compose_core.components.ReadingAuthLayout(
        stringResource(Res.string.admin_auth_title),
    ) {
        val scope = rememberCoroutineScope()
        val host = viewModel.host
        val privateKey by host.privateKey.collectAsState()
        val encryptionPrivateKey by host.encryptionPrivateKey.collectAsState()
        val algo by host.algo.collectAsState()
        val address by host.address.collectAsState()

        val startSign: () -> Unit = {
            scope.launch {
                dialogController.useResult {
                    request {
                        captureFailure(CredentialError::from) {
                            val signResult =
                                getPanelUserSignInPass(
                                    host.createAuthKey(),
                                )
                            signResult to
                                historyFactory.addSession(
                                    RawUserPassInfo(
                                        signResult.address,
                                        signResult.authKey,
                                    ),
                                )
                        }
                    }
                }.onSuccess { (it, pass) ->
                    panelUiViewModel.login(it, pass)
                }
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            PrivateKeyInput(
                privateKey = privateKey,
                encryptionPrivateKey = encryptionPrivateKey,
                algo = algo,
                onAlgoChange = host::updateAlgo,
                updateEncryption = host::updateEncryptionPrivateKey,
                address = address,
                enableRandom = false,
                update = {
                    host.updatePrivateKey(it)
                },
            )
            PanelSigninButton(
                startSign,
                enabled = !address.isNullOrBlank() && (algo != AlgoType.DILITHIUM || encryptionPrivateKey.isNotBlank()),
            )
        }
    }
}

@Composable
private fun PanelSigninButton(startSign: () -> Unit, enabled: Boolean) {
    val label = stringResource(Res.string.admin_auth_submit)
    Button(
        startSign,
        modifier = Modifier.fillMaxWidth().appiumSemantics(text = label),
        enabled = enabled,
    ) {
        Text(label)
    }
}
