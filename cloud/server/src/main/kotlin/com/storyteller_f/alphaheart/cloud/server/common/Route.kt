/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server.common

import com.storyteller_f.alphaheart.cloud.error.ServerError
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.FailureDetails
import com.storyteller_f.result.captureFailure
import com.storyteller_f.result.mapError
import io.ktor.http.*
import io.ktor.server.routing.*
import io.ktor.server.util.*

inline fun <reified T : Any, reified R : Any> RoutingContext.checkParameter(
    name: String,
    block: (T) -> CustomResult<R?, FailureDetails>,
): CustomResult<R?, ServerError> = call.parameters.checkParameter<R, T>(name, block)

inline fun <reified R : Any, reified T : Any> Parameters.checkParameter(
    name: String,
    block: (T) -> CustomResult<R?, FailureDetails>,
): CustomResult<R?, ServerError> {
    val v =
        captureFailure(ServerError::from) {
            getOrFailCompact<T>(name)
        }
    return when (v) {
        is CustomResult.Success -> block(v.value).mapError(ServerError::fromError)
        is CustomResult.Failure -> v
    }
}

inline fun <reified T : Any> Parameters.getOrFailCompact(name: String): T {
    val value =
        if (T::class == ULong::class) {
            getOrFail<String>(name).toULong() as T
        } else {
            getOrFail<T>(name)
        }
    return value
}
