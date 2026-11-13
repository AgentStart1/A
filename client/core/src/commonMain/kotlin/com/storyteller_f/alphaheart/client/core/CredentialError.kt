/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.core

import com.storyteller_f.result.FailureDetails
import com.storyteller_f.result.InvalidInputFailure
import kotlin.coroutines.cancellation.CancellationException

/** Failures belonging to the Credential operation boundary. */
sealed interface CredentialError : FailureDetails {
    data class InvalidInput(override val cause: Throwable) :
        CredentialError,
        InvalidInputFailure
    data class Unexpected(override val cause: Throwable) : CredentialError

    companion object {
        fun fromError(error: FailureDetails): CredentialError =
            if (error is CredentialError) error else from(error.cause)

        fun from(cause: Throwable): CredentialError {
            if (cause is CancellationException) throw cause
            return when (cause) {
                is IllegalArgumentException -> InvalidInput(cause)
                else -> Unexpected(cause)
            }
        }
    }
}
