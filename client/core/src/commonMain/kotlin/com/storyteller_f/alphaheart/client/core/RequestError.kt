/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.core

import com.storyteller_f.result.FailureDetails
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.serialization.SerializationException
import kotlin.coroutines.cancellation.CancellationException

/** Failures from client requests; session invalidation remains owned by serviceCatching. */
sealed interface RequestError : FailureDetails {
    data class SessionIdentityChanged(override val cause: Throwable) : RequestError
    data class ServerRejected(
        val status: Int,
        val responseText: String,
        val isSessionAuthenticationFailure: Boolean,
        override val cause: Throwable,
    ) : RequestError
    data class Timeout(override val cause: Throwable) : RequestError
    data class InvalidResponse(override val cause: Throwable) : RequestError
    data class TransportFailure(override val cause: Throwable) : RequestError

    companion object {
        fun fromError(error: FailureDetails): RequestError = if (error is RequestError) error else from(error.cause)

        fun from(cause: Throwable): RequestError =
            when (cause) {
                is CancellationException -> throw cause

                is SessionIdentityChangedException -> SessionIdentityChanged(cause)

                is ServerErrorException ->
                    ServerRejected(
                        cause.status.value,
                        cause.text,
                        cause.isSessionAuthenticationFailure,
                        cause,
                    )

                is HttpRequestTimeoutException -> Timeout(cause)

                is SerializationException -> InvalidResponse(cause)

                else -> TransportFailure(cause)
            }
    }
}
