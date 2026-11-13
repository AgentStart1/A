/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.ws

import com.maxmind.geoip2.DatabaseReader
import com.perraco.utils.SnowflakeFactory
import com.storyteller_f.alphaheart.backend.core.Backend
import com.storyteller_f.alphaheart.backend.core.CombinedDatabase
import com.storyteller_f.alphaheart.backend.core.CustomConfig
import com.storyteller_f.alphaheart.backend.core.MergedEnv
import com.storyteller_f.alphaheart.backend.core.buildCommunitySearchService
import com.storyteller_f.alphaheart.backend.core.buildFileSearchService
import com.storyteller_f.alphaheart.backend.core.buildMemberSearchService
import com.storyteller_f.alphaheart.backend.core.buildNameService
import com.storyteller_f.alphaheart.backend.core.buildRoomSearchService
import com.storyteller_f.alphaheart.backend.core.buildTopicSearchService
import com.storyteller_f.alphaheart.backend.core.buildUserSearchService
import com.storyteller_f.alphaheart.backend.core.databaseConnection
import com.storyteller_f.alphaheart.backend.core.loadAvif
import com.storyteller_f.alphaheart.backend.core.mediaService
import com.storyteller_f.alphaheart.backend.core.readEnv
import com.storyteller_f.alphaheart.backend.core.service.CommunitySearchService
import com.storyteller_f.alphaheart.backend.core.service.FileSearchService
import com.storyteller_f.alphaheart.backend.core.service.MemberSearchService
import com.storyteller_f.alphaheart.backend.core.service.NameService
import com.storyteller_f.alphaheart.backend.core.service.ObjectStorageService
import com.storyteller_f.alphaheart.backend.core.service.RoomSearchService
import com.storyteller_f.alphaheart.backend.core.service.TopicSearchService
import com.storyteller_f.alphaheart.backend.core.service.UserSearchService
import com.storyteller_f.alphaheart.backend.core.setLogPath
import com.storyteller_f.alphaheart.backend.exposed.buildExposedDatabase
import com.storyteller_f.alphaheart.cloud.server.auth.configureAuth
import com.storyteller_f.alphaheart.cloud.server.auth.getRateLimitKey
import com.storyteller_f.alphaheart.cloud.server.auth.setupUserSessions
import com.storyteller_f.alphaheart.cloud.ws.api.WsEventService
import com.storyteller_f.shared.loadCryptoLibIfNeed
import com.storyteller_f.shared.obj.RoomFrame
import com.storyteller_f.shared.setupKmpLogger
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.callid.generate
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.request.httpMethod
import io.ktor.server.request.uri
import io.ktor.server.routing.routing
import io.ktor.server.sessions.Sessions
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import io.ktor.server.websocket.webSocket
import kotlinx.rpc.krpc.ktor.server.Krpc
import kotlinx.rpc.krpc.ktor.server.rpc
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.serialization.json.Json
import org.slf4j.event.Level
import java.io.OutputStreamWriter
import kotlin.time.Duration.Companion.seconds
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation

fun main(args: Array<String>) {
    setLogPath()
    setupKmpLogger()
    loadCryptoLibIfNeed()
    loadAvif()
    Napier.i {
        "encoding ${OutputStreamWriter(System.out).encoding}"
    }
    SnowflakeFactory.setMachine(2)
    val map = readEnv()
    val serverPort = map["WS_SERVER_PORT"]?.toInt() ?: 8813
    EngineMain.main(args + arrayOf("-port=$serverPort"))
}

fun Application.module() {
    monitor.subscribe(ApplicationStarted) { application ->
        application.environment.log.info("WebSocket server is started")
    }
    monitor.subscribe(ApplicationStopped) { application ->
        application.environment.log.info("WebSocket server is stopped")
        monitor.unsubscribe(ApplicationStarted) {}
        monitor.unsubscribe(ApplicationStopped) {}
    }
    val backend = buildWsBackend()
    configureWsPlugin(backend)
    configureAuth(backend)
    configureWsRoute(buildDatabaseReader(), backend)
    startNewMessageTask(backend)
}

