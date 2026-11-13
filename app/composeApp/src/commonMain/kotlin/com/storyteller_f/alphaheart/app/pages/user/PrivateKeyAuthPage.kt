/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.storyteller_f.alphaheart.api.SignInBody
import com.storyteller_f.alphaheart.api.SignInResponse
import com.storyteller_f.alphaheart.api.SignUpBody
import com.storyteller_f.alphaheart.app.AppGlobalDialogController
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.LocalGlobalDialog
import com.storyteller_f.alphaheart.app.LocalUiViewModel
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.UIViewModel
import com.storyteller_f.alphaheart.app.auth_private_key_subtitle
import com.storyteller_f.alphaheart.app.common.AppNavFactory
import com.storyteller_f.alphaheart.app.error.AccountActionError
import com.storyteller_f.alphaheart.app.invalid_private_key
import com.storyteller_f.alphaheart.app.sign_in
import com.storyteller_f.alphaheart.app.sign_up
import com.storyteller_f.alphaheart.app.start_sign_in
import com.storyteller_f.alphaheart.app.start_sign_up
import com.storyteller_f.alphaheart.client.compose_core.components.PrivateKeyInput
import com.storyteller_f.alphaheart.client.compose_core.components.request
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.alphaheart.client.core.AuthKey
import com.storyteller_f.alphaheart.client.core.PendingTotpSignIn
import com.storyteller_f.alphaheart.client.core.RawUserPassInfo
import com.storyteller_f.alphaheart.client.core.SignResult
import com.storyteller_f.alphaheart.client.core.UserAuthResult
import com.storyteller_f.alphaheart.client.core.getAuthKey
import com.storyteller_f.alphaheart.client.core.getData
import com.storyteller_f.alphaheart.client.core.prepareSignInFromPrivateKey
import com.storyteller_f.alphaheart.client.core.signIn
import com.storyteller_f.alphaheart.client.core.signInTotp
import com.storyteller_f.alphaheart.client.core.signUp
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.andThen
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.result.map
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.model.AlgoType
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun PrivateKeyAuthSignUpPage() {
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
    )
}

@Composable
fun PrivateKeyAuthSignInPage() {
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
) {
    val privateKeyError by viewModel.privateKeyError.collectAsState()
    AuthPageChrome(
        title = stringResource(if (isSignUp) Res.string.sign_up else Res.string.sign_in),
        subtitle = stringResource(Res.string.auth_private_key_subtitle),
        footer = {},
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
            if (privateKeyError != null) {
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
                modifier =
                Modifier.fillMaxWidth().appiumSemantics(
                    testTag = "start_sign",
                    text = stringResource(if (isSignUp) Res.string.start_sign_up else Res.string.start_sign_in),
                ),
                shape = ButtonDefaults.shape,
            ) {
                Text(stringResource(if (isSignUp) Res.string.start_sign_up else Res.string.start_sign_in))
            }
        }
    }
}

internal fun kotlinx.coroutines.CoroutineScope.completePendingTotpSignIn(
    globalDialogController: AppGlobalDialogController,
    appNavFactory: AppNavFactory,
    pending: PendingTotpSignIn,
    code: String,
    uiViewModel: UIViewModel,
    onSuccess: () -> Unit,
) {
    launch {
        globalDialogController.useResult {
            request {
                val userInfo = signInTotp(code).getOrThrow()
                val userPass =
                    uiViewModel.historyManager.addSession(
                        RawUserPassInfo(
                            pending.address,
                            pending.authKey,
                        ),
                    )
                CustomResult.success(
                    SignResult(
                        userInfo,
                        pending.data,
                        pending.signature,
                        pending.address,
                        pending.authKey,
                    ) to userPass,
                )
            }
        }.onSuccess { (it, userPass) ->
            onSuccess()
            uiViewModel.login(
                it.address,
                it.data,
                it.signature,
                userPass,
                it.userInfo,
            )
            appNavFactory.newAppNav().gotoHome()
        }
    }
}

