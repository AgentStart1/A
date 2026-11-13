/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.core.service.error

import com.storyteller_f.result.AuthenticationFailure
import com.storyteller_f.result.ConstraintFailure
import com.storyteller_f.result.FailureDetails
import com.storyteller_f.result.InvalidInputFailure
import com.storyteller_f.result.PermissionFailure
import kotlin.coroutines.cancellation.CancellationException

/** Failures belonging to the Container operation boundary. */
sealed interface ContainerError : FailureDetails {
    data class InvalidInput(override val cause: Throwable) :
        ContainerError,
        InvalidInputFailure
    data class Unauthenticated(override val cause: Throwable) :
        ContainerError,
        AuthenticationFailure
    data class Forbidden(override val cause: Throwable) :
        ContainerError,
        PermissionFailure
    data class Unexpected(override val cause: Throwable) : ContainerError

    data class Conflict(override val cause: Throwable) :
        ContainerError,
        ConstraintFailure

    companion object {
        fun fromError(error: FailureDetails): ContainerError =
            when (error) {
                is ContainerError -> error
                is ConstraintFailure -> Conflict(error.cause)
                is InvalidInputFailure -> InvalidInput(error.cause)
                is AuthenticationFailure -> Unauthenticated(error.cause)
                is PermissionFailure -> Forbidden(error.cause)
                else -> Unexpected(error.cause)
            }

        fun from(cause: Throwable): ContainerError {
            if (cause is CancellationException) throw cause
            return when (cause) {
                is com.storyteller_f.alphaheart.backend.core.UnauthorizedException -> Unauthenticated(cause)
                is com.storyteller_f.alphaheart.backend.core.ForbiddenException -> Forbidden(cause)
                is com.storyteller_f.alphaheart.backend.core.CustomBadRequestException -> InvalidInput(cause)
                is IllegalArgumentException -> InvalidInput(cause)
                else -> Unexpected(cause)
            }
        }
    }
}