private fun Application.configureWsPlugin(backend: Backend) {
    val env = readEnv(readInjectedEnv())
    install(ContentNegotiation) {
        json()
    }
    install(CallLogging) {
        level = Level.INFO
        callIdMdc("call-id")
        format { call ->
            "${call.request.httpMethod.value} ${call.request.uri} ${call.response.status() ?: "<none>"}"
        }
    }
    install(CallId) {
        header(HttpHeaders.XRequestId)
        generate()
    }
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
    }
    install(Sessions) {
        setupUserSessions(env, this@configureWsPlugin)
    }
    install(Krpc)
    if (backend.customConfig.buildType == "prod") {
        install(RateLimit) {
            global {
                rateLimiter(limit = 50, refillPeriod = 10.seconds)
                requestKey { call ->
                    call.getRateLimitKey()
                }
                requestWeight { applicationCall, _ ->
                    when (applicationCall.request.httpMethod) {
                        HttpMethod.Post -> 2
                        else -> 1
                    }
                }
            }
        }
    }
}

private fun Application.configureWsRoute(reader: DatabaseReader, backend: Backend) {
    val httpClient =
        HttpClient {
            expectSuccess = true
            install(Logging)
            install(ClientContentNegotiation) {
                json()
            }
        }
    routing {
        authenticate("user") {
            webSocket("/ws") {
                webSocketContent(reader, backend)
            }
        }
        rpc("/rpc") {
            rpcConfig {
                serialization {
                    json()
                }
            }
            registerService<WsEventService> {
                WsEventServiceImpl(backend, httpClient)
            }
        }
    }
    monitor.subscribe(io.ktor.server.application.ApplicationStopping) {
        httpClient.close()
    }
}

private class WsEventServiceImpl(private val backend: Backend, private val httpClient: HttpClient) : WsEventService {
    override suspend fun publishNewTopic(frame: RoomFrame.NewTopicInfo): Boolean {
        dispatchNewMessageFrame(backend, httpClient, frame)
        return true
    }

    override suspend fun health(): String = "ok"
}

private fun Application.buildWsBackend(): Backend = buildBackendFromEnv(readEnv(readInjectedEnv()))

private fun Application.readInjectedEnv() =
    engine.environment.config.toMap().mapNotNull {
        (it.value as? String)?.let { v ->
            it.key to v
        }
    }.associate { it }

private fun buildDatabaseReader() =
    DatabaseReader.Builder(
        ClassLoader.getSystemResourceAsStream("GeoLite2-Country.mmdb"),
    ).build()

class WsBackend(
    override val customConfig: CustomConfig,
    override val topicSearchService: TopicSearchService,
    override val roomSearchService: RoomSearchService,
    override val communitySearchService: CommunitySearchService,
    override val userSearchService: UserSearchService,
    override val memberSearchService: MemberSearchService,
    override val fileSearchService: FileSearchService,
    override val objectStorageService: ObjectStorageService,
    override val nameService: NameService,
    override val database: CombinedDatabase,
) : Backend

fun buildBackendFromEnv(env: MergedEnv): Backend {
    Napier.i("load env: ${env.getAll("COMPOSE_PROJECT_NAME")}")
    val databaseConnection = databaseConnection(env)
    val buildType = env["BUILD_TYPE"] ?: "prod"
    val flavor = env["FLAVOR"] ?: error("FLAVOR is empty")
    val enableSignUp = env["ENABLE_SIGN_UP"]?.toBoolean() != false
    return WsBackend(
        CustomConfig(buildType, flavor, null, enableSignUp),
        buildTopicSearchService(env),
        buildRoomSearchService(env),
        buildCommunitySearchService(env),
        buildUserSearchService(env),
        buildMemberSearchService(env),
        buildFileSearchService(env),
        mediaService(env),
        buildNameService(env),
        buildExposedDatabase(databaseConnection),
    )
}
