/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server.auth

import com.storyteller_f.alphaheart.cloud.core.service.error.RoomError
import com.storyteller_f.alphaheart.cloud.error.ServerError
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame

internal class ServerErrorTest {
    @Test
    fun `permission category survives HTTP mapping`() {
        val cause = IllegalStateException("permission denied")
        val mapped = ServerError.fromError(RoomError.Forbidden(cause))
        assertSame(cause, assertIs<ServerError.Forbidden>(mapped).cause)
    }

    @Test
    fun `authentication category survives HTTP mapping`() {
        val cause = IllegalStateException("authentication required")
        assertSame(
            cause,
            assertIs<ServerError.Unauthenticated>(ServerError.fromError(RoomError.Unauthenticated(cause))).cause,
        )
    }

    @Test
    fun `unexpected errors retain server failure category`() {
        val cause = IllegalStateException("provider failed")
        val mapped = ServerError.fromError(RoomError.Unexpected(cause))
        assertSame(cause, assertIs<ServerError.Unexpected>(mapped).cause)
    }

    @Test
    fun `legacy state validation remains bad request`() {
        val cause = IllegalStateException("invalid state")
        assertSame(cause, assertIs<ServerError.InvalidInput>(ServerError.from(cause)).cause)
    }

    @Test
    fun `HTTP mapping propagates cancellation`() {
        val cancellation = CancellationException("cancelled")
        assertSame(cancellation, assertFailsWith<CancellationException> { ServerError.from(cancellation) })
    }
}
