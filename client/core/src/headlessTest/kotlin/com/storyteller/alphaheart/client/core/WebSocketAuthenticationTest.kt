/*
 * This is a private project. All rights reserved.
 */

package com.storyteller.alphaheart.client.core

import com.storyteller_f.alphaheart.client.core.AuthKey
import com.storyteller_f.alphaheart.client.core.ConstPassHolder
import com.storyteller_f.alphaheart.client.core.LoadingState
import com.storyteller_f.alphaheart.client.core.RawUserPass
import com.storyteller_f.alphaheart.client.core.RawUserPassInfo
import com.storyteller_f.alphaheart.client.core.ServerErrorException
import com.storyteller_f.alphaheart.client.core.SocketHandshakeException
import com.storyteller_f.alphaheart.client.core.UserSessionModel
import com.storyteller_f.alphaheart.client.core.WebSocketClientImpl
import com.storyteller_f.alphaheart.client.core.createSimpleUserSessionManager
import com.storyteller_f.alphaheart.client.core.defaultClientConfigure
import com.storyteller_f.shared.model.UserInfo
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
internal class WebSocketAuthenticationTest {
    @Test
    fun authFailureStopsReconnectWithoutSignature() =
        runTest {
            val model = UserSessionModel().apply { updateUser(UserInfo.EMPTY) }
            var attempts = 0
            val socket =
                WebSocketClientImpl(model, pass(), { _, _ ->
                    attempts++
                    throw ServerErrorException(HttpStatusCode.Unauthorized, "expired", Exception("expired"))
                }, { _, _ -> })
            backgroundScope.launch { socket.connectWebSocket() }
            runCurrent()
            advanceTimeBy(120_000)
            runCurrent()
            assertEquals(1, attempts)
            assertTrue(model.sessionExpired.value)
        }

    @Test
    fun unexplainedHandshakeFailureHasBoundedAttempts() =
        runTest {
            val model = UserSessionModel().apply { updateUser(UserInfo.EMPTY) }
            var attempts = 0
            val socket =
                WebSocketClientImpl(model, pass(), { _, _ ->
                    attempts++
                    throw SocketHandshakeException()
                }, { _, _ -> })
            backgroundScope.launch { socket.connectWebSocket() }
            runCurrent()
            advanceTimeBy(120_000)
            runCurrent()
            assertEquals(3, attempts)
            socket.retryConnection()
            runCurrent()
            advanceTimeBy(120_000)
            runCurrent()
            assertEquals(6, attempts)
            assertTrue(!model.sessionExpired.value)
        }

    @Test
    fun fetchErrorDuringSessionCheckKeepsConnectionOwnerAliveUntilRetry() =
        runTest {
            val fetchError = Error("Fail to fetch")
            var sessionChecks = 0
            val manager =
                createSimpleUserSessionManager(
                    webSocketUrl = "ws://example.test/ws",
                    passHolder = pass(),
                    createClient = { model, cookies ->
                        HttpClient(MockEngine) {
                            defaultClientConfigure(cookies, model, "http://example.test", LogLevel.NONE)
                            engine {
                                dispatcher = StandardTestDispatcher(testScheduler)
                                addHandler { request ->
                                    assertEquals("/accounts/session", request.url.encodedPath)
                                    sessionChecks++
                                    if (sessionChecks == 1) throw fetchError
                                    respond("expired", HttpStatusCode.Unauthorized)
                                }
                            }
                        }
                    },
                    onReceiveFrame = { _, _, _ -> },
                )
            manager.model.updateUser(UserInfo.EMPTY)
            val socket = manager.webSocketClient
            val connectionJob = backgroundScope.launch { socket.connectWebSocket() }

            try {
                runCurrent()
                assertEquals(1, sessionChecks)
                assertSame(fetchError, assertIs<LoadingState.Error>(socket.connectionHandler.state.value).e)
                assertTrue(connectionJob.isActive)
                assertFalse(manager.model.sessionExpired.value)
                assertSame(UserInfo.EMPTY, manager.model.userHandler.data.value)

                socket.retryConnection()
                runCurrent()

                assertEquals(2, sessionChecks)
                assertTrue(manager.model.sessionExpired.value)
                assertNull(manager.model.userHandler.data.value)
                assertTrue(connectionJob.isActive)
            } finally {
                connectionJob.cancelAndJoin()
                manager.client.close()
            }
        }

    private fun pass() =
        ConstPassHolder(
            RawUserPass(
                RawUserPassInfo(
                    "test",
                    AuthKey.P256("unused", "unused", "unused"),
                ),
            ),
        )
}
