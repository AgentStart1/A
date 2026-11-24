/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.auth

import com.storyteller_f.alphaheart.api.SignInBody
import com.storyteller_f.alphaheart.api.SignInResponse
import com.storyteller_f.alphaheart.api.SignUpBody
import com.storyteller_f.alphaheart.app.AppGlobalDialogController
import com.storyteller_f.alphaheart.app.UIViewModel
import com.storyteller_f.alphaheart.app.common.AppNavFactory
import com.storyteller_f.alphaheart.app.error.AccountActionError
import com.storyteller_f.alphaheart.client.compose_core.tasks.request
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

internal suspend fun AppGlobalDialogController.performSignInAuth(
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

internal suspend fun AppGlobalDialogController.performSignUpAuth(
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
