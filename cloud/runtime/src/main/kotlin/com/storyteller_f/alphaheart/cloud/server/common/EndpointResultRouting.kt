/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server.common

import com.storyteller_f.alphaheart.cloud.error.ServerError
import com.storyteller_f.endpoint4k.common.MutationEndpoint
import com.storyteller_f.endpoint4k.common.MutationEndpointWithPath
import com.storyteller_f.endpoint4k.common.MutationEndpointWithQuery
import com.storyteller_f.endpoint4k.common.MutationEndpointWithQueryAndPath
import com.storyteller_f.endpoint4k.common.SafeEndpoint
import com.storyteller_f.endpoint4k.common.SafeEndpointWithPath
import com.storyteller_f.endpoint4k.common.SafeEndpointWithQuery
import com.storyteller_f.endpoint4k.common.SafeEndpointWithQueryAndPath
import com.storyteller_f.endpoint4k.ktor.server.ParametersDecoder
import com.storyteller_f.endpoint4k.ktor.server.customMutationBind
import com.storyteller_f.result.CustomResult
import com.storyteller_f.result.FailureDetails
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.modules.serializersModuleOf
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

@OptIn(InternalSerializationApi::class)
context(route: Route)
operator fun <R : Any, Q : Any, P : Any> SafeEndpointWithQueryAndPath<R, Q, P>.invoke(
    handleResult: suspend RoutingContext.(CustomResult<R?, FailureDetails>) -> Unit,
    block: suspend RoutingContext.(Q, P) -> CustomResult<R?, FailureDetails>?,
) {
    route.get(urlString) {
        val q = getQuery(queryClass)
        val p = getPathQuery(pathClass)
        handleRequest(handleResult) {
            block(q, p)
        }
    }
}

@OptIn(InternalSerializationApi::class)
context(route: Route)
operator fun <R : Any, Q : Any> SafeEndpointWithQuery<R, Q>.invoke(
    handleResult: suspend RoutingContext.(CustomResult<R?, FailureDetails>) -> Unit,
    block: suspend RoutingContext.(Q) -> CustomResult<R?, FailureDetails>?,
) {
    route.get(urlString) {
        val q = getQuery(queryClass)
        handleRequest(handleResult) {
            block(q)
        }
    }
}

@OptIn(InternalSerializationApi::class)
context(route: Route)
operator fun <R : Any, P : Any> SafeEndpointWithPath<R, P>.invoke(
    handleResult: suspend RoutingContext.(CustomResult<R?, FailureDetails>) -> Unit,
    block: suspend RoutingContext.(P) -> CustomResult<R?, FailureDetails>?,
) {
    route.get(urlString) {
        val p = getPathQuery(pathClass)
        handleRequest(handleResult) {
            block(p)
        }
    }
}

@OptIn(InternalSerializationApi::class)
context(route: Route)
operator fun <R : Any> SafeEndpoint<R>.invoke(
    handleResult: suspend RoutingContext.(CustomResult<R?, FailureDetails>) -> Unit,
    block: suspend RoutingContext.() -> CustomResult<R?, FailureDetails>?,
) {
    route.get(urlString) {
        handleRequest(handleResult, block)
    }
}

@OptIn(InternalSerializationApi::class)
context(route: Route)
operator fun <R : Any, B : Any, Q : Any, P : Any> MutationEndpointWithQueryAndPath<R, B, Q, P>.invoke(
    handleResult: suspend RoutingContext.(CustomResult<R?, FailureDetails>) -> Unit,
    block: suspend RoutingContext.(
        Q,
        P,
        MutationEndpointWithQueryAndPath<R, B, Q, P>,
    ) -> CustomResult<R?, FailureDetails>?,
) {
    route.customMutationBind {
        val q = getQuery(queryClass)
        val p = getPathQuery(pathClass)
        handleRequest(handleResult) {
            block(q, p, this@invoke)
        }
    }
}

@OptIn(InternalSerializationApi::class)
context(route: Route)
operator fun <R : Any, B : Any, Q : Any> MutationEndpointWithQuery<R, B, Q>.invoke(
    handleResult: suspend RoutingContext.(CustomResult<R?, FailureDetails>) -> Unit,
    block: suspend RoutingContext.(Q, MutationEndpointWithQuery<R, B, Q>) -> CustomResult<R?, FailureDetails>?,
) {
    route.customMutationBind {
        val q = getQuery(queryClass)
        handleRequest(handleResult) {
            block(q, this@invoke)
        }
    }
}

@OptIn(InternalSerializationApi::class)
context(route: Route)
operator fun <R : Any, B : Any, P : Any> MutationEndpointWithPath<R, B, P>.invoke(
    handleResult: suspend RoutingContext.(CustomResult<R?, FailureDetails>) -> Unit,
    block: suspend RoutingContext.(P, MutationEndpointWithPath<R, B, P>) -> CustomResult<R?, FailureDetails>?,
) {
    route.customMutationBind {
        val p = getPathQuery(pathClass)
        handleRequest(handleResult) {
            block(p, this@invoke)
        }
    }
}

@OptIn(InternalSerializationApi::class)
context(route: Route)
operator fun <R : Any, B : Any> MutationEndpoint<R, B>.invoke(
    handleResult: suspend RoutingContext.(CustomResult<R?, FailureDetails>) -> Unit,
    block: suspend RoutingContext.(MutationEndpoint<R, B>) -> CustomResult<R?, FailureDetails>?,
) {
    route.customMutationBind {
        handleRequest(handleResult) {
            block(this@invoke)
        }
    }
}

private suspend fun <R : Any> RoutingContext.handleRequest(
    handleResult: suspend RoutingContext.(CustomResult<R?, FailureDetails>) -> Unit,
    block: suspend RoutingContext.() -> CustomResult<R?, FailureDetails>?,
) {
    val result =
        try {
            block()
        } catch (cancellation: kotlin.coroutines.cancellation.CancellationException) {
            throw cancellation
        } catch (expected: Exception) {
            CustomResult.Failure(ServerError.from(expected))
        }
    if (result == null) {
        call.respond(HttpStatusCode.NotFound)
    } else {
        handleResult(result)
    }
}

@OptIn(InternalSerializationApi::class)
private fun <Q : Any> RoutingContext.getQuery(kClass: KClass<Q>): Q {
    val querySerializer = kClass.serializer()
    return querySerializer.deserialize(
        ParametersDecoder(
            serializersModuleOf(kClass, querySerializer),
            call.queryParameters,
            querySerializer.descriptor.elementNames,
        ),
    )
}

@OptIn(InternalSerializationApi::class)
private fun <P : Any> RoutingContext.getPathQuery(kClass: KClass<P>): P {
    val pathSerializer = kClass.serializer()
    return pathSerializer.deserialize(
        ParametersDecoder(
            serializersModuleOf(kClass, pathSerializer),
            call.pathParameters,
            pathSerializer.descriptor.elementNames,
        ),
    )
}
