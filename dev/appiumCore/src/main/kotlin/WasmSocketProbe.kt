/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import com.storyteller_f.alphaheart.api.NewTopic
import com.storyteller_f.shared.commonJson
import com.storyteller_f.shared.model.TopicContent
import com.storyteller_f.shared.obj.NewRoomTopic
import com.storyteller_f.shared.obj.RoomFrame
import com.storyteller_f.shared.type.ObjectType
import kotlinx.coroutines.delay
import org.openqa.selenium.Cookie
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebDriver
import org.openqa.selenium.support.ui.WebDriverWait
import java.io.File
import java.time.Duration
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** Observes native browser transport without recording credentials or payloads. */
internal class WasmSocketProbe(private val browser: WebDriver) {
    private val script get() = browser as JavascriptExecutor
    private val signinCookie = browser.manage().getCookieNamed("user_session")

    fun count(name: String): Long =
        (checkNotNull(script.executeScript("return window.appiumSocketProbe[arguments[0]];", name)) as Number).toLong()

    fun awaitOpen(after: Long = 0) {
        WebDriverWait(browser, Duration.ofSeconds(30)).until { count("opened") > after }
    }

    fun disconnect() {
        script.executeScript("window.appiumSocketProbe.sockets.forEach(socket => socket.close());")
    }

    fun dropCookies() = browser.manage().deleteAllCookies()

    fun expireCookies() {
        browser.manage().cookies.forEach { cookie ->
            browser.manage().addCookie(
                Cookie.Builder(cookie.name, cookie.value).path(cookie.path)
                    .isHttpOnly(cookie.isHttpOnly).expiresOn(Date(0)).build(),
            )
        }
    }

    fun assertForeignOriginRejected() {
        val socketUrl = script.executeScript("return window.appiumSocketProbe.sockets[0].url;")
        WasmDistributionServer(File("."), measureCoverage = false).use { foreign ->
            browser.get(foreign.bootstrapUrl)
            val opened =
                script.executeAsyncScript(
                    """
                    const done = arguments[arguments.length - 1];
                    const socket = new WebSocket(arguments[0]);
                    socket.onopen = () => { socket.close(); done(true); };
                    socket.onerror = () => done(false);
                    """.trimIndent(),
                    socketUrl,
                )
            assertEquals(false, opened, "An unapproved browser Origin must be rejected with a valid Cookie")
        }
    }

    fun assertRevokedCookieRejected() {
        browser.manage().addCookie(checkNotNull(signinCookie))
        val opened =
            script.executeAsyncScript(
                """
                const done = arguments[arguments.length - 1];
                const socket = new WebSocket(window.appiumSocketProbe.sockets[0].url);
                socket.onopen = () => { socket.close(); done(true); };
                socket.onerror = () => done(false);
                """.trimIndent(),
            )
        assertEquals(false, opened, "A revoked Cookie must not authenticate a new browser WebSocket")
    }

    /** Uses this actual client's Cookie/WS to verify both mutation boundaries reject a valid payload. */
    fun assertRoomWriteRejected(roomId: Long, text: String, expectedError: String = "Invalid operation") {
        val body = commonJson.encodeToString(NewTopic.serializer(), NewTopic(ObjectType.ROOM, roomId, text))
        val status =
            script.executeAsyncScript(
                """
                const done = arguments[arguments.length - 1];
                fetch('topics', {method: 'POST', credentials: 'same-origin',
                  headers: {'Content-Type': 'application/json'}, body: arguments[0]})
                  .then(response => done(response.status));
                """.trimIndent(),
                body,
            )
        assertEquals(403, assertIs<Number>(status).toInt(), "Authenticated HTTP writes must be forbidden")
        val frame = RoomFrame.Message(NewRoomTopic(ObjectType.ROOM, roomId, TopicContent.Plain(text)))
        val response =
            script.executeAsyncScript(
                """
                const done = arguments[arguments.length - 1];
                const socket = window.appiumSocketProbe.sockets.find(socket => socket.readyState === WebSocket.OPEN);
                if (!socket) throw new Error('No authenticated client WebSocket');
                const receive = event => {
                  const frame = JSON.parse(event.data);
                  if (frame.type === 'error') { socket.removeEventListener('message', receive); done(event.data); }
                };
                socket.addEventListener('message', receive);
                socket.send(arguments[0]);
                """.trimIndent(),
                commonJson.encodeToString(RoomFrame.serializer(), frame),
            )
        val error = assertIs<RoomFrame.Error>(commonJson.decodeFromString<RoomFrame>(assertIs<String>(response)))
        assertEquals(expectedError, error.error, "WS must reject the valid message for the expected permission reason")
    }

    suspend fun assertNoFurtherAttempts() {
        val attempts = count("attempts")
        delay(15_000)
        assertEquals(attempts, count("attempts"), "Authentication failure must stop reconnecting")
    }
}
