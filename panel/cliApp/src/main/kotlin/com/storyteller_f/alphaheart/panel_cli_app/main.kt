/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.panel_cli_app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jakewharton.mosaic.layout.onKeyEvent
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.runMosaicBlocking
import com.jakewharton.mosaic.text.SpanStyle
import com.jakewharton.mosaic.text.buildAnnotatedString
import com.jakewharton.mosaic.text.withStyle
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.Column
import com.jakewharton.mosaic.ui.Text
import com.jakewharton.mosaic.ui.TextStyle
import com.storyteller_f.alphaheart.api.PaginationQuery
import com.storyteller_f.alphaheart.client.core.AuthKey
import com.storyteller_f.alphaheart.client.core.PanelSessionManager
import com.storyteller_f.alphaheart.client.core.SimplePassHolder
import com.storyteller_f.alphaheart.client.core.createSimplePanelSessionManager
import com.storyteller_f.alphaheart.client.core.defaultClientConfigureForPanel
import com.storyteller_f.alphaheart.client.core.getAllCommunities
import com.storyteller_f.alphaheart.client.core.getAllPublicRooms
import com.storyteller_f.alphaheart.client.core.getAllUsers
import com.storyteller_f.alphaheart.client.core.overview
import com.storyteller_f.alphaheart.client.core.panelSignIn
import com.storyteller_f.alphaheart.client.core.panelSignUp
import com.storyteller_f.result.fold
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.result.onFailure
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.getAlgo
import com.storyteller_f.shared.loadCryptoLibIfNeed
import com.storyteller_f.shared.model.AlgoType
import com.storyteller_f.shared.model.CommunityInfo
import com.storyteller_f.shared.model.PanelOverview
import com.storyteller_f.shared.model.RoomInfo
import com.storyteller_f.shared.model.UserInfo
import io.github.aakira.napier.Antilog
import io.github.aakira.napier.LogLevel
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import kotlinx.coroutines.channels.Channel

private val LogGray = Color(160, 160, 160)
private val TitleGreen = Color(100, 255, 100)

val logs = mutableStateListOf<String>()

class ConsoleAntilog : Antilog() {
    override fun performLog(priority: LogLevel, tag: String?, throwable: Throwable?, message: String?) {
        if (message != null) {
            val prefix = tag?.let { "[$it] " }.orEmpty()
            logs.add("$priority $prefix$message")
            if (logs.size > 15) logs.removeAt(0)
        }
        throwable?.let {
            logs.add("$priority ${it.message ?: "<none>"}")
            if (logs.size > 15) logs.removeAt(0)
        }
    }
}

fun sysLog(s: String) {
    Napier.i(s)
}

fun sysLogError(s: String, t: Throwable? = null) {
    if (t != null) {
        Napier.e(s, t)
    } else {
        Napier.e(s)
    }
}

sealed interface Screen {
    data object Main : Screen
    data class PromptRegister(val pem: String = "") : Screen
    data class PromptLogin(val pem: String = "") : Screen
    data class UserList(val users: List<UserInfo>) : Screen
    data class CommunityList(val communities: List<CommunityInfo>) : Screen
    data class RoomList(val rooms: List<RoomInfo>) : Screen
    data class Overview(val state: PanelOverview) : Screen
}

private sealed interface PemEntry {
    data class Pending(val pem: String) : PemEntry
    data class Complete(val pem: String) : PemEntry
}

private fun appendPemLine(previous: String, line: String): PemEntry =
    when {
        previous.isEmpty() && !line.startsWith("-----BEGIN ") -> PemEntry.Complete(line)
        line.startsWith("-----END ") -> PemEntry.Complete(previous + line + "\n")
        else -> PemEntry.Pending(previous + line + "\n")
    }