private suspend fun AppGlobalDialogController.performSignInAuth(
    appNav: AppNavFactory,
    privateKey: String,
    encryptionPrivateKey: String?,
    algo: AlgoType,
    uiViewModel: UIViewModel,
): PendingTotpSignIn? {
    if (privateKey.isBlank()) return null
    return performKeySignInAuth(appNav, uiViewModel) { getAuthKey(algo, privateKey, encryptionPrivateKey) }
}

internal suspend fun AppGlobalDialogController.performKeySignInAuth(
    appNav: AppNavFactory,
    uiViewModel: UIViewModel,
    loadKey: suspend () -> AuthKey,
): PendingTotpSignIn? =
    useResult {
        val sessionManager = context.sessionManager
        request {
            val authKey = loadKey()
            prepareSignInFromPrivateKey(authKey) {
                sessionManager.getData()
            }.andThen(AccountActionError::fromError) { param ->
                sessionManager.signIn(SignInBody(param.address, param.signature))
                    .map { response ->
                        when (response) {
                            is SignInResponse.Success -> {
                                val userPassInfo = RawUserPassInfo(param.address, param.authKey)
                                val userPass =
                                    uiViewModel.historyManager.addSession(userPassInfo)
                                val signResult =
                                    SignResult(
                                        response.userInfo,
                                        param.data,
                                        param.signature,
                                        param.address,
                                        param.authKey,
                                    )
                                UserAuthResult.Success(signResult, userPass)
                            }

                            SignInResponse.RequiresTotp ->
                                UserAuthResult.RequiresTotp(
                                    PendingTotpSignIn(
                                        param.authKey,
                                        param.data,
                                        param.signature,
                                        param.address,
                                    ),
                                )
                        }
                    }
            }
        }
    }.map { result ->
        when (result) {
            is UserAuthResult.Success -> {
                val signResult = result.signResult
                uiViewModel.login(
                    signResult.address,
                    signResult.data,
                    signResult.signature,
                    result.userPass,
                    signResult.userInfo,
                )
                appNav.newAppNav().gotoHome()
                null
            }

            is UserAuthResult.RequiresTotp -> result.pending
        }
    }.getOrNull()

private suspend fun AppGlobalDialogController.performSignUpAuth(
    appNav: AppNavFactory,
    privateKey: String,
    encryptionPrivateKey: String?,
    algo: AlgoType,
    uiViewModel: UIViewModel,
) {
    if (privateKey.isBlank()) return
    useResult {
        val sessionManager = context.sessionManager
        request {
            val authKey =
                getAuthKey(
                    algo,
                    privateKey,
                    encryptionPrivateKey,
                )
            prepareSignInFromPrivateKey(authKey) {
                sessionManager.getData()
            }.andThen(AccountActionError::fromError) { param ->
                val encryptionPublicKey =
                    (param.authKey as? AuthKey.Dilithium)?.derEncryptionPublicKey
                sessionManager.signUp(
                    SignUpBody(
                        param.authKey.derPublicKey,
                        param.signature,
                        encryptionPublicKey,
                    ),
                ).map { userInfo ->
                    val userPass =
                        uiViewModel.historyManager.addSession(
                            RawUserPassInfo(
                                param.address,
                                param.authKey,
                            ),
                        )
                    SignResult(
                        userInfo,
                        param.data,
                        param.signature,
                        param.address,
                        param.authKey,
                    ) to userPass
                }
            }
        }
    }.onSuccess { (it, userPass) ->
        uiViewModel.login(
            it.address,
            it.data,
            it.signature,
            userPass,
            it.userInfo,
        )
        appNav.newAppNav().gotoHome()
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
                Text("OK")
            }
        }, title = {
            Text("Two-factor authentication")
        }, text = {
            OutlinedTextField(code, {
                code = it
            }, label = {
                Text("TOTP code")
            })
        })
    }
}
