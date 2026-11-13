/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.common

import com.storyteller_f.alphaheart.app.UIViewModel
import com.storyteller_f.alphaheart.app.buildHttpClient
import com.storyteller_f.alphaheart.client.core.ConstPassHolder
import com.storyteller_f.alphaheart.client.core.SimpleUserSessionManager
import com.storyteller_f.alphaheart.client.core.UserSessionManager
import com.storyteller_f.alphaheart.client.core.UserSessionModel
import com.storyteller_f.alphaheart.client.core.WebSocketClientImpl
import io.ktor.client.HttpClient
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.http.Url

/** A request owns its client, including when the originating account closes during transfer. */
internal class DownloadSession(val session: UserSessionManager, private val release: () -> Unit) {
    fun close() = release()
}

internal suspend fun UIViewModel.captureDownloadSession(): DownloadSession =
    captureDownloadSession(httpUrl, instance.value.sessionManager)

internal suspend fun captureDownloadSession(
    httpUrl: String,
    source: SimpleUserSessionManager,
    createClient: (String, CookiesStorage, UserSessionModel) -> HttpClient = ::buildHttpClient,
): DownloadSession {
    val user = source.model.userHandler.data.value
    val cookies = AcceptAllCookiesStorage()
    val origin = Url(httpUrl)
    source.cookieManager.get(origin)
        .forEach { cookies.addCookie(origin, it) }
    val model = UserSessionModel()
    user?.let(model::updateUser)
    val client = createClient(httpUrl, cookies, model)
    val session =
        object : UserSessionManager {
            override val client = client
            override val model = model
            override val passHolder = ConstPassHolder(null)
            override val webSocketClient: WebSocketClientImpl get() = error("Resource transfers do not use WebSockets")
        }
    return DownloadSession(session) { client.close() }
}
