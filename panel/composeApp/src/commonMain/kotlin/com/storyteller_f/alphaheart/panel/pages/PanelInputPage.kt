/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.storyteller_f.alphaheart.client.compose_core.CoreStrings
import com.storyteller_f.alphaheart.client.compose_core.components.CenterBox
import com.storyteller_f.alphaheart.client.compose_core.components.PrivateKeyInput
import com.storyteller_f.alphaheart.client.compose_core.components.request
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.alphaheart.client.core.CredentialError
import com.storyteller_f.alphaheart.client.core.RawUserPassInfo
import com.storyteller_f.alphaheart.client.core.getPanelUserSignInPass
import com.storyteller_f.alphaheart.panel.LocalPanelGlobalDialog
import com.storyteller_f.alphaheart.panel.LocalPanelUiViewModel
import com.storyteller_f.result.captureFailure
import com.storyteller_f.result.onSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val panelInputCoordination = Dispatchers.Default.limitedParallelism(1)

class PanelInputViewModel : ViewModel() {
    internal val host = PanelInputHost(viewModelScope, panelInputCoordination, Dispatchers.Default)

    override fun onCleared() {
        host.close()
    }
}

@Composable
fun PanelInputPage(back: () -> Unit) {
    val viewModel: PanelInputViewModel = viewModel { PanelInputViewModel() }
    val dialogController = LocalPanelGlobalDialog.current
    val panelUiViewModel = LocalPanelUiViewModel.current
    CenterBox {
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
                    back()
                }
            }
        }
        Column(modifier = Modifier.padding(20.dp)) {
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    startSign,
                    modifier = Modifier.appiumSemantics(text = CoreStrings.start(), onClick = startSign),
                ) {
                    Text(CoreStrings.start())
                }
            }
        }
    }
}
