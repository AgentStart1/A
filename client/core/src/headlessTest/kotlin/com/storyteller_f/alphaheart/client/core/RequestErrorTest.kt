/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.core

import io.ktor.http.HttpStatusCode
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

internal class RequestErrorTest {
    @Test
    fun `business 401 is not a session failure`() {
        val cause = ServerErrorException(HttpStatusCode.Unauthorized, "denied", IllegalStateException("server"))
        val error = assertIs<RequestError.ServerRejected>(RequestError.from(cause))
        assertEquals(401, error.status)
        assertFalse(error.isSessionAuthenticationFailure)
        assertSame(cause, error.cause)
    }

    @Test
    fun `session 401 retains authentication flag`() {
        val cause =
            ServerErrorException(
                HttpStatusCode.Unauthorized,
                "expired",
                IllegalStateException("server"),
                isSessionAuthenticationFailure = true,
            )
        val error = assertIs<RequestError.ServerRejected>(RequestError.from(cause))
        assertEquals(401, error.status)
        assertTrue(error.isSessionAuthenticationFailure)
    }

    @Test
    fun `identity change has its own category`() {
        val cause = SessionIdentityChangedException()
        assertSame(cause, assertIs<RequestError.SessionIdentityChanged>(RequestError.from(cause)).cause)
    }

    @Test
    fun `request mapping propagates cancellation`() {
        val cancellation = CancellationException("cancelled")
        assertSame(cancellation, assertFailsWith<CancellationException> { RequestError.from(cancellation) })
    }
}
