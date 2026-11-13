/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.error

import com.storyteller_f.result.AuthenticationFailure
import com.storyteller_f.result.FailureDetails
import com.storyteller_f.result.InvalidInputFailure
import com.storyteller_f.result.PermissionFailure
import kotlin.coroutines.cancellation.CancellationException

/** Failures belonging to the Server operation boundary. */
sealed interface ServerError : FailureDetails {
    data class InvalidInput(override val cause: Throwable) :
        ServerError,
        InvalidInputFailure
    data class Unauthenticated(override val cause: Throwable) :
        ServerError,
        AuthenticationFailure
    data class Forbidden(override val cause: Throwable) :
        ServerError,
        PermissionFailure
    data class Unexpected(override val cause: Throwable) : ServerError

    companion object {
        fun fromError(error: FailureDetails): ServerError =
            when (error) {
                is ServerError -> error
                is InvalidInputFailure -> InvalidInput(error.cause)
                is AuthenticationFailure -> Unauthenticated(error.cause)
                is PermissionFailure -> Forbidden(error.cause)
                else -> Unexpected(error.cause)
            }

        fun from(cause: Throwable): ServerError {
            if (cause is CancellationException) throw cause
            return when (cause) {
                is com.storyteller_f.alphaheart.backend.core.UnauthorizedException -> Unauthenticated(cause)

                is com.storyteller_f.alphaheart.backend.core.ForbiddenException -> Forbidden(cause)

                is com.storyteller_f.alphaheart.backend.core.CustomBadRequestException -> InvalidInput(cause)

                is IllegalArgumentException, is IllegalStateException,
                is io.ktor.server.plugins.BadRequestException,
                is io.ktor.server.plugins.ContentTransformationException,
                -> InvalidInput(cause)

                else -> Unexpected(cause)
            }
        }
    }
}
