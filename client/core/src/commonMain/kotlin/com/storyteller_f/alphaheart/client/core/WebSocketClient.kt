/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.core

import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.mapError
import com.storyteller_f.shared.model.UserInfo
import com.storyteller_f.shared.obj.RoomFrame
import io.github.aakira.napier.Napier
import io.ktor.client.plugins.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.*
import kotlin.time.Duration.Companion.milliseconds

interface WebSocketClient {
    fun retryConnection()
    val connectionHandler: LoadingHandler<DefaultClientWebSocketSession>
    val localState: StateFlow<LoadingState?>
    val frameFlow: SharedFlow<RoomFrame>
    suspend fun <T> useWebSocket(block: suspend DefaultClientWebSocketSession.() -> T): CustomResult<T, RequestError>
}

@OptIn(DelicateCoroutinesApi::class)
class WebSocketClientImpl(
    val sessionModel: SessionModel<UserInfo>,
    val passHolder: PassHolder,
    val buildConnection: suspend (UserInfo, String) -> DefaultClientWebSocketSession,
    val onMessage: suspend (RoomFrame, DefaultClientWebSocketSession) -> Unit,
) : WebSocketClient {
    private val retryRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override fun retryConnection() {
        retryRequests.tryEmit(Unit)
    }

    override val connectionHandler = FixedLoadingHandler<DefaultClientWebSocketSession>()
    override val localState = MutableStateFlow<LoadingState?>(null)
    override val frameFlow = MutableSharedFlow<RoomFrame>()

    suspend fun connectWebSocket() {
        coroutineScope {
            val requests = Channel<UserInfo?>(Channel.CONFLATED)
            val connector =
                launch {
                    var connectionJob: Job? = null
                    try {
                        for (userInfo in requests) {
                            connectionJob?.cancelAndJoin()
                            connectionJob = null
                            if (userInfo == null) {
                                connectionHandler.data.value?.cancel(CancellationException("User sign out"))
                            } else {
                                connectionJob =
                                    launch {
                                        maintainWebSocket(userInfo)
                                    }
                            }
                        }
                    } finally {
                        connectionJob?.cancelAndJoin()
                        connectionHandler.data.value?.cancel()
                        connectionHandler.data.value = null
                    }
                }
            try {
                merge(
                    sessionModel.userHandler.data,
                    retryRequests.map { sessionModel.userHandler.data.value },
                ).collect { userInfo ->
                    if (userInfo != null) {
                        requests.trySend(userInfo)
                    } else {
                        requests.trySend(null)
                    }
                }
            } finally {
                requests.close()
                connector.cancelAndJoin()
            }
        }
    }

    private suspend fun maintainWebSocket(userInfo: UserInfo) {
        var failedHandshakes = 0
        var retryDelay = 5000L
        while (currentCoroutineContext().isActive && canConnectWebSocket(userInfo)) {
            connectWebSocketIfNeed(userInfo)
            val failure = (connectionHandler.state.value as? LoadingState.Error)?.e
            if (failure is SocketHandshakeException) {
                failedHandshakes++
            } else {
                failedHandshakes =
                    0
            }
            if (failedHandshakes >= 3) break
            delay(retryDelay.milliseconds)
            retryDelay =
                if (failure == null) {
                    5000
                } else {
                    (retryDelay * 2).coerceAtMost(
                        60_000,
                    )
                }
        }
    }

    override suspend fun <T> useWebSocket(
        block: suspend DefaultClientWebSocketSession.() -> T,
    ): CustomResult<T, RequestError> {
        val old = connectionHandler.data.value
        return (
            if (old != null && old.isActive) {
                localState.value = LoadingState.Loading
                try {
                    val r = old.block()
                    localState.value = LoadingState.Done
                    CustomResult.success(r)
                } catch (cancellation: kotlin.coroutines.cancellation.CancellationException) {
                    throw cancellation
                } catch (e: Exception) {
                    localState.value = LoadingState.Error(e)
                    CustomResult.Failure(RequestError.from(e))
                }
            } else {
                Napier.i {
                    "useWebSocket failed"
                }
                CustomResult.Failure(RequestError.from(Exception("WebSocket not connected")))
            }
            ).mapError(RequestError::fromError)
    }

    private suspend fun connectWebSocketIfNeed(userInfo: UserInfo) {
        val sig = sessionModel.dataAndSignature?.second.orEmpty()
        when (connectionHandler.state.value) {
            is LoadingState.Loading -> return

            is LoadingState.Done -> {
                val oldSession = connectionHandler.data.value
                if (oldSession?.isActive == true) return
            }

            else -> {}
        }
        connectionHandler.state.markLoading()
        Napier.i(tag = "ws") {
            "loading"
        }
        try {
            val session = buildConnection(userInfo, sig)
            startListenerWebSocket(session, userInfo)
            connectionHandler.done(session)
            connectionHandler.state.markDone()
            Napier.i(tag = "ws") {
                "done"
            }
        } catch (cancellation: kotlin.coroutines.cancellation.CancellationException) {
            throw cancellation
        } catch (e: SessionIdentityChangedException) {
            failConnection(e)
            expireConnection()
        } catch (e: ServerErrorException) {
            failConnection(e)
            if (e.status == io.ktor.http.HttpStatusCode.Unauthorized) expireConnection()
        } catch (e: Exception) {
            failConnection(e)
        }
    }

    private fun failConnection(failure: Exception) {
        connectionHandler.state.markError(failure)
        Napier.i(tag = "ws") { "error" }
    }

    private fun expireConnection() {
        sessionModel.sessionExpired.value = true
        sessionModel.userHandler.data.value = null
    }

    private fun canConnectWebSocket(userInfo: UserInfo): Boolean =
        passHolder.currentUserPass != null &&
            sessionModel.userHandler.data.value == userInfo

    private suspend fun startListenerWebSocket(session: DefaultClientWebSocketSession, userInfo: UserInfo) {
        CoroutineScope(currentCoroutineContext()).launch {
            try {
                while (isActive) {
                    val frame = session.receiveDeserialized<RoomFrame>()
                    onMessage(frame, session)
                    frameFlow.emit(frame)
                }
            } catch (cancellation: kotlin.coroutines.cancellation.CancellationException) {
                throw cancellation
            } catch (_: ClosedReceiveChannelException) {
                Napier.i(tag = "ClientWebSocket") { "Connection closed" }
            } catch (e: Exception) {
                Napier.e(e, tag = "ClientWebSocket") { "Receive failed" }
            } finally {
                session.cancel()
                if (connectionHandler.data.value === session) {
                    connectionHandler.data.value = null
                    connectionHandler.state.value = null
                }
                if (session.closeReason.await()?.code == io.ktor.websocket.CloseReason.Codes.VIOLATED_POLICY.code &&
                    sessionModel.userHandler.data.value == userInfo
                ) {
                    expireConnection()
                }
            }
        }
    }
}

suspend fun DefaultClientWebSocketSession.sendFrame(roomFrame: RoomFrame) {
    sendSerialized(roomFrame)
}
