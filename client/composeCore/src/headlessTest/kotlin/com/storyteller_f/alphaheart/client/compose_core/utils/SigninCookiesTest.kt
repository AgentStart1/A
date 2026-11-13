/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.utils

import io.ktor.http.Cookie
import io.ktor.http.Url
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class SigninCookiesTest {
    @Test
    fun corruptSavedCredentialDoesNotPreventNewLogin() =
        runTest {
            var persisted = "invalid"
            val storage = SigninCookies("isolated", { persisted }, { _, value -> persisted = value })
            val origin = Url("https://api.example.test/")
            assertTrue(storage.get(origin).isEmpty())
            storage.addCookie(origin, Cookie("user_session", "new-credential", maxAge = 3600))
            assertEquals("new-credential", storage.get(origin).single().value)
        }

    @Test
    fun restoresCookieForHttpAndWsAndClearsOnLogout() =
        runTest {
            var persisted: String? = null
            val origin = Url("https://api.example.test/")
            val first = SigninCookies("isolated", { persisted }, { _, value -> persisted = value })
            first.addCookie(
                origin,
                Cookie("user_session", "opaque", path = "/", secure = true, httpOnly = true, maxAge = 3600),
            )
            first.close()
            val restored = SigninCookies("isolated", { persisted }, { _, value -> persisted = value })
            assertEquals("opaque", restored.get(origin).single().value)
            assertEquals("opaque", restored.get(Url("wss://api.example.test/ws")).single().value)
            assertTrue(restored.get(Url("https://other.example.test/")).isEmpty())
            restored.addCookie(origin, Cookie("user_session", "", path = "/", maxAge = 0))
            val afterLogout = SigninCookies("isolated", { persisted }, { _, value -> persisted = value })
            assertTrue(afterLogout.get(origin).isEmpty())
        }
}
