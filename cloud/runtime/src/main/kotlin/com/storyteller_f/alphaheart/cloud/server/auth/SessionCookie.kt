/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server.auth

import com.storyteller_f.alphaheart.backend.core.MergedEnv
import com.storyteller_f.alphaheart.backend.core.service.SigninSessionService
import com.storyteller_f.alphaheart.backend.core.service.SigninSessionUnavailableException
import com.storyteller_f.alphaheart.backend.core.service.buildSigninSessionService
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.call
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.sessions.SessionSerializer
import io.ktor.server.sessions.SessionTrackerById
import io.ktor.server.sessions.SessionsConfig
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.cookie
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessionId
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import io.ktor.util.AttributeKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
internal data class PanelSession(val session: UserSession)

private val claimedChallengeKey = AttributeKey<Unit>("ClaimedSigninChallenge")
private val trackersKey = AttributeKey<Map<String, SessionTrackerById<*>>>("SigninTrackers")
private val storesKey = AttributeKey<Map<String, SigninSessionService>>("SigninStores")

fun SessionsConfig.setupUserSessions(env: MergedEnv, application: Application) {
    val origins =
        env["SIGNIN_ALLOWED_ORIGINS"].orEmpty().split(
            ',',
        ).map { it.trim() }.filter { it.isNotBlank() }.toSet()
    application.intercept(ApplicationCallPipeline.Monitoring) {
        try {
            proceed()
        } catch (_: SigninSessionUnavailableException) {
            call.respond(HttpStatusCode.ServiceUnavailable)
            finish()
        }
    }
    application.intercept(ApplicationCallPipeline.Plugins) {
        val origin = call.request.headers[HttpHeaders.Origin]
        if (origin != null && origin !in origins) {
            call.respond(HttpStatusCode.Forbidden)
            finish()
        }
    }
    val stores = listOf("user", "panel").associateWith { buildSigninSessionService(env, it) }
    application.attributes.put(storesKey, stores)
    application.monitor.subscribe(ApplicationStopped) { stores.values.forEach { it.close() } }
    val secure = env["BUILD_TYPE"] != "test" && env["BUILD_TYPE"] != "dev"
    cookie<UserSession>("user_session", SigninSessionStorage(stores.getValue("user"))) {
        serializer =
            object : SessionSerializer<UserSession> {
                override fun serialize(session: UserSession) = Json.encodeToString<UserSession>(session)
                override fun deserialize(text: String) = Json.decodeFromString<UserSession>(text)
            }
        cookie.path = "/"
        cookie.httpOnly = true
        cookie.secure = secure
        cookie.maxAgeInSeconds = 90L * 24 * 60 * 60
        cookie.extensions["SameSite"] = "Strict"
        sendOnlyIfModified = true
    }
    cookie<PanelSession>("panel_session", SigninSessionStorage(stores.getValue("panel"))) {
        serializer =
            object : SessionSerializer<PanelSession> {
                override fun serialize(session: PanelSession) = Json.encodeToString<PanelSession>(session)
                override fun deserialize(text: String) = Json.decodeFromString<PanelSession>(text)
            }
        cookie.path = "/admin"
        cookie.httpOnly = true
        cookie.secure = secure
        cookie.maxAgeInSeconds = 90L * 24 * 60 * 60
        cookie.extensions["SameSite"] = "Strict"
        sendOnlyIfModified = true
    }
    application.attributes.put(trackersKey, providers.associate { it.name to it.tracker as SessionTrackerById<*> })
}

internal fun ApplicationCall.isPanelSession() = request.path().startsWith("/admin")

internal fun ApplicationCall.readSession(): UserSession? =
    if (isPanelSession()) sessions.get<PanelSession>()?.session else sessions.get<UserSession>()

internal fun ApplicationCall.setSession(session: UserSession) {
    if (isPanelSession()) sessions.set(PanelSession(session)) else sessions.set(session)
}

suspend fun ApplicationCall.clearSigninSession() {
    val name = if (isPanelSession()) "panel_session" else "user_session"
    application.attributes[trackersKey].getValue(name).clear(this)
    if (isPanelSession()) sessions.clear<PanelSession>() else sessions.clear<UserSession>()
}

/** Rechecks Redis rather than Ktor's per-call snapshot, including on an existing WebSocket. */
suspend fun ApplicationCall.isSigninSessionValid(active: Boolean = false): Boolean {
    if (readSession() !is UserSession.Success) return false
    val id = if (isPanelSession()) sessionId<PanelSession>() else sessionId<UserSession>()
    if (id == null) return false
    val store = application.attributes[storesKey].getValue(if (isPanelSession()) "panel" else "user")
    return try {
        store.read(id)
        if (active) store.touchSession(id) else true
    } catch (_: NoSuchElementException) {
        false
    }
}

/** Deletes every session of this account in the correct user/panel namespace. */
suspend fun ApplicationCall.clearAllSigninSessions() {
    val session = readSession() as? UserSession.Success ?: return
    application.attributes[storesKey].getValue(session.label).invalidateAccount(session.id)
    clearSigninSession()
}

/** Claims a proof once across all API instances, before creating an account or issuing a session. */
suspend fun ApplicationCall.consumeSigninChallenge(expectedType: String = "pending") {
    if (attributes.contains(claimedChallengeKey)) return
    val session = getSession()
    val id = if (isPanelSession()) sessionId<PanelSession>() else sessionId<UserSession>()
    val store = application.attributes[storesKey].getValue(if (isPanelSession()) "panel" else "user")
    if (id == null || !store.consumeChallenge(id, session.data, expectedType)) {
        throw io.ktor.server.plugins.BadRequestException("Sign-in challenge expired or already used")
    }
    attributes.put(claimedChallengeKey, Unit)
}
