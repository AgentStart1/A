/*
 * This is a private project. All rights reserved.
 */
package com.storytellerf.alphaheart.cloud.worker.llm

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.channels.Channel
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.net.InetSocketAddress

internal data class CapturedLlmRequest(
    val method: String,
    val path: String,
    val query: String?,
    val authorization: String?,
    val body: JsonObject,
)

internal class LlmHttpTestServer(responseBody: String) : AutoCloseable {
    val requests = Channel<CapturedLlmRequest>(Channel.UNLIMITED)
    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
    val baseUrl: String get() = "http://127.0.0.1:${server.address.port}"

    init {
        server.createContext("/") { exchange ->
            exchange.use {
                val body = exchange.requestBody.bufferedReader().use { reader -> reader.readText() }
                requests.trySend(
                    CapturedLlmRequest(
                        method = exchange.requestMethod,
                        path = exchange.requestURI.path,
                        query = exchange.requestURI.rawQuery,
                        authorization = exchange.requestHeaders.getFirst("Authorization"),
                        body = Json.parseToJsonElement(body).jsonObject,
                    ),
                ).getOrThrow()
                val response = responseBody.toByteArray()
                exchange.responseHeaders.set("Content-Type", "application/json")
                exchange.sendResponseHeaders(200, response.size.toLong())
                exchange.responseBody.buffered().use { output -> output.write(response) }
            }
        }
        server.start()
    }

    override fun close() {
        server.stop(0)
        requests.close()
    }
}
