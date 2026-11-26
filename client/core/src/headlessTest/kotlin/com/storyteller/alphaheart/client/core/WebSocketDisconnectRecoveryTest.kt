/*
 * This is a private project. All rights reserved.
 */

package com.storyteller.alphaheart.client.core

import com.storyteller_f.alphaheart.client.core.AuthKey
import com.storyteller_f.alphaheart.client.core.ConstPassHolder
import com.storyteller_f.alphaheart.client.core.LoadingState
import com.storyteller_f.alphaheart.client.core.RawUserPass
import com.storyteller_f.alphaheart.client.core.RawUserPassInfo
import com.storyteller_f.alphaheart.client.core.UserSessionModel
import com.storyteller_f.alphaheart.client.core.WebSocketClientImpl
import com.storyteller_f.shared.model.UserInfo
import io.ktor.client.HttpClient
import io.ktor.client.call.HttpClientCall
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSocketException
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.url
import io.ktor.http.Headers
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.util.date.GMTDate
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import io.ktor.websocket.CloseReason
import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketExtension
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.coroutines.CoroutineContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class, InternalAPI::class)
internal class WebSocketDisconnectRecoveryTest {
    @Test
    fun unexpectedIncomingClosePublishesErrorAndExplicitRetryRestoresUsableSession() =
        runTest {
            val model = UserSessionModel().apply { updateUser(UserInfo.EMPTY) }
            val client = createClient()
            val sessions = mutableListOf<ControlledWebSocketSession>()
            var connectionAttempts = 0
            val socket =
                WebSocketClientImpl(model, pass(), { _, _ ->
                    connectionAttempts += 1
                    controlledSession(client).also(sessions::add).clientSession
                }, { _, _ -> })
            val connectionJob = backgroundScope.launch { socket.connectWebSocket() }

            try {
                runCurrent()
                assertEquals(1, connectionAttempts)
                assertIs<LoadingState.Done>(socket.connectionHandler.state.value)

                sessions.first().incomingFrames.close()
                runCurrent()
                val disconnectedState = socket.connectionHandler.state.value
                assertIs<LoadingState.Error>(
                    disconnectedState,
                    "An unexpected receive-channel close should remain visible as an error",
                )

                socket.retryConnection()
                runCurrent()

                assertEquals(2, connectionAttempts)
                assertIs<LoadingState.Done>(socket.connectionHandler.state.value)
                val result = socket.useWebSocket { send(Frame.Text("retry-probe")) }
                assertTrue(result.isSuccess, "The retried connection should accept a WebSocket operation")
                assertIs<Frame.Text>(sessions.last().outgoingFrames.tryReceive().getOrNull())
                assertFalse(model.sessionExpired.value)
            } finally {
                connectionJob.cancelAndJoin()
                sessions.forEach(ControlledWebSocketSession::close)
                client.close()
            }
        }

    @Test
    fun failedCloseReasonDoesNotStopAutomaticReconnect() =
        runTest {
            val model = UserSessionModel().apply { updateUser(UserInfo.EMPTY) }
            val client = createClient()
            val closeReason = CompletableDeferred<CloseReason?>()
            val sessions = mutableListOf<ControlledWebSocketSession>()
            val socket =
                WebSocketClientImpl(model, pass(), { _, _ ->
                    val session =
                        if (sessions.isEmpty()) controlledSession(client, closeReason) else controlledSession(client)
                    sessions.add(session)
                    session.clientSession
                }, { _, _ -> })
            val connectionJob = backgroundScope.launch { socket.connectWebSocket() }

            try {
                runCurrent()
                assertIs<LoadingState.Done>(socket.connectionHandler.state.value)
                val failure = WebSocketException("Transport failed")
                closeReason.completeExceptionally(failure)
                sessions.first().incomingFrames.close(failure)
                runCurrent()

                assertTrue(connectionJob.isActive)
                assertIs<LoadingState.Error>(socket.connectionHandler.state.value)
                advanceTimeBy(5_000)
                runCurrent()

                assertEquals(2, sessions.size)
                assertIs<LoadingState.Done>(socket.connectionHandler.state.value)
                assertFalse(model.sessionExpired.value)
            } finally {
                connectionJob.cancelAndJoin()
                sessions.forEach(ControlledWebSocketSession::close)
                client.close()
            }
        }

