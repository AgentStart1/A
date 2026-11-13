/*
 * This is a private project. All rights reserved.
 */

@file:OptIn(ExperimentalWasmJsInterop::class)

package com.storyteller_f.alphaheart.client.core

import io.ktor.client.*
import io.ktor.client.engine.js.Js
import io.ktor.client.fetchOptions
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.protocolWithAuthority
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.toJsString

private val credentialedApiRequests =
    createClientPlugin("CredentialedApiRequests") {
        onRequest { request, _ ->
            val credentialedOrigin = request.attributes.getOrNull(CredentialedOriginAttribute)
            if (credentialedOrigin != null && request.url.build().protocolWithAuthority == credentialedOrigin) {
                request.fetchOptions {
                    credentials = "include".toJsString()
                }
            }
        }
    }

private fun HttpClientConfig<*>.configureClient(block: HttpClientConfig<*>.() -> Unit) {
    install(credentialedApiRequests)
    block()
}

actual fun getClient(block: HttpClientConfig<*>.() -> Unit): HttpClient = HttpClient(Js) { configureClient(block) }
