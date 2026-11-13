/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.core.error

import com.storyteller_f.result.FailureDetails
import com.storyteller_f.result.InvalidInputFailure
import kotlin.coroutines.cancellation.CancellationException

/** Failures belonging to the Search operation boundary. */
sealed interface SearchError : FailureDetails {
    data class InvalidInput(override val cause: Throwable) :
        SearchError,
        InvalidInputFailure
    data class Unexpected(override val cause: Throwable) : SearchError

    companion object {
        fun fromError(error: FailureDetails): SearchError = if (error is SearchError) error else from(error.cause)

        fun from(cause: Throwable): SearchError {
            if (cause is CancellationException) throw cause
            return when (cause) {
                is IllegalArgumentException -> InvalidInput(cause)
                else -> Unexpected(cause)
            }
        }
    }
}