@Suppress("CyclomaticComplexMethod", "LongMethod")
suspend fun handleInput(
    line: String,
    screen: Screen,
    setScreen: (Screen) -> Unit,
    sessionManager: PanelSessionManager,
    passHolder: SimplePassHolder,
    exit: () -> Nothing,
) {
    when (screen) {
        is Screen.Main -> {
            when (line) {
                "1" -> setScreen(Screen.PromptRegister())

                "2" -> setScreen(Screen.PromptLogin())

                "3" -> {
                    sysLog("Fetching Users...")
                    sessionManager.getAllUsers(PaginationQuery(size = 10)).fold(
                        onSuccess = { setScreen(Screen.UserList(it.data)) },
                        onFailure = { sysLogError("Failed", it.cause) },
                    )
                }

                "4" -> {
                    sysLog("Fetching Communities...")
                    sessionManager.getAllCommunities(PaginationQuery(size = 10)).fold(
                        onSuccess = { setScreen(Screen.CommunityList(it.data)) },
                        onFailure = { sysLogError("Failed", it.cause) },
                    )
                }

                "5" -> {
                    sysLog("Fetching Rooms...")
                    sessionManager.getAllPublicRooms(PaginationQuery(size = 10)).fold(
                        onSuccess = { setScreen(Screen.RoomList(it.data)) },
                        onFailure = { sysLogError("Failed", it.cause) },
                    )
                }

                "6" -> {
                    sysLog("Fetching Overview...")
                    sessionManager.overview().fold(
                        onSuccess = { setScreen(Screen.Overview(it)) },
                        onFailure = { sysLogError("Failed", it.cause) },
                    )
                }

                "0" -> exit()

                else -> sysLog("Invalid Choice")
            }
        }

        is Screen.PromptRegister -> {
            val key =
                when (val entry = appendPemLine(screen.pem, line)) {
                    is PemEntry.Pending -> {
                        setScreen(screen.copy(pem = entry.pem))
                        return
                    }

                    is PemEntry.Complete -> entry.pem
                }
            var pk = key
            if (pk.isEmpty()) {
                val algo = getAlgo(AlgoType.P256)
                pk = algo.generatePemKeyPair().getOrNull()?.first.orEmpty()
                sysLog("Generated Private Key: $pk")
            }
            if (pk.isNotEmpty()) {
                try {
                    val algo = getAlgo(AlgoType.P256)
                    val derPriKey = algo.getDerPrivateKey(pk).getOrThrow()
                    val derPubKey = algo.getDerPublicKeyFromPrivateKey(pk).getOrThrow()
                    val user =
                        sessionManager.panelSignUp(
                            AuthKey.P256(pk, derPriKey, derPubKey),
                            passHolder,
                        )
                    sysLog("Registered and Logged in as: ${user.name}")
                    setScreen(Screen.Main)
                } catch (cancellation: kotlin.coroutines.cancellation.CancellationException) {
                    throw cancellation
                } catch (e: Exception) {
                    sysLogError("Error", e)
                    setScreen(Screen.Main)
                }
            }
        }

        is Screen.PromptLogin -> {
            val key =
                when (val entry = appendPemLine(screen.pem, line)) {
                    is PemEntry.Pending -> {
                        setScreen(screen.copy(pem = entry.pem))
                        return
                    }

                    is PemEntry.Complete -> entry.pem
                }
            try {
                val algo = getAlgo(AlgoType.P256)
                val derPriKey = algo.getDerPrivateKey(key).getOrThrow()
                val derPubKey = algo.getDerPublicKeyFromPrivateKey(key).getOrThrow()
                val user =
                    sessionManager.panelSignIn(
                        AuthKey.P256(key, derPriKey, derPubKey),
                        passHolder,
                    )
                sysLog("Logged in as: ${user.name}")
                setScreen(Screen.Main)
            } catch (cancellation: kotlin.coroutines.cancellation.CancellationException) {
                throw cancellation
            } catch (e: Exception) {
                sysLogError("Error", e)
                setScreen(Screen.Main)
            }
        }

        is Screen.UserList -> {
            if (line == "") {
                setScreen(Screen.Main)
            }
        }

        is Screen.CommunityList -> {
            if (line == "") {
                setScreen(Screen.Main)
            }
        }

        is Screen.RoomList -> {
            if (line == "") {
                setScreen(Screen.Main)
            }
        }

        is Screen.Overview -> {
            if (line == "") {
                setScreen(Screen.Main)
            }
        }
    }
}

