/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server

import com.storyteller_f.alphaheart.client.core.restoreSession
import com.storyteller_f.alphaheart.client.core.signOutAll
import com.storyteller_f.result.getOrThrow
import io.ktor.websocket.CloseReason
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class SigninLifecycleTest {
    @Test
    fun revocationClosesExistingWsAndRejectsHttp() =
        test {
            attachSession { account ->
                assertEquals(account.uid, restoreSession().getOrThrow().id)
                val socket =
                    withTimeout(10_000) {
                        webSocketClient.connectionHandler.data.filterNotNull().first()
                    }
                signOutAll().getOrThrow()
                val reason = withTimeout(10_000) { socket.closeReason.await() }
                assertEquals(CloseReason.Codes.VIOLATED_POLICY.code, reason?.code)
                assertTrue(restoreSession().isFailure)
            }
        }

    @Test
    fun panelSessionRestoresAndRevokes() =
        test {
            attachPanelSession { account ->
                assertEquals(account.uid, restoreSession().getOrThrow().id)
                signOutAll().getOrThrow()
                assertTrue(restoreSession().isFailure)
            }
        }
}
