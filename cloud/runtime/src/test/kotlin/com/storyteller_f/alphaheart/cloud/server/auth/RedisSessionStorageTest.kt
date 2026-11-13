/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.server.auth

import com.storyteller_f.alphaheart.backend.core.MergedEnv
import com.storyteller_f.alphaheart.backend.core.service.buildSigninSessionService
import com.storyteller_f.result.getOrThrow
import io.github.crackthecodeabhi.kreds.connection.Endpoint
import io.github.crackthecodeabhi.kreds.connection.newClient
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.sessions.Sessions
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import java.security.MessageDigest
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class RedisSessionStorageTest {
    @Test
    fun signInRotatesCredential() =
        withRedis { endpoint ->
            testApplication {
                application { configureSessionRoutes(endpoint) }
                val browser = createClient { install(HttpCookies) }
                val pending =
                    browser.get("/challenge") {
                        headers.append("X-Requested-With", "A-client")
                    }.headers[HttpHeaders.SetCookie].orEmpty().substringBefore(';')
                val signed = browser.post("/login").headers[HttpHeaders.SetCookie].orEmpty()
                assertTrue(signed.contains("HttpOnly", ignoreCase = true))
                assertTrue(signed.substringBefore(';') != pending)
                assertEquals(HttpStatusCode.OK, browser.get("/protected").status)
            }
        }

    @Test
    fun rejectsUntrustedBrowserOrigin() =
        withRedis { endpoint ->
            testApplication {
                application { configureSessionRoutes(endpoint) }
                val browser = createClient { install(HttpCookies) }
                browser.get("/challenge") { headers.append("X-Requested-With", "A-client") }
                browser.post("/login")
                assertEquals(
                    HttpStatusCode.Forbidden,
                    browser.get("/protected") {
                        headers.append(HttpHeaders.Origin, "https://untrusted.example.test")
                    }.status,
                )
            }
        }

    @Test
    fun logoutRevokesCredential() =
        withRedis { endpoint ->
            testApplication {
                application { configureSessionRoutes(endpoint) }
                val browser = createClient { install(HttpCookies) }
                browser.get("/challenge") { headers.append("X-Requested-With", "A-client") }
                browser.post("/login")
                assertEquals(HttpStatusCode.OK, browser.post("/logout").status)
                assertEquals(HttpStatusCode.Unauthorized, browser.get("/protected").status)
            }
        }

    @Test
    fun consumesChallengeOnlyOnceAndRejectsWrongProof() =
        withRedis { endpoint ->
            registeredStorage(endpoint, "user").use { storage ->
                val pending = """{"type":"pending","data":"fresh","remote":"test","label":"user"}"""
                storage.write("pending", pending)
                assertEquals(false, storage.consumeChallenge("pending", "wrong", "pending"))
                assertEquals(false, storage.consumeChallenge("pending", "fresh", "success"))
                assertTrue(storage.consumeChallenge("pending", "fresh", "pending"))
                assertEquals(false, storage.consumeChallenge("pending", "fresh", "pending"))
                assertFailsWith<NoSuchElementException> { storage.read("pending") }
            }
        }

    @Test
    fun concurrentInstancesConsumeChallengeOnlyOnce() =
        withRedis { endpoint ->
            registeredStorage(endpoint, "user").use { first ->
                registeredStorage(endpoint, "user").use { second ->
                    first.write("pending", """{"type":"pending","data":"fresh","remote":"test","label":"user"}""")
                    val outcomes =
                        coroutineScope {
                            listOf(first, second).map { storage ->
                                async { storage.consumeChallenge("pending", "fresh", "pending") }
                            }.awaitAll()
                        }
                    assertEquals(1, outcomes.count { it })
                }
            }
        }

    @Test
    fun activityRenewsIdleExpiryWithinAbsoluteLimit() =
        withRedis { endpoint ->
            registeredStorage(endpoint, "user").use { storage ->
                storage.write("credential", SUCCESS)
                newClient(Endpoint.from(endpoint)).use { redis ->
                    val key = sessionKey("credential")
                    redis.eval(
                        "local r=cjson.decode(redis.call('GET',KEYS[1])); " +
                            "local now=tonumber(redis.call('TIME')[1]); " +
                            "r.active=now-600; r.absolute=now+120; " +
                            "redis.call('SET',KEYS[1],cjson.encode(r),'EX',30); return 1",
                        arrayOf(key),
                        emptyArray(),
                    )
                    assertTrue(storage.touchSession("credential"))
                    val remaining = redis.ttl(key)
                    assertTrue(remaining > 30 && remaining <= 120)
                    assertEquals(SUCCESS, storage.read("credential"))
                }
            }
        }

    @Test
    fun sharesSessionsAcrossApiAndWsAndRevokes() =
        withRedis { endpoint ->
            registeredStorage(endpoint, "user").use { api ->
                registeredStorage(endpoint, "user").use { ws ->
                    api.write("credential", SUCCESS)
                    assertEquals(SUCCESS, ws.read("credential"))
                    api.invalidate("credential")
                    assertFailsWith<NoSuchElementException> { ws.read("credential") }
                    ws.touchSession("credential")
                    assertFailsWith<NoSuchElementException> { api.read("credential") }
                }
            }
        }

    @Test
    fun isolatesUserAndPanelAndRevokesAllDevices() =
        withRedis { endpoint ->
            registeredStorage(endpoint, "user").use { user ->
                registeredStorage(endpoint, "panel").use { panel ->
                    user.write("first", SUCCESS)
                    user.write("second", SUCCESS)
                    panel.write("first", SUCCESS)
                    assertFailsWith<NoSuchElementException> { panel.read("second") }
                    user.invalidateAccount(7)
                    assertFailsWith<NoSuchElementException> { user.read("first") }
                    assertFailsWith<NoSuchElementException> { user.read("second") }
                    assertEquals(SUCCESS, panel.read("first"))
                }
            }
        }

    @Test
    fun readsDoNotRenewIdleExpiry() =
        withRedis { endpoint ->
            registeredStorage(endpoint, "user").use { storage ->
                storage.write("credential", SUCCESS)
                newClient(Endpoint.from(endpoint)).use { redis ->
                    val key = sessionKey("credential")
                    redis.expire(key, 120u)
                    storage.read("credential")
                    assertTrue(redis.ttl(key) <= 120)
                    assertNull(redis.get("signin:user:credential"))
                }
            }
        }

    @Test
    fun rejectsIdleAndAbsoluteExpiryWithoutResurrection() =
        withRedis { endpoint ->
            registeredStorage(endpoint, "user").use { storage ->
                newClient(Endpoint.from(endpoint)).use { redis ->
                    for (field in listOf("active", "absolute")) {
                        storage.write("credential", SUCCESS)
                        redis.eval(
                            "local r=cjson.decode(redis.call('GET',KEYS[1])); r[ARGV[1]]=0; " +
                                "redis.call('SET',KEYS[1],cjson.encode(r)); return 1",
                            arrayOf(sessionKey("credential")),
                            arrayOf(field),
                        )
                        storage.touchSession("credential")
                        assertFailsWith<NoSuchElementException> { storage.read("credential") }
                    }
                }
            }
        }

    @Test
    fun reconnectingServiceRetainsRedisSession() =
        withRedis { endpoint ->
            registeredStorage(endpoint, "user").use { it.write("credential", SUCCESS) }
            registeredStorage(endpoint, "user").use { assertEquals(SUCCESS, it.read("credential")) }
        }

    private fun Application.configureSessionRoutes(endpoint: String) {
        install(Sessions) {
            setupUserSessions(
                MergedEnv(
                    listOf(
                        mapOf(
                            "BUILD_TYPE" to "test",
                            "REDIS_ENDPOINT" to endpoint,
                            "SIGNIN_ALLOWED_ORIGINS" to "https://app.example.test",
                        ),
                    ),
                ),
                this@configureSessionRoutes,
            )
        }
        routing {
            get("/challenge") {
                call.beginSigninChallenge().getOrThrow()
                call.respondText("ready")
            }
            post("/login") {
                call.saveSuccessSession(call.getSession() as UserSession.Pending, 7)
                call.respondText("signed in")
            }
            get("/protected") {
                call.respondText(
                    "result",
                    status =
                    if (call.isSigninSessionValid() &&
                        call.getSession() is UserSession.Success
                    ) {
                        HttpStatusCode.OK
                    } else {
                        HttpStatusCode.Unauthorized
                    },
                )
            }
            post("/logout") {
                call.clearSigninSession()
                call.respondText("signed out")
            }
        }
    }

    private fun registeredStorage(endpoint: String, namespace: String) =
        buildSigninSessionService(MergedEnv(listOf(mapOf("SIGNIN_REDIS_ENDPOINT" to endpoint))), namespace)

    private fun withRedis(block: suspend (String) -> Unit) =
        runBlocking {
            GenericContainer(DockerImageName.parse("redis:7.4-alpine")).withExposedPorts(6379).use { redis ->
                redis.start()
                block("${redis.host}:${redis.firstMappedPort}")
            }
        }

    private fun sessionKey(id: String) =
        "signin:user:" +
            MessageDigest.getInstance("SHA-256")
                .digest(id.encodeToByteArray()).joinToString("") { "%02x".format(Locale.ROOT, it) }

    companion object {
        private const val SUCCESS = """{"type":"success","data":"challenge","remote":"test","id":7,"label":"user"}"""
    }
}
