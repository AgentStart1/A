/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.backend.core

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.http.takeFrom
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.rpc.RpcClient
import kotlinx.rpc.krpc.ktor.client.KtorRpcClient
import kotlinx.rpc.krpc.ktor.client.installKrpc
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.serialization.json.json

/** Acquires one proxy to retain throughout a business operation. Never replays calls. */
fun interface RpcServiceProvider<Service : Any> {
    suspend fun acquire(): Service
}

/** Serializes establishment; only subsequent operations replace a disconnected transport. */
class ManagedRpcService<Service : Any>(
    private val rpcUrl: String,
    private val createService: (RpcClient) -> Service,
    private val checkHealth: suspend (Service) -> Unit,
) : RpcServiceProvider<Service> {
    private val mutex = Mutex()
    private var connection: ServiceConnection<Service>? = null

    override suspend fun acquire(): Service =
        mutex.withLock {
            currentCoroutineContext().ensureActive()
            val previous = connection
            if (previous?.isActive == true) {
                previous.service
            } else {
                previous?.close()
                connection = null
                openConnection().also { connection = it }.service
            }
        }

    suspend fun close() =
        withContext(NonCancellable) {
            mutex.withLock {
                connection?.close()
                connection = null
            }
        }

    private suspend fun openConnection(): ServiceConnection<Service> {
        val client =
            HttpClient(CIO) {
                install(WebSockets)
                installKrpc()
            }
        val rpcClient =
            client.rpc {
                url { takeFrom(rpcUrl) }
                rpcConfig { serialization { json() } }
            }
        val candidate = ServiceConnection(client, rpcClient, createService(rpcClient))
        var ready = false
        return try {
            checkHealth(candidate.service)
            currentCoroutineContext().ensureActive()
            val session = rpcClient.webSocketSession.await()
            session.coroutineContext.job.invokeOnCompletion { candidate.close() }
            ready = true
            candidate
        } finally {
            if (!ready) candidate.close()
        }
    }
}

private class ServiceConnection<Service : Any>(
    private val client: HttpClient,
    private val rpcClient: KtorRpcClient,
    val service: Service,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    val isActive: Boolean
        get() {
            val session = rpcClient.webSocketSession
            return client.coroutineContext.isActive && session.isCompleted && !session.isCancelled &&
                session.getCompleted().coroutineContext.isActive
        }

    fun close() {
        rpcClient.close()
        client.close()
        client.coroutineContext.cancel()
    }
}
