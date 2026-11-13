/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server

import com.maxmind.geoip2.DatabaseReader
import com.perraco.utils.SnowflakeFactory
import com.storyteller_f.alphaheart.backend.core.Backend
import com.storyteller_f.alphaheart.backend.core.CombinedDatabase
import com.storyteller_f.alphaheart.backend.core.CustomBadRequestException
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
import com.storyteller_f.alphaheart.cloud.server.route.bindAccountRoute
import com.storyteller_f.alphaheart.cloud.server.route.bindCommunityRoute
import com.storyteller_f.alphaheart.cloud.server.route.bindProtectedAccountRoute
import com.storyteller_f.alphaheart.cloud.server.route.bindProtectedAdminRoute
import com.storyteller_f.alphaheart.cloud.server.route.bindProtectedCommunityRoute
import com.storyteller_f.alphaheart.cloud.server.route.bindProtectedFileRoute
import com.storyteller_f.alphaheart.cloud.server.route.bindProtectedRoomRoute
import com.storyteller_f.alphaheart.cloud.server.route.bindProtectedTitleRoute
import com.storyteller_f.alphaheart.cloud.server.route.bindProtectedTopicRoute
import com.storyteller_f.alphaheart.cloud.server.route.bindProtectedUserRoute
import com.storyteller_f.alphaheart.cloud.server.route.bindProtectedUserSubscriptionRoute
import com.storyteller_f.alphaheart.cloud.server.route.bindRoomRoute
import com.storyteller_f.alphaheart.cloud.server.route.bindTopicRoute
import com.storyteller_f.alphaheart.cloud.server.route.bindUnauthenticatedPanelRoute
import com.storyteller_f.alphaheart.cloud.server.route.bindUnauthenticatedRoute
import com.storyteller_f.alphaheart.cloud.server.route.bindUnprotectedAccountRoute
import com.storyteller_f.alphaheart.cloud.server.route.bindUserRoute
import com.storyteller_f.alphaheart.cloud.ws.api.GlobalWsEventPublisher
import com.storyteller_f.shared.loadCryptoLibIfNeed
import com.storyteller_f.shared.setupKmpLogger
import io.github.aakira.napier.Napier
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.callid.generate
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.partialcontent.PartialContent
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.request.httpMethod
import io.ktor.server.request.queryString
import io.ktor.server.request.uri
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import io.ktor.server.sessions.Sessions
import io.ktor.util.toMap
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.close
import io.sentry.Sentry
import kotlinx.coroutines.runBlocking
import org.slf4j.event.Level
import java.io.OutputStreamWriter
import kotlin.time.Duration.Companion.seconds

fun main(args: Array<String>) {
    setLogPath()
    setupKmpLogger()
    loadCryptoLibIfNeed()
    loadAvif()
    Napier.i {
        "encoding ${OutputStreamWriter(System.out).encoding}"
    }
    SnowflakeFactory.setMachine(0)

    val map = readEnv()
    val serverPort = map["SERVER_PORT"]?.toInt() ?: 80
    val extraArgs = arrayOf("-port=$serverPort")
    if (map["BUILD_TYPE"] == "prod") {
        Sentry.init { options ->
            options.dsn = map["SENTRY_DSN"]
            options.isDebug = false
        }
    }
    Runtime.getRuntime().addShutdownHook(
        Thread {
            Napier.i("程序即将退出，执行清理操作...")
        },
    )
    EngineMain.main(args + extraArgs)
}

fun Application.module() {
    monitor.subscribe(ApplicationStarted) { application ->
        application.environment.log.info("Server is started")
    }
    monitor.subscribe(ApplicationStopped) { application ->
        application.environment.log.info("Server is stopped")
        // Release resources and unsubscribe from events
        monitor.unsubscribe(ApplicationStarted) {}
        monitor.unsubscribe(ApplicationStopped) {}
    }
    val backend =
        try {
            buildBackend()
        } catch (e: Exception) {
            Napier.e(e, tag = "module") {
                "buildBackend failed"
            }
            throw e
        }
    val reader =
        try {
            buildDatabaseReader()
        } catch (e: Exception) {
            Napier.i(e, tag = "module") {
                "buildDatabaseReader failed"
            }
            throw e
        }
    runBlocking {
        if (backend.customConfig.buildType == "test") {
            backend.database.init()
        } else {
            backend.database.migration()
        }
    }
    GlobalWsEventPublisher.configure(readInjectedEnv()["WS_RPC_URL"])
    configurePlugin(reader, backend)
    configureAuth(backend)
    configureRoute(backend)
}

