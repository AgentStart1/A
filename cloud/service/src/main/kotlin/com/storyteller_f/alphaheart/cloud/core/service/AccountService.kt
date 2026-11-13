/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.core.service

import com.perraco.utils.SnowflakeFactory
import com.storyteller_f.alphaheart.api.SignInBody
import com.storyteller_f.alphaheart.api.SignInResponse
import com.storyteller_f.alphaheart.api.SignUpBody
import com.storyteller_f.alphaheart.backend.core.Backend
import com.storyteller_f.alphaheart.backend.core.CustomBadRequestException
import com.storyteller_f.alphaheart.backend.core.types.PanelAccount
import com.storyteller_f.alphaheart.backend.core.types.User
import com.storyteller_f.alphaheart.backend.core.types.toUserInfo
import com.storyteller_f.alphaheart.cloud.core.service.error.AccountError
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.andThen
import com.storyteller_f.result.captureFailure
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.result.map
import com.storyteller_f.result.mapError
import com.storyteller_f.result.requireTrue
import com.storyteller_f.result.requireValue
import com.storyteller_f.shared.Algo
import com.storyteller_f.shared.finalData
import com.storyteller_f.shared.getAlgo
import com.storyteller_f.shared.model.AlgoType
import com.storyteller_f.shared.model.PanelAccountInfo
import com.storyteller_f.shared.model.PassType
import com.storyteller_f.shared.model.UserInfo
import com.storyteller_f.shared.model.UserLogType
import com.storyteller_f.shared.obj.ob
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.shared.utils.now

suspend fun Backend.signUp(data: String, pack: SignUpBody): CustomResult<UserInfo, AccountError> {
    val f = finalData(data)
    // Simple heuristic: Dilithium keys are much larger than P256 keys.
    // P256 PEM is ~200-300 chars. Dilithium PEM is > 2000 chars.
    val algoType = if (pack.publicKey.length > 1000) AlgoType.DILITHIUM else AlgoType.P256
    val algo = getAlgo(algoType)
    return captureFailure(AccountError::from) {
        signUpInternal(algo, pack, f, algoType).toUserInfo()
    }.mapError(AccountError::fromError)
}

private suspend fun Backend.signUpInternal(algo: Algo, pack: SignUpBody, f: String, algoType: AlgoType): User {
    val verify = algo.verify(pack.publicKey, pack.signature, f).getOrThrow()
    if (!verify) {
        throw CustomBadRequestException("Verify failed")
    }
    if (!database.user.isUserNotExistsByPublicKey(pack.publicKey).getOrThrow()) {
        throw CustomBadRequestException("User exists")
    }
    val ad = algo.calcAddress(pack.publicKey).getOrThrow()
    val newId = SnowflakeFactory.nextId()
    val notificationId = SnowflakeFactory.nextId()
    val name = nameService.parse(newId)

    val encPubKey =
        if (algoType == AlgoType.DILITHIUM) {
            pack.encryptionPublicKey
        } else {
            null
        }

    val user =
        User(
            null,
            encPubKey,
            pack.publicKey,
            ad,
            null,
            name,
            newId,
            now(),
            0,
            PassType.RAW,
            algoType,
            notificationId,
        )
    return database.user.createUser(user).getOrThrow()
}

suspend fun Backend.signIn(data: String, pack: SignInBody): CustomResult<SignInServiceResponse, AccountError> {
    val f = finalData(data)
    return database.user.getRawUserAndPublicKeyByAddress(pack.address)
        .requireValue(AccountError::fromError) {
            AccountError.from(
                CustomBadRequestException("user not found"),
            )
        }.andThen(AccountError::fromError) { (rawUser, publicKey) ->
            getAlgo(
                rawUser.user.algoType,
            ).verify(publicKey, pack.signature, f).andThen(AccountError::fromError) { isVerified ->
                if (isVerified) {
                    CustomResult.success(rawUser)
                } else {
                    CustomResult.Failure(AccountError.from(CustomBadRequestException("Verify failed")))
                }
            }
        }.andThen(AccountError::fromError) { rawUser ->
            val id = rawUser.user.id
            isTwoFactorEnabled(id).andThen(AccountError::fromError) { enabled ->
                if (enabled) {
                    CustomResult.success(SignInServiceResponse.RequiresTotp(id))
                } else {
                    addUserLog(id, UserLogType.SIGN_IN, id ob ObjectType.USER)
                    processRawUserToUserInfo(listOf(rawUser)).map {
                        SignInServiceResponse.Success(it.first())
                    }
                }
            }
        }.mapError(AccountError::fromError)
}

sealed interface SignInServiceResponse {
    val uid: PrimaryKey
    val response: SignInResponse

    data class Success(val userInfo: UserInfo) : SignInServiceResponse {
        override val uid = userInfo.id
        override val response = SignInResponse.Success(userInfo)
    }

    data class RequiresTotp(override val uid: PrimaryKey) : SignInServiceResponse {
        override val response = SignInResponse.RequiresTotp
    }
}

suspend fun Backend.adminSignIn(data: String, pack: SignInBody): CustomResult<PanelAccountInfo, AccountError> {
    val f = finalData(data)
    return database.panelAccount.getRawUserAndPublicKeyByAddress(pack.address)
        .requireValue(AccountError::fromError) {
            AccountError.from(
                CustomBadRequestException("user not found"),
            )
        }.andThen(AccountError::fromError) { (rawPanelAccount, publicKey) ->
            getAlgo(
                rawPanelAccount.algoType,
            ).verify(publicKey, pack.signature, f).andThen(AccountError::fromError) { isVerified ->
                if (isVerified) {
                    CustomResult.success(rawPanelAccount)
                } else {
                    CustomResult.Failure(AccountError.from(CustomBadRequestException("Verify failed")))
                }
            }
        }.map { rawPanelAccount ->
            val id = rawPanelAccount.id
            PanelAccountInfo(id, rawPanelAccount.name)
        }.mapError(AccountError::fromError)
}

suspend fun Backend.adminSignUp(data: String, pack: SignUpBody): CustomResult<PanelAccountInfo, AccountError> {
    val f = finalData(data)
    return getAlgo(AlgoType.P256).run {
        verify(pack.publicKey, pack.signature, f).requireTrue(AccountError::fromError) {
            AccountError.from(
                CustomBadRequestException("Verify failed"),
            )
        }.andThen(AccountError::fromError) {
            database.panelAccount.isUserNotExistsByPublicKey(
                pack.publicKey,
            ).requireTrue(AccountError::fromError) {
                AccountError.from(
                    CustomBadRequestException("User exists"),
                )
            }
        }.andThen(AccountError::fromError) {
            calcAddress(pack.publicKey)
        }.andThen(AccountError::fromError) { ad ->
            val newId = SnowflakeFactory.nextId()
            val name = nameService.parse(newId)
            val user = PanelAccount(newId, name, PassType.RAW, AlgoType.P256, pack.publicKey, ad)
            database.panelAccount.addPanelAccount(user).map {
                PanelAccountInfo(newId, name)
            }
        }
    }.mapError(AccountError::fromError)
}
