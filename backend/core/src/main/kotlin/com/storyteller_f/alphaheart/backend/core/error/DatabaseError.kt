/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.core.error

import com.storyteller_f.alphaheart.backend.core.UnauthorizedException
import com.storyteller_f.result.AuthenticationFailure
import com.storyteller_f.result.ConstraintFailure
import com.storyteller_f.result.FailureDetails
import kotlin.coroutines.cancellation.CancellationException

/** Failures belonging to the Database operation boundary. */
sealed interface DatabaseError : FailureDetails {
    data class Unauthenticated(override val cause: Throwable) :
        DatabaseError,
        AuthenticationFailure
    data class ConstraintViolation(override val cause: Throwable) :
        DatabaseError,
        ConstraintFailure
    data class Unexpected(override val cause: Throwable) : DatabaseError

    companion object {
        fun fromError(error: FailureDetails): DatabaseError =
            when (error) {
                is DatabaseError -> error
                is AuthenticationFailure -> Unauthenticated(error.cause)
                else -> from(error.cause)
            }

        fun from(cause: Throwable): DatabaseError {
            if (cause is CancellationException) throw cause
            return when (cause) {
                is UnauthorizedException -> Unauthenticated(cause)
                else -> Unexpected(cause)
            }
        }
    }
}
