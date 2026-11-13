/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.core

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json

actual fun getClient(block: HttpClientConfig<*>.() -> Unit): HttpClient =
    HttpClient(Darwin) {
        install(ContentNegotiation) {
            json()
        }
        block()
    }
