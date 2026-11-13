/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.http.Cookie
import io.ktor.http.Url
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InjectedSessionCookiesTest {
    @Test
    fun `injected cookie binds device host`() =
        runAppiumBlockingTest {
            val issued =
                Cookie("panel_session", "fixture-session", path = "/admin", domain = "127.0.0.1", httpOnly = true)
            val serialized =
                buildInjectedSessionJson(
                    InjectedSession("address", "pem", "private", "public", listOf(issued)),
                )
            val cookies =
                Json.decodeFromJsonElement(
                    ListSerializer(Cookie.serializer()),
                    Json.parseToJsonElement(serialized).jsonObject.getValue("cookies"),
                )
            val target = Url("http://localhost:8811/admin/session")
            val storage = AcceptAllCookiesStorage()
            cookies.forEach { storage.addCookie(target, it) }
            val restored = storage.get(target).single()
            assertEquals(issued.value, restored.value)
            assertEquals(issued.path, restored.path)
            assertTrue(restored.httpOnly)
            assertTrue(storage.get(Url("http://localhost:8811/accounts/session")).isEmpty())
            assertEquals("127.0.0.1", issued.domain)
        }
}
