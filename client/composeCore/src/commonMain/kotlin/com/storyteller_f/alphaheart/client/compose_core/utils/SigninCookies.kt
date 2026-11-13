/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.utils

import io.github.aakira.napier.Napier
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.http.Cookie
import io.ktor.http.Url
import io.ktor.util.date.GMTDate
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** The browser owns its cookie jar; native main-account cookies use protected platform storage. */
fun createSigninCookies(serverUrl: String, role: String): CookiesStorage = SigninCookies("$role:$serverUrl")

internal expect suspend fun readSigninCookies(key: String): String?
internal expect suspend fun writeSigninCookies(key: String, value: String)
expect val supportsChildAccountSwitching: Boolean

@Serializable
private data class SavedCookie(val url: String, val cookie: Cookie)

internal class SigninCookies(
    private val key: String,
    private val read: suspend (String) -> String? = { readSigninCookies(it) },
    private val write: suspend (String, String) -> Unit = { key, value -> writeSigninCookies(key, value) },
) : CookiesStorage {
    private val delegate = AcceptAllCookiesStorage()
    private val gate = Mutex()
    private val saved = mutableListOf<SavedCookie>()
    private var loaded = false

    override suspend fun get(requestUrl: Url): List<Cookie> =
        gate.withLock {
            loadCookies()
            delegate.get(requestUrl)
        }

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) =
        gate.withLock {
            loadCookies()
            delegate.addCookie(requestUrl, cookie)
            if (cookie.name == "user_session" || cookie.name == "panel_session") {
                saved.removeAll { it.cookie.name == cookie.name && Url(it.url).host == requestUrl.host }
                if (cookie.maxAge != 0 && cookie.value.isNotEmpty()) {
                    val expires =
                        cookie.maxAge?.let { GMTDate(GMTDate().timestamp + it.toLong() * 1000) }
                            ?: cookie.expires
                    saved.add(SavedCookie(requestUrl.toString(), cookie.copy(maxAge = null, expires = expires)))
                }
                persistCookies()
            }
        }

    override fun close() = delegate.close()

    private suspend fun loadCookies() {
        if (loaded) return
        val cookies =
            try {
                read(key)?.let { Json.decodeFromString<List<SavedCookie>>(it) }.orEmpty()
            } catch (cancellation: kotlin.coroutines.cancellation.CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                Napier.w("Saved login credential cannot be restored; explicit sign-in is required")
                emptyList()
            }
        cookies.filter { it.cookie.expires?.timestamp?.let { time -> time > GMTDate().timestamp } != false }
            .forEach {
                saved.add(it)
                delegate.addCookie(Url(it.url), it.cookie)
            }
        loaded = true
    }

    private suspend fun persistCookies() {
        try {
            write(key, Json.encodeToString<List<SavedCookie>>(saved))
        } catch (cancellation: kotlin.coroutines.cancellation.CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            Napier.w("Protected session storage unavailable; login will not survive restart")
        }
    }
}