@Suppress("ComplexMethod", "LongMethod")
@Composable
fun App(sessionManager: PanelSessionManager, passHolder: SimplePassHolder, exit: () -> Nothing) {
    var screen by remember { mutableStateOf<Screen>(Screen.Main) }
    var inputBuffer by remember { mutableStateOf("") }
    val submitChannel = remember { Channel<String>(Channel.UNLIMITED) }

    LaunchedEffect(Unit) {
        var currentScreen: Screen = Screen.Main
        for (line in submitChannel) {
            handleInput(
                line,
                currentScreen,
                { next ->
                    currentScreen = next
                    screen = next
                },
                sessionManager,
                passHolder,
                exit,
            )
        }
    }

    Column(
        modifier =
        Modifier.onKeyEvent { event ->
            if (event.key == "Enter") {
                submitChannel.trySend(inputBuffer)
                inputBuffer = ""
            } else if (event.key == "Backspace") {
                if (inputBuffer.isNotEmpty()) inputBuffer = inputBuffer.dropLast(1)
            } else if (event.key == "Space") {
                inputBuffer += " "
            } else if (event.key.length == 1) {
                inputBuffer += event.key
            }
            true
        },
    ) {
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = TitleGreen, textStyle = TextStyle.Bold)) {
                    append("PANEL CLI")
                }
            },
        )
        Text("===============")
        Text("")
        when (val s = screen) {
            is Screen.Main -> {
                Text("--- Menu ---")
                Text("1. Register")
                Text("2. Login")
                Text("3. Get All Users")
                Text("4. Get All Communities")
                Text("5. Get All Public Rooms")
                Text("6. Get Overview")
                Text("0. Exit")
                Text("\nChoice: ")
            }

            is Screen.PromptRegister -> {
                Text("Enter Private Key (leave empty for auto-generate): ")
            }

            is Screen.PromptLogin -> {
                Text("Enter Private Key: ")
            }

            is Screen.UserList -> {
                Text("=== Member List ===")
                s.users.forEachIndexed { i, u ->
                    Text("[$i] User ID: ${u.id} - ${u.nickname}")
                }
                Text("===================")
                Text("\nHit enter to go back: ")
            }

            is Screen.CommunityList -> {
                Text("=== Community List ===")
                s.communities.forEachIndexed { i, c ->
                    Text("[$i] Community ID: ${c.id} - ${c.name}")
                }
                Text("======================")
                Text("\nHit enter to go back: ")
            }

            is Screen.RoomList -> {
                Text("=== Room List ===")
                s.rooms.forEachIndexed { i, r ->
                    Text("[$i] Room ID: ${r.id} - ${r.name}")
                }
                Text("=================")
                Text("\nHit enter to go back: ")
            }

            is Screen.Overview -> {
                Text("=== Overview ===")
                Text("User count: ${s.state.userCount}")
                Text("Topic count: ${s.state.topicCount}")
                Text("Community count: ${s.state.communityCount}")
                Text("Private room count: ${s.state.privateRoomCount}")
                Text("Community room count: ${s.state.communityRoomCount}")
                Text("File count: ${s.state.fileCount}")
                Text("File volume: ${s.state.fileVolume}")
                Text("Title count: ${s.state.titleCount}")
                Text("=================")
                Text("\nHit enter to go back: ")
            }
        }

        Text("")
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = TitleGreen)) {
                    append("> ")
                }
                append(inputBuffer)
                append("\u2588")
            },
        )
        Text("")
        Text("--- Logs ---")
        logs.forEach { logText ->
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = LogGray)) {
                        append(logText)
                    }
                },
            )
        }
    }
}

fun main() {
    kotlinx.coroutines.runBlocking {
        Napier.base(ConsoleAntilog())
        loadCryptoLibIfNeed()

        val httpUrl = System.getenv("SERVER_URL") ?: "http://127.0.0.1:8080"

        val passHolder = SimplePassHolder()

        val sessionManager =
            createSimplePanelSessionManager(
                passHolder,
                AcceptAllCookiesStorage(),
            ) { model, cookieManager ->
                HttpClient(OkHttp) {
                    defaultClientConfigureForPanel(cookieManager, model, httpUrl)
                }
            }

        runMosaicBlocking {
            App(sessionManager, passHolder) { kotlin.system.exitProcess(0) }
        }
    }
}
