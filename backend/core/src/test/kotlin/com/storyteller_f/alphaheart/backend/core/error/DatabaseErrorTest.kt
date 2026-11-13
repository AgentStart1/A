/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.core.error

import com.storyteller_f.alphaheart.backend.core.UnauthorizedException
import com.storyteller_f.result.AuthenticationFailure
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame

internal class DatabaseErrorTest {
    @Test
    fun `database authentication rejection remains typed`() {
        val cause = UnauthorizedException()
        val error = DatabaseError.from(cause)
        assertSame(cause, assertIs<DatabaseError.Unauthenticated>(error).cause)
        assertIs<AuthenticationFailure>(error)
    }

    @Test
    fun `authentication conversion preserves its category`() {
        val cause = IllegalStateException("authentication required")
        val source =
            object : AuthenticationFailure {
                override val cause = cause
            }
        assertSame(cause, assertIs<DatabaseError.Unauthenticated>(DatabaseError.fromError(source)).cause)
    }

    @Test
    fun `unexpected database failure remains unexpected`() {
        val cause = IllegalStateException("database unavailable")
        assertSame(cause, assertIs<DatabaseError.Unexpected>(DatabaseError.from(cause)).cause)
    }

    @Test
    fun `database conversion propagates cancellation`() {
        val cancellation = CancellationException("cancelled")
        assertSame(cancellation, assertFailsWith<CancellationException> { DatabaseError.from(cancellation) })
    }
}
