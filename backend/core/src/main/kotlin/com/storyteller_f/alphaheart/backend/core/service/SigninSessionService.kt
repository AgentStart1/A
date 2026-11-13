/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.core.service

import com.storyteller_f.alphaheart.backend.core.MergedEnv
import java.util.ServiceLoader

/** Shared across HTTP and WS; reads do not renew activity and a missing/revoked ID throws NoSuchElementException. */
interface SigninSessionService : AutoCloseable {
    suspend fun read(id: String): String
    suspend fun write(id: String, value: String)
    suspend fun invalidate(id: String)
    suspend fun touchSession(id: String): Boolean

    /** Atomically consumes an unexpired matching proof, returning false on expiry/replay. */
    suspend fun consumeChallenge(id: String, data: String, expectedType: String): Boolean
    suspend fun invalidateAccount(id: Long)
}

interface SigninSessionServiceFactory {
    fun match(env: MergedEnv): Boolean

    /** The caller owns the resulting service and closes it when the application stops. */
    fun build(env: MergedEnv, namespace: String): SigninSessionService
}

fun buildSigninSessionService(env: MergedEnv, namespace: String): SigninSessionService {
    require(namespace in setOf("user", "panel")) { "Unsupported sign-in namespace" }
    val factory =
        ServiceLoader.load(SigninSessionServiceFactory::class.java).firstOrNull { it.match(env) }
            ?: error("No registered sign-in session service matches the configuration")
    return factory.build(env, namespace)
}

class SigninSessionUnavailableException : Exception("Authentication service unavailable")
