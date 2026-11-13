/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.ws.api

import com.storyteller_f.alphaheart.cloud.error.WebSocketError
import com.storyteller_f.result.captureFailure
import com.storyteller_f.result.onFailure
import com.storyteller_f.shared.obj.RoomFrame
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.http.takeFrom
import kotlinx.rpc.annotations.Rpc
import kotlinx.rpc.krpc.ktor.client.installKrpc
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.rpc.withService

@Rpc
interface WsEventService {
    suspend fun publishNewTopic(frame: RoomFrame.NewTopicInfo): Boolean
    suspend fun health(): String
}

class WsEventPublisher(private val rpcUrl: String?) {
    suspend fun publishNewTopic(frame: RoomFrame.NewTopicInfo) {
        val url = rpcUrl
        if (url.isNullOrBlank()) {
            return
        }
        captureFailure(WebSocketError::from) {
            HttpClient(OkHttp) {
                install(WebSockets)
                installKrpc()
            }.use { client ->
                val rpcClient =
                    client.rpc {
                        url {
                            takeFrom(url)
                        }
                        rpcConfig {
                            serialization {
                                json()
                            }
                        }
                    }
                rpcClient.withService<WsEventService>().publishNewTopic(frame)
            }
        }.onFailure {
            Napier.e(it.cause) {
                "publish websocket event failed"
            }
        }
    }
}

object GlobalWsEventPublisher {
    @Volatile
    private var publisher = WsEventPublisher(null)

    fun configure(rpcUrl: String?) {
        publisher = WsEventPublisher(rpcUrl)
    }

    suspend fun publishNewTopic(frame: RoomFrame.NewTopicInfo) {
        publisher.publishNewTopic(frame)
    }
}
