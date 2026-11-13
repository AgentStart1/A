/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server.auth

import com.storyteller_f.alphaheart.backend.core.Backend
import com.storyteller_f.alphaheart.backend.core.ForbiddenException
import com.storyteller_f.alphaheart.backend.core.ObjectFetch
import com.storyteller_f.alphaheart.backend.core.UserAuthData
import com.storyteller_f.alphaheart.cloud.core.service.isTwoFactorEnabled
import com.storyteller_f.alphaheart.cloud.error.ServerError
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.andThen
import com.storyteller_f.result.andThenPresent
import com.storyteller_f.result.exceptionOrNull
import com.storyteller_f.result.map
import com.storyteller_f.result.mapError
import com.storyteller_f.result.mapPresent
import com.storyteller_f.shared.finalData
import com.storyteller_f.shared.getAlgo
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.shared.type.UserStatus
import io.ktor.http.auth.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

sealed class CustomCredential(open val sig: String) {
    data class IdCredential(val id: PrimaryKey, override val sig: String) : CustomCredential(sig)
    data class AidCredential(val aid: String, override val sig: String) : CustomCredential(sig)
    data class AddressCredential(val ad: String, override val sig: String) : CustomCredential(sig)
}

private fun HttpAuthHeader.Parameterized.customCredential(): CustomCredential? {
    if (parameters.size > 2) return null
    val listMap =
        parameters.associate {
            it.name to it.value
        }
    val sig = listMap["sig"] ?: return null
    return listMap["id"]?.toLongOrNull()?.let {
        CustomCredential.IdCredential(it, sig)
    } ?: listMap["aid"]?.let {
        CustomCredential.AidCredential(it, sig)
    } ?: listMap["ad"]?.let {
        CustomCredential.AddressCredential(it, sig)
    }
}

data class CustomPrincipal(val uid: PrimaryKey, val readOnly: Boolean = false)

inline fun AuthenticationConfig.custom(name: String? = null, configure: CustomAuthProvider.Config.() -> Unit) {
    val provider = CustomAuthProvider(CustomAuthProvider.Config(name).apply(configure))
    register(provider)
}

typealias CustomValidator =
    suspend (UserSession, ApplicationCall, CustomCredential?) -> CustomResult<CustomPrincipal?, ServerError>
typealias CustomChallenge = suspend (UserSession, ApplicationCall) -> Unit

class CustomAuthProvider(private val config: Config) : AuthenticationProvider(config) {
    class Config(name: String?) : AuthenticationProvider.Config(name) {
        lateinit var validateFunction: CustomValidator
        lateinit var challengeFunction: CustomChallenge

        fun validate(f: CustomValidator) {
            validateFunction = f
        }

        fun challenge(f: CustomChallenge) {
            challengeFunction = f
        }
    }

    override suspend fun onAuthenticate(context: AuthenticationContext) {
        val call = context.call
        val session = call.getSession()
        val credential = call.customCredential()
        val result = config.validateFunction(session, call, credential)
        val principal = result.getOrNull()
        val err = result.exceptionOrNull()
        if (principal != null) {
            context.principal(name, principal)
        } else {
            val cause =
                if (credential == null) {
                    AuthenticationFailedCause.NoCredentials
                } else if (err != null) {
                    AuthenticationFailedCause.Error(err.message?.toString().orEmpty())
                } else {
                    AuthenticationFailedCause.InvalidCredentials
                }

            @Suppress("NAME_SHADOWING")
            context.challenge("CustomChallengeKey", cause) { challenge, call ->
                config.challengeFunction(session, call)
                challenge.complete()
            }
        }
    }

    private fun ApplicationCall.customCredential(): CustomCredential? {
        return when (val authHeader = request.parseAuthorizationHeader()) {
            is HttpAuthHeader.Parameterized -> {
                if (!authHeader.authScheme.equals("Custom", ignoreCase = true)) return null
                authHeader.customCredential()
            }

            else -> null
        }
    }
}

suspend fun ApplicationCall.respondUnauthorizedResponse() {
    val data = getData()
    respond(UnauthorizedResponse(HttpAuthHeader.Single("Custom", data)))
}

suspend fun ApplicationCall.saveSuccessSession(session: UserSession.Pending, id: PrimaryKey) {
    consumeSigninChallenge()
    clearSigninSession()
    setSession(UserSession.Success(session.data, session.remote, id, session.label))
}

suspend fun ApplicationCall.saveTwoFactorPendingSession(session: UserSession.Pending, id: PrimaryKey) {
    consumeSigninChallenge()
    clearSigninSession()
    setSession(UserSession.TwoFactorPending(session.data, session.remote, id, session.label))
}

suspend fun ApplicationCall.saveSuccessSession(session: UserSession.TwoFactorPending) {
    consumeSigninChallenge("two_factor_pending")
    clearSigninSession()
    setSession(UserSession.Success(session.data, session.remote, session.id, session.label))
}

fun ApplicationCall.getData(): String = getSession().data

/** An explicit sign-in gets a fresh, single-use server challenge and rotates the current credential. */
suspend fun ApplicationCall.beginSigninChallenge(): CustomResult<String, ServerError> {
    if (request.headers["X-Requested-With"] != "A-client") {
        return CustomResult.Failure(ServerError.from(ForbiddenException())).mapError(ServerError::fromError)
    }
    clearSigninSession()
    val pending = createPendingSession(request.origin.remoteAddress)
    setSession(pending)
    return CustomResult.success(pending.data)
}

/** Anonymous/background requests never issue a pending cookie that could overwrite a new login. */
fun ApplicationCall.getSession(): UserSession {
    val remote = request.origin.remoteAddress
    return when (val session = readSession()) {
        is UserSession.Success -> session
        is UserSession.Pending -> if (remote == session.remote) session else createPendingSession(remote)
        is UserSession.TwoFactorPending -> if (remote == session.remote) session else createPendingSession(remote)
        null -> createPendingSession(remote)
    }
}

@OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
private fun ApplicationCall.createPendingSession(remote: String): UserSession.Pending {
    val data = Uuid.random().toString()
    val type = if (request.path().startsWith("/admin")) "panel" else "user"
    val value = UserSession.Pending(data, remote, type)
    return value
}

fun ApplicationCall.getRateLimitKey(): Comparable<*> {
    if (request.uri == "/metrics") return "metrics"
    return when (val session = getSession()) {
        is UserSession.Success -> session.id
        is UserSession.TwoFactorPending -> session.id
        is UserSession.Pending -> session.remote
    }
}

fun Application.configureAuth(backend: Backend) {
    install(Authentication) {
        custom("user") {
            validate { session, call, _ ->
                when (session) {
                    is UserSession.Success -> {
                        val label = "user"
                        if (session.label == label &&
                            call.request.headers["X-Account-Id"]?.let { it.toLongOrNull() == session.id } != false &&
                            call.isSigninSessionValid()
                        ) {
                            CustomResult.success(CustomPrincipal(session.id))
                        } else {
                            CustomResult.success(null)
                        }
                    }

                    is UserSession.TwoFactorPending -> CustomResult.success(null)

                    is UserSession.Pending -> CustomResult.success(null)
                }.andThenPresent(ServerError::fromError) { principal ->
                    val uid = principal.uid
                    backend.database.user.getRawUser(ObjectFetch.IdFetch(uid)).andThenPresent(ServerError::fromError) {
                        CustomResult.success(principal.copy(readOnly = it.user.status == UserStatus.READ_ONLY))
                    }
                }
            }
            challenge { _, call ->
                call.respondUnauthorizedResponse()
            }
        }
        custom("admin") {
            validate { session, call, _ ->
                when (session) {
                    is UserSession.Success -> {
                        val label = "panel"
                        if (session.label == label &&
                            call.request.headers["X-Account-Id"]?.let { it.toLongOrNull() == session.id } != false &&
                            call.isSigninSessionValid()
                        ) {
                            CustomResult.success(CustomPrincipal(session.id))
                        } else {
                            CustomResult.success(null)
                        }
                    }

                    is UserSession.TwoFactorPending -> CustomResult.success(null)

                    is UserSession.Pending -> CustomResult.success(null)
                }
            }
            challenge { _, call ->
                call.respondUnauthorizedResponse()
            }
        }
    }
}

suspend fun Backend.getUserAuthData(credential: CustomCredential): CustomResult<UserAuthData?, ServerError> {
    val userDatabase = database.user
    return when (credential) {
        is CustomCredential.AidCredential -> userDatabase.getUserAuthDataByAid(credential.aid)
        is CustomCredential.IdCredential -> userDatabase.getUserAuthDataById(credential.id)
        is CustomCredential.AddressCredential -> userDatabase.getUserAuthDataByAddress(credential.ad)
    }.mapError(ServerError::fromError)
}

suspend fun ApplicationCall.checkApiRequest(
    backend: Backend,
    credential: CustomCredential,
    session: UserSession.Pending,
): CustomResult<CustomPrincipal?, ServerError> {
    if (session.label != "user") return CustomResult.success(null)
    val sig = credential.sig
    return when {
        backend.customConfig.buildType != "prod" &&
            credential is CustomCredential.IdCredential &&
            sig == credential.id.toString() -> {
            val id = credential.id
            backend.database.user.getRawUser(ObjectFetch.IdFetch(id)).mapPresent {
                saveSuccessSession(session, id)
                CustomPrincipal(id)
            }
        }

        sig.isNotBlank() && session.data.isNotBlank() -> {
            backend.getUserAuthData(credential).andThenPresent(ServerError::fromError) { (pubKey, id, algo) ->
                getAlgo(algo).verify(
                    pubKey,
                    sig,
                    finalData(session.data),
                ).andThen(ServerError::fromError) { isVerified ->
                    if (!isVerified) {
                        CustomResult.success(null)
                    } else {
                        backend.isTwoFactorEnabled(id).map {
                            if (it) {
                                null
                            } else {
                                saveSuccessSession(session, id)
                                CustomPrincipal(id)
                            }
                        }
                    }
                }
            }
        }

        else -> {
            CustomResult.success(null)
        }
    }.mapError(ServerError::fromError)
}

suspend fun Backend.getAdminAuthData(credential: CustomCredential): CustomResult<UserAuthData?, ServerError> =
    when (credential) {
        is CustomCredential.IdCredential -> database.panelAccount.getUserAuthDataById(credential.id)
        is CustomCredential.AddressCredential -> database.panelAccount.getUserAuthDataByAddress(credential.ad)
        else -> CustomResult.Failure(ServerError.from(ForbiddenException()))
    }.mapError(ServerError::fromError)

suspend fun ApplicationCall.checkAdminApiRequest(
    backend: Backend,
    credential: CustomCredential,
    session: UserSession.Pending,
): CustomResult<CustomPrincipal?, ServerError> {
    if (session.label != "panel") return CustomResult.success(null)
    val sig = credential.sig
    if (!sig.isNotBlank() || !session.data.isNotBlank()) {
        return CustomResult.success(null)
    }
    return backend.getAdminAuthData(credential).andThenPresent(ServerError::fromError) { (pubKey, id, algo) ->
        getAlgo(algo).verify(
            pubKey,
            sig,
            finalData(session.data),
        ).mapPresent {
            saveSuccessSession(session, id)
            CustomPrincipal(id)
        }
    }.mapError(ServerError::fromError)
}
