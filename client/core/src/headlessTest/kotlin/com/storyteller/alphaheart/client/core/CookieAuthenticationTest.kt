/*
 * This is a private project. All rights reserved.
 */

package com.storyteller.alphaheart.client.core

import com.storyteller_f.alphaheart.client.core.ConstPassHolder
import com.storyteller_f.alphaheart.client.core.PanelSessionModel
import com.storyteller_f.alphaheart.client.core.SimplePanelSessionManager
import com.storyteller_f.alphaheart.client.core.defaultClientConfigureForPanel
import com.storyteller_f.alphaheart.client.core.serviceCatching
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.request.post
import io.ktor.http.Cookie
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class CookieAuthenticationTest {
    @Test
    fun unauthorizedOperationIsNotReplayedOrSigned() =
        runTest {
            var requests = 0
            val model = PanelSessionModel()
            val pass = ConstPassHolder(null)
            val cookies = AcceptAllCookiesStorage()
            cookies.addCookie(
                Url("https://api.example.test/admin"),
                Cookie(
                    "panel_session",
                    "opaque-credential",
                    domain = "api.example.test",
                    path = "/admin",
                    secure = true,
                ),
            )
            val engine =
                MockEngine { request ->
                    requests++
                    assertNull(request.headers[HttpHeaders.Authorization])
                    assertTrue(request.headers[HttpHeaders.Cookie].orEmpty().contains("panel_session="))
                    respond(
                        "Unauthorized",
                        HttpStatusCode.Unauthorized,
                        headersOf(HttpHeaders.WWWAuthenticate, "Custom challenge"),
                    )
                }
            HttpClient(engine) {
                defaultClientConfigureForPanel(cookies, model, "https://api.example.test")
            }.use { client ->
                val manager = SimplePanelSessionManager(client, model, cookies, pass)
                assertTrue(manager.serviceCatching { post("https://api.example.test/admin/action") }.isFailure)
                assertEquals(1, requests)
                assertTrue(model.sessionExpired.value)
            }
        }
}
