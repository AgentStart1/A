/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.core

import com.storyteller_f.result.captureFailure
import com.storyteller_f.result.fold
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.finalData
import io.github.aakira.napier.Napier
import io.ktor.client.call.HttpClientCall
import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.util.AttributeKey
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val CustomAuthRetryAttribute = AttributeKey<Unit>("CustomAuthRetry")

class SingleFlightCustomAuthConfig {
    internal lateinit var addRequestHeaders: suspend (HttpRequestBuilder) -> Unit
    internal lateinit var refreshSignature: suspend (String) -> Boolean

    fun addRequestHeaders(block: suspend (HttpRequestBuilder) -> Unit) {
        addRequestHeaders = block
    }

    fun refreshSignature(block: suspend (String) -> Boolean) {
        refreshSignature = block
    }
}

/** Authentication plugin that coalesces concurrent signature refreshes. */
val SingleFlightCustomAuthPlugin: ClientPlugin<SingleFlightCustomAuthConfig> =
    createClientPlugin(
        "SingleFlightCustomAuthPlugin",
        ::SingleFlightCustomAuthConfig,
    ) {
        val addRequestHeaders = pluginConfig.addRequestHeaders
        val refreshSignature = pluginConfig.refreshSignature
        val refreshGate = AuthRefreshGate()

        on(Send) { originalRequest ->
            val origin = proceed(originalRequest)
            if (origin.response.status != HttpStatusCode.Unauthorized) return@on origin
            if (origin.request.attributes.contains(CustomAuthRetryAttribute)) return@on origin

            val challengeData = origin.response.customChallengeData() ?: return@on origin
            when (val turn = refreshGate.enter()) {
                is AuthRefreshTurn.Leader -> {
                    var success = false
                    try {
                        if (!refreshSignature(challengeData)) {
                            Napier.i(tag = "SingleFlight") {
                                "Leader failed because refresh signature failed"
                            }
                            return@on origin
                        }
                        val retriedCall = executeWithCustomAuth(originalRequest, addRequestHeaders)
                        if (retriedCall.response.status == HttpStatusCode.Unauthorized) {
                            Napier.i(tag = "SingleFlight") {
                                "Leader retry failed"
                            }
                            return@on retriedCall
                        }
                        success = true
                        retriedCall
                    } finally {
                        refreshGate.complete(turn, success)
                    }
                }

                is AuthRefreshTurn.Waiter -> {
                    if (!turn.result.await()) {
                        Napier.i(tag = "SingleFlight") {
                            "Waiter failed because of Leader failed"
                        }
                        return@on origin
                    }
                    executeWithCustomAuth(originalRequest, addRequestHeaders)
                }
            }
        }
    }

private suspend fun AuthRefreshGate.complete(turn: AuthRefreshTurn.Leader, success: Boolean) {
    complete(turn.result, success)
}

@OptIn(io.ktor.utils.io.InternalAPI::class)
private suspend fun Send.Sender.executeWithCustomAuth(
    originalRequest: HttpRequestBuilder,
    addRequestHeaders: suspend (HttpRequestBuilder) -> Unit,
): HttpClientCall {
    val request = HttpRequestBuilder()
    request.takeFromWithExecutionContext(originalRequest)
    request.headers.remove(HttpHeaders.Authorization)
    addRequestHeaders(request)
    request.attributes.put(CustomAuthRetryAttribute, Unit)
    return proceed(request)
}

private fun HttpResponse.customChallengeData(): String? {
    val headerValues = headers.getAll(HttpHeaders.WWWAuthenticate).orEmpty()
    return headerValues.firstNotNullOfOrNull { header ->
        header.splitToSequence(',')
            .map { it.trim() }
            .firstNotNullOfOrNull { challenge ->
                val scheme = challenge.substringBefore(' ')
                if (!scheme.equals("Custom", ignoreCase = true)) {
                    null
                } else {
                    challenge.substringAfter(' ', missingDelimiterValue = "")
                        .trim()
                        .takeIf { it.isNotBlank() }
                }
            }
    }
}

private sealed interface AuthRefreshTurn {
    data class Leader(val result: CompletableDeferred<Boolean>) : AuthRefreshTurn
    data class Waiter(val result: CompletableDeferred<Boolean>) : AuthRefreshTurn
}

private class AuthRefreshGate {
    private val mutex = Mutex()
    private var current: CompletableDeferred<Boolean>? = null

    suspend fun enter(): AuthRefreshTurn =
        mutex.withLock {
            current?.let { AuthRefreshTurn.Waiter(it) } ?: CompletableDeferred<Boolean>().also {
                current = it
            }.let { AuthRefreshTurn.Leader(it) }
        }

    private suspend fun clear(result: CompletableDeferred<Boolean>) {
        mutex.withLock {
            if (current === result) {
                current = null
            }
        }
    }

    suspend fun complete(result: CompletableDeferred<Boolean>, success: Boolean) {
        result.complete(success)
        clear(result)
    }
}

fun <U> SingleFlightCustomAuthConfig.configClientAuth(
    manager: SessionModel<U>,
    passHolder: PassHolder,
    addRequestHeader: HttpRequestBuilder.(U, String) -> Unit,
) {
    addRequestHeaders { request ->
        request.addRequestHeaders(manager, passHolder, addRequestHeader)
    }
    refreshSignature { data ->
        val session = passHolder.currentUserPass
        Napier.v("refreshSignature $data", tag = "ClientAuth")
        if (session == null) {
            false
        } else {
            manager.updateSignature(data, null)
            captureFailure(CredentialError::from) {
                manager.updateSignature(data, session.signature(finalData(data)).getOrThrow())
            }.fold({
                Napier.v(tag = "ClientAuth") {
                    "refreshSignature success"
                }
                true
            }, {
                Napier.e("refreshSignature failed", it.cause, tag = "ClientAuth")
                false
            })
        }
    }
}
