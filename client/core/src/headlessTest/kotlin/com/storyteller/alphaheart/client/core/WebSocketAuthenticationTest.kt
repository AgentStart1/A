/*
 * This is a private project. All rights reserved.
 */

package com.storyteller.alphaheart.client.core

import com.storyteller_f.alphaheart.client.core.AuthKey
import com.storyteller_f.alphaheart.client.core.ConstPassHolder
import com.storyteller_f.alphaheart.client.core.RawUserPass
import com.storyteller_f.alphaheart.client.core.RawUserPassInfo
import com.storyteller_f.alphaheart.client.core.ServerErrorException
import com.storyteller_f.alphaheart.client.core.SocketHandshakeException
import com.storyteller_f.alphaheart.client.core.UserSessionModel
import com.storyteller_f.alphaheart.client.core.WebSocketClientImpl
import com.storyteller_f.shared.model.UserInfo
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
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