    @Test
    fun incomingChannelCancellationWhileCallerRemainsActivePublishesError() =
        runTest {
            val model = UserSessionModel().apply { updateUser(UserInfo.EMPTY) }
            val client = createClient()
            val sessions = mutableListOf<ControlledWebSocketSession>()
            val socket =
                WebSocketClientImpl(model, pass(), { _, _ ->
                    controlledSession(client).also(sessions::add).clientSession
                }, { _, _ -> })
            val connectionJob = backgroundScope.launch { socket.connectWebSocket() }

            try {
                runCurrent()
                assertIs<LoadingState.Done>(socket.connectionHandler.state.value)

                sessions.first().incomingFrames.cancel(CancellationException("transport closed"))
                runCurrent()

                assertTrue(connectionJob.isActive, "The owning connection coroutine should remain active")
                assertIs<LoadingState.Error>(
                    socket.connectionHandler.state.value,
                    "Cancelling the incoming transport channel should be reported as a connection error",
                )
                assertFalse(model.sessionExpired.value)
            } finally {
                connectionJob.cancelAndJoin()
                sessions.forEach(ControlledWebSocketSession::close)
                client.close()
            }
        }

    @Test
    fun transportCancellationDuringHandshakePublishesErrorAndExplicitRetryConnects() =
        runTest {
            val model = UserSessionModel().apply { updateUser(UserInfo.EMPTY) }
            val client = createClient()
            val sessions = mutableListOf<ControlledWebSocketSession>()
            var connectionAttempts = 0
            val socket =
                WebSocketClientImpl(model, pass(), { _, _ ->
                    connectionAttempts += 1
                    if (connectionAttempts == 1) {
                        throw CancellationException("handshake transport closed")
                    }
                    controlledSession(client).also(sessions::add).clientSession
                }, { _, _ -> })
            val connectionJob = backgroundScope.launch { socket.connectWebSocket() }

            try {
                runCurrent()

                assertEquals(1, connectionAttempts)
                assertTrue(connectionJob.isActive, "A handshake transport close must not cancel its caller")
                assertIs<LoadingState.Error>(
                    socket.connectionHandler.state.value,
                    "A transport cancellation during handshake should be visible as a connection error",
                )

                socket.retryConnection()
                runCurrent()

                assertEquals(2, connectionAttempts)
                assertIs<LoadingState.Done>(socket.connectionHandler.state.value)
                val result = socket.useWebSocket { send(Frame.Text("handshake-retry-probe")) }
                assertTrue(result.isSuccess, "The retried handshake should accept a WebSocket operation")
                assertIs<Frame.Text>(sessions.last().outgoingFrames.tryReceive().getOrNull())
                assertFalse(model.sessionExpired.value)
            } finally {
                connectionJob.cancelAndJoin()
                sessions.forEach(ControlledWebSocketSession::close)
                client.close()
            }
        }

    @Test
    fun callerCancellationAndLogoutDoNotBecomeConnectionErrors() =
        runTest {
            val model = UserSessionModel().apply { updateUser(UserInfo.EMPTY) }
            val client = createClient()
            val sessions = mutableListOf<ControlledWebSocketSession>()
            val socket =
                WebSocketClientImpl(model, pass(), { _, _ ->
                    controlledSession(client).also(sessions::add).clientSession
                }, { _, _ -> })
            val connectionJob = backgroundScope.launch { socket.connectWebSocket() }

            try {
                runCurrent()
                assertIs<LoadingState.Done>(socket.connectionHandler.state.value)

                connectionJob.cancelAndJoin()
                runCurrent()
                assertFalse(socket.connectionHandler.state.value is LoadingState.Error)
                assertFalse(model.sessionExpired.value)

                val logoutSocket =
                    WebSocketClientImpl(model, pass(), { _, _ ->
                        controlledSession(client).also(sessions::add).clientSession
                    }, { _, _ -> })
                model.updateUser(UserInfo.EMPTY)
                val logoutJob = backgroundScope.launch { logoutSocket.connectWebSocket() }
                runCurrent()
                model.clear()
                runCurrent()

                assertFalse(logoutSocket.connectionHandler.state.value is LoadingState.Error)
                assertFalse(model.sessionExpired.value)
                assertNull(logoutSocket.connectionHandler.data.value)
                logoutJob.cancelAndJoin()
            } finally {
                connectionJob.cancelAndJoin()
                sessions.forEach(ControlledWebSocketSession::close)
                client.close()
            }
        }

