/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.core

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.callid.CallId
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.pingInterval
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import io.ktor.http.buildUrl
import io.ktor.http.protocolWithAuthority
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.util.AttributeKey
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class ServerErrorException(
    val status: HttpStatusCode,
    val text: String,
    cause: Exception,
    val isSessionAuthenticationFailure: Boolean = false,
) : Exception("$status, $text", cause)

expect fun getClient(block: HttpClientConfig<*>.() -> Unit): HttpClient

internal val CredentialedOriginAttribute = AttributeKey<String>("CredentialedOrigin")

@OptIn(ExperimentalUuidApi::class)
fun HttpClientConfig<*>.defaultClientConfigure(
    cookiesStorage: CookiesStorage,
    manager: UserSessionModel,
    httpUrl: String? = null,
    logLevel: LogLevel = LogLevel.HEADERS,
) {
    expectSuccess = true
    configureAccountIdentity(manager)

    install(HttpTimeout) {
        requestTimeoutMillis = 15.minutes.inWholeMilliseconds
    }
    defaultRequest {
        if (httpUrl != null) {
            url(httpUrl)
            attributes.put(CredentialedOriginAttribute, buildUrl { takeFrom(httpUrl) }.protocolWithAuthority)
        }
    }
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
            },
        )
    }
    install(CallId) {
        generate { Uuid.random().toString() }
        addToHeader(HttpHeaders.XRequestId)
    }
    install(Logging) {
        level = logLevel
        sanitizeHeader {
            it.equals(HttpHeaders.Cookie, true) || it.equals(HttpHeaders.SetCookie, true) ||
                it.equals(HttpHeaders.Authorization, true)
        }
    }
    install(HttpCookies) {
        storage = cookiesStorage
    }
    install(HttpRequestRetry) {
        retryIf { request, response ->
            request.method in
                setOf(
                    HttpMethod.Get,
                    HttpMethod.Head,
                ) && response.status == HttpStatusCode.TooManyRequests
        }
        delayMillis {
            1000
        }
    }
    install(WebSockets) {
        pingInterval = 2.seconds
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
    }
    configureResponseErrors()
}

@OptIn(ExperimentalUuidApi::class)
fun HttpClientConfig<*>.defaultClientConfigureForPanel(
    cookiesStorage: CookiesStorage,
    manager: PanelSessionModel,
    httpUrl: String? = null,
    logLevel: LogLevel = LogLevel.HEADERS,
) {
    expectSuccess = true
    configureAccountIdentity(manager)

    install(HttpTimeout) {
        requestTimeoutMillis = 15.minutes.inWholeMilliseconds
    }
    defaultRequest {
        if (httpUrl != null) {
            url(httpUrl)
            attributes.put(CredentialedOriginAttribute, buildUrl { takeFrom(httpUrl) }.protocolWithAuthority)
        }
    }
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
            },
        )
    }
    install(CallId) {
        generate { Uuid.random().toString() }
        addToHeader(HttpHeaders.XRequestId)
    }
    install(Logging) {
        level = logLevel
        sanitizeHeader {
            it.equals(HttpHeaders.Cookie, true) || it.equals(HttpHeaders.SetCookie, true) ||
                it.equals(HttpHeaders.Authorization, true)
        }
    }
    install(HttpCookies) {
        storage = cookiesStorage
    }
    install(HttpRequestRetry) {
        retryIf { request, response ->
            request.method in
                setOf(
                    HttpMethod.Get,
                    HttpMethod.Head,
                ) && response.status == HttpStatusCode.TooManyRequests
        }
        delayMillis {
            1000
        }
    }
    configureResponseErrors()
}

fun buildWebSocketUrl(wsServerUrl: String): String = URLBuilder(wsServerUrl).appendPathSegments("ws").buildString()

private fun HttpClientConfig<*>.configureAccountIdentity(manager: SessionModel<*>) {
    install(
        createClientPlugin("AccountIdentity") {
            onRequest { request, _ ->
                val origin = request.attributes.getOrNull(CredentialedOriginAttribute)
                if (origin == null || request.url.build().protocolWithAuthority == origin) {
                    request.header("X-Requested-With", "A-client")
                    manager.uid?.let { request.header("X-Account-Id", it) }
                }
            }
        },
    )
}

private fun HttpClientConfig<*>.configureResponseErrors() {
    HttpResponseValidator {
        handleResponseExceptionWithRequest { exception, request ->
            if (exception is ResponseException) {
                val exceptionResponse = exception.response
                val exceptionResponseText = exceptionResponse.bodyAsText()
                val origin = request.attributes.getOrNull(CredentialedOriginAttribute)
                throw ServerErrorException(
                    exceptionResponse.status,
                    exceptionResponseText,
                    exception,
                    origin != null && request.url.protocolWithAuthority == origin,
                )
            }
        }
    }
}