private fun Application.configurePlugin(reader: DatabaseReader, backend: Backend) {
    val env = readEnv(readInjectedEnv())
    install(ContentNegotiation) {
        json()
    }
    install(CallLogging) {
        level = Level.INFO
        callIdMdc("call-id")
        filter {
            it.request.uri != "/metrics"
        }
        format { call ->
            buildLog(call, reader)
        }
    }
    install(CallId) {
        header(HttpHeaders.XRequestId)
        generate()
    }
    install(Sessions) {
        setupUserSessions(env, this@configurePlugin)
    }
    if (backend.customConfig.buildType == "prod") {
        setupRateLimit()
    }
    install(PartialContent)
    install(RegionBlocker) {
        this.reader = reader
        denyIsoCodes = setOf("CN", "HK", "MO", "TW")
        excludePaths = setOf("/metrics")
    }
    configureMonitor()
}

private fun Application.setupRateLimit() {
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

private fun Application.buildBackend(): Backend {
    val injectedEnv = readInjectedEnv()
    val env = readEnv(injectedEnv)
    Napier.i {
        if (env["BUILD_TYPE"] == "test") {
            "start server in `${env["METHOD_NAME"]}`"
        } else {
            "start server on ${env["SERVER_PORT"]}"
        }
    }
    return buildBackendFromEnv(env)
}

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

private fun buildLog(call: ApplicationCall, reader: DatabaseReader): String {
    val status = call.response.status()
    val httpMethod = call.request.httpMethod.value
    val ipList = call.remoteIp(reader).joinToString(",")
    return formatRequestLog(
        "Url" to call.request.uri,
        "Status" to (status?.toString() ?: "<none>"),
        "HTTP method" to httpMethod,
        "Query" to call.request.queryString(),
        "Headers" to call.request.headers.toMap().toString(),
        "Ip" to ipList,
    )
}

class RegionBlockerConfig {
    lateinit var reader: DatabaseReader
    var denyIsoCodes: Set<String> = setOf("CN")
    var excludePaths: Set<String> = setOf("/metrics")
}

val RegionBlocker =
    createApplicationPlugin(name = "RegionBlocker", ::RegionBlockerConfig) {
        val reader = pluginConfig.reader
        val deny = pluginConfig.denyIsoCodes
        val exclude = pluginConfig.excludePaths
        onCall { call ->
            val uri = call.request.uri
            if (exclude.any { uri.startsWith(it) }) return@onCall
            val codes = call.remoteIp(reader).mapNotNull { it.second }.toSet()
            if (codes.any { it in deny }) {
                call.respond(HttpStatusCode(451, "Unavailable For Legal Reasons"))
            }
        }
    }

class ServerBackend(
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

    val topicSearchService = buildTopicSearchService(env)
    val userSearchService = buildUserSearchService(env)
    val roomSearchService = buildRoomSearchService(env)
    val communitySearchService = buildCommunitySearchService(env)
    val memberSearchService = buildMemberSearchService(env)
    val fileSearchService = buildFileSearchService(env)
    val mediaService = mediaService(env)

    val snapshotKeyStore = loadSnapshotKeyStore(env)
    val customConfig = CustomConfig(buildType, flavor, snapshotKeyStore, enableSignUp)

    return ServerBackend(
        customConfig,
        topicSearchService,
        roomSearchService,
        communitySearchService,
        userSearchService,
        memberSearchService,
        fileSearchService,
        mediaService,
        buildNameService(env),
        buildExposedDatabase(databaseConnection),
    )
}

@Suppress("ThrowsCount", "unused")
@OptIn(InternalAPI::class)
suspend fun ByteReadChannel.copyWithLimitAndClose(channel: ByteWriteChannel, limit: Long): Long {
    var result = 0L
    try {
        while (!isClosedForRead) {
            result += readBuffer.transferTo(channel.writeBuffer)
            if (result > limit) {
                throw CustomBadRequestException("exceed content length")
            }
            channel.flush()
            awaitContent()
        }

        closedCause?.let { throw it }
    } catch (cancellation: kotlin.coroutines.cancellation.CancellationException) {
        throw cancellation
    } catch (cause: Throwable) {
        cancel(cause)
        channel.close(cause)
        throw cause
    } finally {
        channel.flushAndClose()
    }

    return result
}

fun Application.configureRoute(backend: Backend) {
    routing {
        authenticate("user") {
            bindProtectedRoomRoute(backend)
            bindProtectedTopicRoute(backend)
            bindProtectedCommunityRoute(backend)
            bindProtectedUserRoute(backend)
            bindProtectedUserSubscriptionRoute(backend)
            bindProtectedFileRoute(backend)
            bindProtectedTitleRoute(backend)
            bindProtectedAccountRoute(backend)
        }
        authenticate("user", optional = true) {
            bindAccountRoute(backend)
            bindRoomRoute(backend)
            bindTopicRoute(backend)
            bindCommunityRoute(backend)
            bindUserRoute(backend)
        }
        bindUnprotectedAccountRoute(backend)
        bindUnauthenticatedRoute(backend)
    }
    routing {
        authenticate("admin") {
            bindProtectedAdminRoute(backend)
        }
        bindUnauthenticatedPanelRoute(backend)
    }
}
