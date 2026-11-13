/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.client.compose_core.CoreStrings
import com.storyteller_f.alphaheart.client.compose_core.Res
import com.storyteller_f.alphaheart.client.compose_core.cancel
import com.storyteller_f.alphaheart.client.compose_core.confirm
import com.storyteller_f.alphaheart.client.compose_core.copy
import com.storyteller_f.alphaheart.client.compose_core.edit_private_key
import com.storyteller_f.alphaheart.client.compose_core.generate
import com.storyteller_f.alphaheart.client.compose_core.private_key
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.getAlgo
import com.storyteller_f.shared.model.AlgoType
import com.storyteller_f.shared.replaceCrlf
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivateKeyInput(
    privateKey: String,
    encryptionPrivateKey: String?,
    address: String?,
    enableRandom: Boolean = true,
    algo: AlgoType = AlgoType.P256,
    onAlgoChange: (AlgoType) -> Unit = {},
    update: (String) -> Unit,
    updateEncryption: (String) -> Unit = {},
) {
    var showDialog by remember { mutableStateOf(false) }

    Row(
        modifier =
        Modifier.padding(top = 10.dp).fillMaxWidth().appiumSemantics(
            testTag = "privateKeyInput",
            description = if (privateKey.isEmpty()) "Private Key Input" else "private key loaded",
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val shape = RoundedCornerShape(10.dp)
        Row(
            modifier =
            Modifier.weight(1f)
                .border(1.dp, MaterialTheme.colorScheme.primaryContainer, shape)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                address.orEmpty(),
                modifier =
                Modifier.weight(1f).appiumSemantics(
                    description = address?.let { ACCOUNT_ADDRESS_DESCRIPTION },
                    text = address,
                ),
            )
            IconButton({
                showDialog = true
            }, modifier = Modifier.appiumSemantics(description = stringResource(Res.string.edit_private_key))) {
                Icon(Icons.Default.Edit, stringResource(Res.string.edit_private_key))
            }
        }
    }

    if (showDialog) {
        PrivateKeyDialog(privateKey, encryptionPrivateKey, enableRandom, algo, { showDialog = false }, onAlgoChange, {
            update(it)
        }) {
            updateEncryption(it)
            showDialog = false
        }
    }
}

private const val ACCOUNT_ADDRESS_DESCRIPTION = "account address"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivateKeyDialog(
    privateKey: String,
    encryptionPrivateKey: String?,
    enableRandom: Boolean,
    algo: AlgoType,
    onDismissRequest: () -> Unit,
    onAlgoChange: (AlgoType) -> Unit,
    onConfirmPrivateKey: (String) -> Unit,
    onConfirmEncryptionPrivateKey: (String) -> Unit,
) {
    BasicAlertDialog(onDismissRequest = onDismissRequest) {
        DialogContainer {
            PrivateKeyEditor(
                privateKey,
                encryptionPrivateKey,
                enableRandom,
                algo,
                onAlgoChange,
                onConfirmPrivateKey,
                onConfirmEncryptionPrivateKey,
                onDismissRequest,
            )
        }
    }
}

@Suppress("LongMethod")
@Composable
fun PrivateKeyEditor(
    privateKey: String,
    encryptionPrivateKey: String?,
    enableRandom: Boolean,
    algo: AlgoType,
    onAlgoChange: (AlgoType) -> Unit,
    onConfirmPrivateKey: (String) -> Unit,
    onConfirmEncryptionPrivateKey: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var currentKey by remember { mutableStateOf(privateKey) }
    var currentEncryptionKey by remember { mutableStateOf(encryptionPrivateKey.orEmpty()) }
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(stringResource(Res.string.edit_private_key), style = MaterialTheme.typography.titleMedium)

        AlgoTypeSelector(algo) {
            onAlgoChange(it)
        }

        // 警告提示紧跟算法选择器显示
        if (algo == AlgoType.P256) {
            Text(
                "P256 算法不够安全，可能面临未来被解密的风险",
                color = androidx.compose.ui.graphics.Color.Red,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        PrivateKeyTools(currentKey, enableRandom, algo) {
            currentKey = it
        }

        OutlinedTextField(
            value = currentKey,
            onValueChange = { currentKey = it },
            modifier =
            Modifier.fillMaxWidth().appiumSemantics(
                input = true,
                inputValue = currentKey,
                onInputValueChange = { currentKey = it },
            ),
            label = { Text(stringResource(Res.string.private_key)) },
            minLines = 3,
            maxLines = 5,
        )

        if (algo == AlgoType.DILITHIUM) {
            PrivateKeyTools(currentEncryptionKey, enableRandom, algo) {
                currentEncryptionKey = it
            }

            OutlinedTextField(
                value = currentEncryptionKey,
                onValueChange = { currentEncryptionKey = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Encryption Private Key") },
                minLines = 3,
                maxLines = 5,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onCancel) {
                Text(stringResource(Res.string.cancel))
            }
            TextButton(onClick = {
                onConfirmPrivateKey(currentKey)
                if (algo == AlgoType.DILITHIUM) {
                    onConfirmEncryptionPrivateKey(currentEncryptionKey)
                } else {
                    onConfirmEncryptionPrivateKey("")
                }
            }, modifier = Modifier.appiumSemantics(text = stringResource(Res.string.confirm))) {
                Text(stringResource(Res.string.confirm))
            }
        }
    }
}

@Composable
fun PrivateKeyTools(currentKey: String, enableRandom: Boolean, algo: AlgoType, onKeyChange: (String) -> Unit) {
    val clipboard = LocalClipboard.current
    val toast = LocalToaster.current
    val scope = rememberCoroutineScope()
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IconButton(onClick = {
            scope.launch {
                val f = FileKit.openFilePicker()
                if (f != null) {
                    onKeyChange(f.readBytes().decodeToString().replaceCrlf())
                }
            }
        }) {
            Icon(Icons.Default.FileOpen, CoreStrings.selectFile())
        }
        if (enableRandom) {
            IconButton(onClick = {
                scope.launch {
                    getAlgo(algo).generatePemKeyPair().onSuccess {
                        onKeyChange(it.first)
                    }
                }
            }) {
                Icon(Icons.Default.Casino, stringResource(Res.string.generate))
            }
        }

        IconButton(onClick = {
            scope.launch {
                clipboard.copyText(currentKey, toast)
            }
        }) {
            Icon(Icons.Default.ContentCopy, stringResource(Res.string.copy))
        }
    }
}
