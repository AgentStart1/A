/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.LocalGlobalDialog
import com.storyteller_f.alphaheart.app.LocalUiViewModel
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.auth.InputPrivateKeyViewModel
import com.storyteller_f.alphaheart.app.auth.completePendingTotpSignIn
import com.storyteller_f.alphaheart.app.auth.performSignInAuth
import com.storyteller_f.alphaheart.app.auth.performSignUpAuth
import com.storyteller_f.alphaheart.app.invalid_private_key
import com.storyteller_f.alphaheart.app.sign_in
import com.storyteller_f.alphaheart.app.sign_up
import com.storyteller_f.alphaheart.app.start_sign_in
import com.storyteller_f.alphaheart.app.start_sign_up
import com.storyteller_f.alphaheart.app.totp_code
import com.storyteller_f.alphaheart.app.two_factor_authentication
import com.storyteller_f.alphaheart.client.compose_core.CoreStrings
import com.storyteller_f.alphaheart.client.compose_core.components.PrivateKeyInput
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.alphaheart.client.core.PendingTotpSignIn
import com.storyteller_f.shared.model.AlgoType
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingAlertDialog as AlertDialog
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingButton as Button
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingOutlinedTextField as OutlinedTextField

@Composable
fun PrivateKeyAuthSignUpPage(navigation: (@Composable () -> Unit)? = null, footer: @Composable () -> Unit = {}) {
    val viewModel = viewModel { InputPrivateKeyViewModel() }
    val privateKey by viewModel.privateKey.collectAsState()
    val uIViewModel = LocalUiViewModel.current
    val encryptionPrivateKey by viewModel.encryptionPrivateKey.collectAsState()
    val address by viewModel.address.collectAsState()
    val algo by viewModel.algo.collectAsState()
    val scope = rememberCoroutineScope()
    val appNavFactory = LocalAppNavFactory.current
    val globalDialogController = LocalGlobalDialog.current

    PrivateKeyAuthContent(
        isSignUp = true,
        privateKey = privateKey,
        encryptionPrivateKey = encryptionPrivateKey,
        address = address,
        algo = algo,
        startSign = {
            scope.launch {
                globalDialogController.performSignUpAuth(
                    appNavFactory,
                    privateKey,
                    encryptionPrivateKey,
                    algo,
                    uIViewModel,
                )
            }
        },
        viewModel = viewModel,
        navigation = navigation,
        footer = footer,
    )
}

@Composable
fun PrivateKeyAuthSignInPage(navigation: (@Composable () -> Unit)? = null, footer: @Composable () -> Unit = {}) {
    val viewModel = viewModel { InputPrivateKeyViewModel() }
    val uIViewModel = LocalUiViewModel.current
    val privateKey by viewModel.privateKey.collectAsState()
    val encryptionPrivateKey by viewModel.encryptionPrivateKey.collectAsState()
    val address by viewModel.address.collectAsState()
    val algo by viewModel.algo.collectAsState()
    val scope = rememberCoroutineScope()
    val appNavFactory = LocalAppNavFactory.current
    val globalDialogController = LocalGlobalDialog.current
    var pendingTotp by remember { mutableStateOf<PendingTotpSignIn?>(null) }

    PrivateKeyAuthContent(
        isSignUp = false,
        privateKey = privateKey,
        encryptionPrivateKey = encryptionPrivateKey,
        address = address,
        algo = algo,
        startSign = {
            scope.launch {
                val pending =
                    globalDialogController.performSignInAuth(
                        appNavFactory,
                        privateKey,
                        encryptionPrivateKey,
                        algo,
                        uIViewModel,
                    )
                if (pending != null) {
                    pendingTotp = pending
                }
            }
        },
        viewModel = viewModel,
        navigation = navigation,
        footer = footer,
    )
    TotpSignInDialog(pendingTotp, {
        pendingTotp = null
    }) { pending, code ->
        scope.completePendingTotpSignIn(globalDialogController, appNavFactory, pending, code, uIViewModel) {
            pendingTotp = null
        }
    }
}

@Composable
private fun PrivateKeyAuthContent(
    isSignUp: Boolean,
    privateKey: String,
    encryptionPrivateKey: String?,
    address: String?,
    algo: AlgoType,
    startSign: () -> Unit,
    viewModel: InputPrivateKeyViewModel,
    navigation: (@Composable () -> Unit)?,
    footer: @Composable () -> Unit,
) {
    val privateKeyError by viewModel.privateKeyError.collectAsState()
    AuthPageChrome(
        title = stringResource(if (isSignUp) Res.string.sign_up else Res.string.sign_in),
        navigation = navigation,
        footer = footer,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PrivateKeyInput(privateKey, encryptionPrivateKey, address, isSignUp, algo, {
                viewModel.updateAlgo(it)
            }, {
                viewModel.updatePrivateKey(it)
            }) {
                viewModel.updateEncryptionPrivateKey(it)
            }
            if (privateKey.isNotBlank() && privateKeyError != null) {
                val errorText = stringResource(Res.string.invalid_private_key)
                Text(
                    text = errorText,
                    color = MaterialTheme.colorScheme.error,
                    modifier =
                    Modifier.appiumSemantics(
                        description = "private key error",
                        text = errorText,
                    ),
                )
            }
            Button(
                onClick = startSign,
                enabled =
                !address.isNullOrBlank() && privateKeyError == null &&
                    (algo != AlgoType.DILITHIUM || !encryptionPrivateKey.isNullOrBlank()),
                modifier =
                Modifier.fillMaxWidth().appiumSemantics(
                    testTag = "start_sign",
                    text = stringResource(if (isSignUp) Res.string.start_sign_up else Res.string.start_sign_in),
                ),
            ) {
                Text(stringResource(if (isSignUp) Res.string.start_sign_up else Res.string.start_sign_in))
            }
        }
    }
}

@Composable
internal fun TotpSignInDialog(
    pending: PendingTotpSignIn?,
    onDismiss: () -> Unit,
    onConfirm: (PendingTotpSignIn, String) -> Unit,
) {
    var code by remember(pending) { mutableStateOf("") }
    if (pending != null) {
        AlertDialog(onDismiss, {
            Button({
                onConfirm(pending, code)
            }) {
                Text(CoreStrings.ok())
            }
        }, title = {
            Text(stringResource(Res.string.two_factor_authentication))
        }, text = {
            OutlinedTextField(code, {
                code = it
            }, label = {
                Text(stringResource(Res.string.totp_code))
            })
        })
    }
}
