/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.filesystem

import com.storyteller_f.result.FailureDetails
import com.storyteller_f.result.InvalidInputFailure
import kotlin.coroutines.cancellation.CancellationException

/** Failures belonging to the Filesystem operation boundary. */
sealed interface FilesystemError : FailureDetails {
    data class InvalidInput(override val cause: Throwable) :
        FilesystemError,
        InvalidInputFailure
    data class Unexpected(override val cause: Throwable) : FilesystemError

    companion object {
        fun fromError(error: FailureDetails): FilesystemError =
            if (error is FilesystemError) error else from(error.cause)

        fun from(cause: Throwable): FilesystemError {
            if (cause is CancellationException) throw cause
            return when (cause) {
                is IllegalArgumentException -> InvalidInput(cause)
                else -> Unexpected(cause)
            }
        }
    }
}