    @Test
    fun aClosedOlderSessionCannotClearTheNewerCurrentSession() =
        runTest {
            val model = UserSessionModel().apply { updateUser(UserInfo.EMPTY) }
            val client = createClient()
            val sessions = mutableListOf<ControlledWebSocketSession>()
            val socket =
                WebSocketClientImpl(model, pass(), { _, _ ->
                    controlledSession(client).also(sessions::add).clientSession
                }, { _, _ -> })
            val connectionJob = backgroundScope.launch { socket.connectWebSocket() }

            try {
                runCurrent()
                val older = sessions.first()
                val newer = controlledSession(client).also(sessions::add)
                socket.connectionHandler.done(newer.clientSession)
                socket.connectionHandler.state.value = LoadingState.Done

                older.incomingFrames.close()
                runCurrent()

                assertSame(newer.clientSession, socket.connectionHandler.data.value)
                assertIs<LoadingState.Done>(socket.connectionHandler.state.value)
            } finally {
                connectionJob.cancelAndJoin()
                sessions.forEach(ControlledWebSocketSession::close)
                client.close()
            }
        }

    private suspend fun controlledSession(
        client: HttpClient,
        closeReason: CompletableDeferred<CloseReason?> = CompletableDeferred(null),
    ): ControlledWebSocketSession {
        val parentContext = currentCoroutineContext()
        val sessionContext = parentContext + Job(parentContext[Job])
        val rawSession = ControlledDefaultWebSocketSession(sessionContext, closeReason)
        return ControlledWebSocketSession(
            incomingFrames = rawSession.incomingFrames,
            outgoingFrames = rawSession.outgoingFrames,
            clientSession = DefaultClientWebSocketSession(client.createCall(sessionContext), rawSession),
            rawSession = rawSession,
        )
    }

    private fun HttpClient.createCall(sessionContext: CoroutineContext): HttpClientCall {
        val request = HttpRequestBuilder().apply { url("ws://example.test/room") }.build()
        val response =
            HttpResponseData(
                statusCode = HttpStatusCode.SwitchingProtocols,
                requestTime = GMTDate(),
                headers = Headers.Empty,
                version = HttpProtocolVersion.HTTP_1_1,
                body = ByteReadChannel.Empty,
                callContext = sessionContext,
            )
        return HttpClientCall(this, request, response)
    }

    private fun createClient() =
        HttpClient(MockEngine) {
            install(WebSockets) {
                contentConverter = KotlinxWebsocketSerializationConverter(Json)
            }
            engine {
                addHandler { respond(content = "", status = HttpStatusCode.OK) }
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

private data class ControlledWebSocketSession(
    val incomingFrames: Channel<Frame>,
    val outgoingFrames: Channel<Frame>,
    val clientSession: DefaultClientWebSocketSession,
    val rawSession: ControlledDefaultWebSocketSession,
) {
    fun close() {
        incomingFrames.close()
        outgoingFrames.close()
        rawSession.coroutineContext.cancel()
    }
}

private class ControlledDefaultWebSocketSession(
    override val coroutineContext: CoroutineContext,
    override val closeReason: CompletableDeferred<CloseReason?>,
) : DefaultWebSocketSession {
    val incomingFrames = Channel<Frame>(Channel.UNLIMITED)
    val outgoingFrames = Channel<Frame>(Channel.UNLIMITED)

    override val incoming: ReceiveChannel<Frame> = incomingFrames
    override val outgoing: SendChannel<Frame> = outgoingFrames
    override val extensions: List<WebSocketExtension<*>> = emptyList()
    override var pingIntervalMillis: Long = 0
    override var timeoutMillis: Long = 0
    override var masking: Boolean = false
    override var maxFrameSize: Long = Long.MAX_VALUE

    @OptIn(InternalAPI::class)
    override fun start(negotiatedExtensions: List<WebSocketExtension<*>>) = Unit

    override suspend fun flush() = Unit

    @OptIn(InternalAPI::class)
    override fun terminate() {
        incomingFrames.close()
        outgoingFrames.close()
        coroutineContext.cancel()
    }
}
