/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.core

import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.rpc.krpc.ktor.server.Krpc
import kotlinx.rpc.krpc.ktor.server.rpc
import kotlinx.rpc.krpc.serialization.json.json
import kotlin.reflect.KClass

/** Restarts a real loopback RPC endpoint on the same port without replacing client adapters. */
class RestartableRpcServer<Service : Any>(
    private val serviceType: KClass<Service>,
    private val createService: () -> Service,
) {
    private var server: EmbeddedServer<*, *>? = null
    private var port = 0
    private val opened = MutableStateFlow(0)
    val connections = opened.asStateFlow()
    private val active = MutableStateFlow(0)
    val url: String get() = "ws://127.0.0.1:$port/rpc"

    suspend fun start() =
        withContext(Dispatchers.IO) {
            check(server == null)
            val endpoint =
                io.ktor.server.engine.embeddedServer(CIO, host = "127.0.0.1", port = port) {
                    install(WebSockets)
                    install(Krpc)
                    routing {
                        rpc("/rpc") {
                            opened.update { it + 1 }
                            active.update { it + 1 }
                            coroutineContext.job.invokeOnCompletion { active.update { it - 1 } }
                            rpcConfig { serialization { json() } }
                            registerService(serviceType, createService)
                        }
                    }
                }.start(wait = false)
            port = endpoint.engine.resolvedConnectors().single().port
            server = endpoint
        }

    suspend fun stop() {
        withContext(Dispatchers.IO) {
            server?.stop(0, 1_000)
            server = null
            // RPC shutdown completes on real dispatchers, so use a wall-clock deadline.
            withTimeout(5_000) { active.first { it == 0 } }
        }
    }
}
