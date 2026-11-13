/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server.auth

import com.storyteller_f.alphaheart.backend.core.Backend
import com.storyteller_f.alphaheart.backend.core.ForbiddenException
import com.storyteller_f.alphaheart.backend.core.UnauthorizedException
import com.storyteller_f.alphaheart.cloud.core.service.FileResponse
import com.storyteller_f.alphaheart.cloud.error.ServerError
import com.storyteller_f.endpoint4k.ktor.server.handleCaughtException
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.FailureDetails
import com.storyteller_f.result.mapError
import com.storyteller_f.result.onFailure
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.type.PrimaryKey
import io.ktor.http.ContentDisposition
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.log
import io.ktor.server.auth.principal
import io.ktor.server.request.httpMethod
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondFile
import io.ktor.server.routing.RoutingContext
import io.ktor.server.websocket.DefaultWebSocketServerSession

inline fun <reified R : Any, E> omitPrincipal(block: () -> CustomResult<R?, E>) = block()

inline fun <reified R : Any> RoutingContext.usePrincipal(block: (uid: PrimaryKey) -> CustomResult<R?, FailureDetails>) =
    usePrincipalOrNull { uid ->
        if (uid != null) {
            block(uid)
        } else {
            CustomResult.Failure(ServerError.from(UnauthorizedException()))
        }
    }

suspend inline fun <reified R : Any> RoutingContext.callRespond(
    backend: Backend,
    block: () -> CustomResult<R?, FailureDetails>?,
) {
    try {
        val result = block()
        if (result == null) {
            call.respond(HttpStatusCode.NotFound)
            return
        }
        handleResultInternal(result, backend.customConfig.buildType)
    } catch (cancellation: kotlin.coroutines.cancellation.CancellationException) {
        throw cancellation
    } catch (e: Exception) {
        handleCaughtException(e)
    }
}

inline fun <reified R : Any> RoutingContext.usePrincipalOrNull(
    block: (uid: PrimaryKey?) -> CustomResult<R?, FailureDetails>?,
): CustomResult<R?, ServerError>? {
    val principal = call.principal<CustomPrincipal>()
    val method = call.request.httpMethod
    return if (principal?.readOnly == true && method !in listOf(HttpMethod.Get, HttpMethod.Head, HttpMethod.Options)) {
        CustomResult.Failure(ServerError.from(ForbiddenException("User is read only")))
    } else {
        block(principal?.uid)?.mapError(ServerError::fromError)
    }
}

suspend fun RoutingContext.respondError(error: FailureDetails, buildType: String) {
    when (val failure = ServerError.fromError(error)) {
        is ServerError.Forbidden -> call.respond(HttpStatusCode.Forbidden, failure.cause.message.orEmpty())

        is ServerError.Unauthenticated -> call.respondUnauthorizedResponse()

        is ServerError.InvalidInput -> call.respond(HttpStatusCode.BadRequest, failure.cause.localizedMessage)

        is ServerError.Unexpected ->
            call.respond(
                HttpStatusCode.InternalServerError,
                if (buildType == "prod") "" else failure.cause.message ?: failure.cause.toString(),
            )
    }
}

inline fun <reified R : Any> DefaultWebSocketServerSession.usePrincipal(block: (PrimaryKey) -> R?) {
    val uid = call.principal<CustomPrincipal>()?.uid
    if (uid == null) {
        return
    } else {
        block(uid)
    }
}

inline fun <reified R> handleResult(
    backend: Backend,
): suspend RoutingContext.(it: CustomResult<R, FailureDetails>) -> Unit =
    { result ->
        handleResultInternal(result, backend.customConfig.buildType)
    }

suspend inline fun <reified R, E : FailureDetails> RoutingContext.handleResultInternal(
    result: CustomResult<R, E>,
    buildType: String,
) {
    result.onSuccess { value ->
        when (value) {
            null -> call.respond(HttpStatusCode.NotFound)

            is FileResponse -> {
                call.response.header(
                    HttpHeaders.ContentDisposition,
                    ContentDisposition.Attachment.withParameter(
                        ContentDisposition.Parameters.FileName,
                        value.file.name,
                    ).toString(),
                )
                call.respondFile(value.file)
            }

            is Unit -> call.respond(HttpStatusCode.OK)

            else -> call.respond(value)
        }
    }.onFailure { throwable ->
        respondError(throwable, buildType)
        call.application.log.error("Occur server exception", throwable.cause)
    }
}
