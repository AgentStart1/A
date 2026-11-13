/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.error

import com.storyteller_f.result.FailureDetails
import com.storyteller_f.result.InvalidInputFailure
import kotlin.coroutines.cancellation.CancellationException

/** Failures belonging to the FileAction operation boundary. */
sealed interface FileActionError : FailureDetails {
    data class InvalidInput(override val cause: Throwable) :
        FileActionError,
        InvalidInputFailure
    data class Unexpected(override val cause: Throwable) : FileActionError

    companion object {
        fun fromError(error: FailureDetails): FileActionError =
            if (error is FileActionError) error else from(error.cause)

        fun from(cause: Throwable): FileActionError {
            if (cause is CancellationException) throw cause
            return when (cause) {
                is IllegalArgumentException -> InvalidInput(cause)
                else -> Unexpected(cause)
            }
        }
    }
}
