/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cli_app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jakewharton.mosaic.layout.onKeyEvent
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.runMosaic
import com.jakewharton.mosaic.text.SpanStyle
import com.jakewharton.mosaic.text.buildAnnotatedString
import com.jakewharton.mosaic.text.withStyle
import com.jakewharton.mosaic.ui.Color
import com.jakewharton.mosaic.ui.Column
import com.jakewharton.mosaic.ui.Text
import com.jakewharton.mosaic.ui.TextStyle
import com.storyteller_f.alphaheart.api.CustomApi
import com.storyteller_f.alphaheart.api.NewFavorite
import com.storyteller_f.alphaheart.api.NewSubscription
import com.storyteller_f.alphaheart.api.PaginationQuery
import com.storyteller_f.alphaheart.client.core.AuthKey
import com.storyteller_f.alphaheart.client.core.SimplePassHolder
import com.storyteller_f.alphaheart.client.core.UserSessionManager
import com.storyteller_f.alphaheart.client.core.addFavorite
import com.storyteller_f.alphaheart.client.core.addReaction
import com.storyteller_f.alphaheart.client.core.addSubscription
import com.storyteller_f.alphaheart.client.core.buildWebSocketUrl
import com.storyteller_f.alphaheart.client.core.createSimpleUserSessionManager
import com.storyteller_f.alphaheart.client.core.createTopic
import com.storyteller_f.alphaheart.client.core.defaultClientConfigure
import com.storyteller_f.alphaheart.client.core.exitCommunity
import com.storyteller_f.alphaheart.client.core.exitRoom
import com.storyteller_f.alphaheart.client.core.getCommunityRooms
import com.storyteller_f.alphaheart.client.core.getCommunityTopics
import com.storyteller_f.alphaheart.client.core.getFavorites
import com.storyteller_f.alphaheart.client.core.getRecommendTopics
import com.storyteller_f.alphaheart.client.core.getRoomMembersPublicKeys
import com.storyteller_f.alphaheart.client.core.getRoomTopics
import com.storyteller_f.alphaheart.client.core.getSubscriptions
import com.storyteller_f.alphaheart.client.core.getUserCommunities
import com.storyteller_f.alphaheart.client.core.getUserRooms
import com.storyteller_f.alphaheart.client.core.joinCommunity
import com.storyteller_f.alphaheart.client.core.joinRoom
import com.storyteller_f.alphaheart.client.core.onBackgroundTask
import com.storyteller_f.alphaheart.client.core.searchAllMembers
import com.storyteller_f.alphaheart.client.core.searchCommunityMembers
import com.storyteller_f.alphaheart.client.core.searchRoomMembers
import com.storyteller_f.alphaheart.client.core.sendMessage
import com.storyteller_f.alphaheart.client.core.userSignIn
import com.storyteller_f.alphaheart.client.core.userSignUp
import com.storyteller_f.result.fold
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.result.onFailure
import com.storyteller_f.result.onSuccess
import com.storyteller_f.shared.getAlgo
import com.storyteller_f.shared.loadCryptoLibIfNeed
import com.storyteller_f.shared.model.AlgoType
import com.storyteller_f.shared.model.CommunityInfo
import com.storyteller_f.shared.model.MemberInfo
import com.storyteller_f.shared.model.RoomInfo
import com.storyteller_f.shared.model.TopicContent
import com.storyteller_f.shared.model.UserFavoriteItem
import com.storyteller_f.shared.model.UserInfo
import com.storyteller_f.shared.model.UserSubscriptionItem
import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.type.ObjectType
import io.github.aakira.napier.Antilog
import io.github.aakira.napier.LogLevel
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import kotlinx.coroutines.channels.Channel
import com.storyteller_f.shared.model.TopicDetail as TopicData

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
    data class TopicList(val topics: List<TopicData>) : Screen
    data class TopicDetail(val topic: TopicData) : Screen
    data class TopicAddComment(val topic: TopicData) : Screen
    data class TopicAddReaction(val topic: TopicData) : Screen
    data class CommunityList(val communities: List<CommunityInfo>) : Screen
    data class CommunityDetail(val community: CommunityInfo) : Screen
    data class CommunityMemberList(val members: List<MemberInfo>, val parent: Screen) : Screen
    data class RoomMemberList(val members: List<MemberInfo>, val parent: Screen) : Screen
    data class UserList(val users: List<UserInfo>) : Screen
    data class UserDetail(val user: UserInfo, val parent: Screen) : Screen
    data class RoomList(val rooms: List<RoomInfo>) : Screen
    data class RoomDetail(val room: RoomInfo) : Screen
    data class RoomSendMessage(val room: RoomInfo) : Screen
    data class FavoriteList(val favorites: List<UserFavoriteItem>) : Screen
    data class SubscriptionList(val subscriptions: List<UserSubscriptionItem>) : Screen
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

// Mosaic 0.18's legacy key adapter accepts ASCII only. Code points keep reaction input usable.
// https://github.com/JakeWharton/mosaic/blob/0.18.0/mosaic-runtime/src/commonMain/kotlin/com/jakewharton/mosaic/compat.kt
private fun decodeEmojiInput(line: String): String? {
    if (!line.startsWith("U+", ignoreCase = true)) return line
    val points =
        line.trim().split(Regex("\\s+"))
            .map { it.removePrefix("U+").removePrefix("u+").toIntOrNull(16) }
    if (points.any { it == null || !Character.isValidCodePoint(it) || it in 0xD800..0xDFFF }) return null
    return points.joinToString("") { String(Character.toChars(requireNotNull(it))) }
}

@Suppress("CyclomaticComplexMethod", "LongMethod")
suspend fun handleInput(
    line: String,
    screen: Screen,
    setScreen: (Screen) -> Unit,
    sessionManager: UserSessionManager,
    passHolder: SimplePassHolder,
    onExit: () -> Unit,
) {
    when (screen) {
        is Screen.Main -> {
            when (line) {
                "1" -> setScreen(Screen.PromptRegister())

                "2" -> setScreen(Screen.PromptLogin())

                "3" -> {
                    sysLog("Fetching Recommended Topics...")
                    sessionManager.getRecommendTopics(PaginationQuery(size = 10)).fold(
                        onSuccess = { setScreen(Screen.TopicList(it.data)) },
                        onFailure = { sysLogError("Failed", it.cause) },
                    )
                }

                "4" -> {
                    sysLog("Fetching Joined Communities...")
                    sessionManager.getUserCommunities(
                        CustomApi.Users.JoinedCommunities.UserCommunitiesQuery(size = 10),
                    ).fold(
                        onSuccess = { setScreen(Screen.CommunityList(it.data.map { item -> item.communityInfo })) },
                        onFailure = { sysLogError("Failed", it.cause) },
                    )
                }

                "5" -> {
                    sysLog("Fetching Joined Rooms...")
                    sessionManager.getUserRooms(PaginationQuery(size = 10)).fold(
                        onSuccess = { setScreen(Screen.RoomList(it.data)) },
                        onFailure = { sysLogError("Failed", it.cause) },
                    )
                }

                "6" -> {
                    sysLog("Fetching Member List...")
                    sessionManager.searchAllMembers(null, 10, "").fold(
                        onSuccess = { setScreen(Screen.UserList(it.data)) },
                        onFailure = { sysLogError("Failed", it.cause) },
                    )
                }

                "7" -> {
                    sysLog("Fetching Favorites...")
                    sessionManager.getFavorites(PaginationQuery(size = 10)).fold(
                        onSuccess = { setScreen(Screen.FavoriteList(it.data)) },
                        onFailure = { sysLogError("Failed", it.cause) },
                    )
                }

                "8" -> {
                    sysLog("Fetching Subscriptions...")
                    sessionManager.getSubscriptions(PaginationQuery(size = 10)).fold(
                        onSuccess = { setScreen(Screen.SubscriptionList(it.data)) },
                        onFailure = { sysLogError("Failed", it.cause) },
                    )
                }

                "0" -> onExit()

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
                    val authKey = AuthKey.P256(pk, derPriKey, derPubKey)
                    val user = sessionManager.userSignUp(authKey, passHolder)
                    sysLog("Registered and Logged in as: ${user.id} - ${user.nickname}")
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
                val authKey = AuthKey.P256(key, derPriKey, derPubKey)
                val user = sessionManager.userSignIn(authKey, passHolder)
                sysLog("Logged in as: ${user.id} - ${user.nickname}")
                setScreen(Screen.Main)
            } catch (cancellation: kotlin.coroutines.cancellation.CancellationException) {
                throw cancellation
            } catch (e: Exception) {
                sysLogError("Error", e)
                setScreen(Screen.Main)
            }
        }

        is Screen.TopicList -> {
            if (line == "") {
                setScreen(Screen.Main)
            } else {
                val idx = line.toIntOrNull()
                if (idx != null && idx in screen.topics.indices) {
                    setScreen(Screen.TopicDetail(screen.topics[idx]))
                }
            }
        }

        is Screen.TopicDetail -> {
            when (line) {
                "1" -> {
                    sysLog("Favoriting Topic...")
                    sessionManager.addFavorite(NewFavorite(ObjectType.TOPIC, screen.topic.id)).fold(
                        onSuccess = { sysLog("Favorited successfully") },
                        onFailure = { sysLogError("Failed to favorite", it.cause) },
                    )
                }

                "2" -> {
                    sysLog("Subscribing Topic...")
                    sessionManager.addSubscription(
                        NewSubscription(
                            screen.topic.id,
                            ObjectType.TOPIC,
                        ),
                    ).fold(
                        onSuccess = { sysLog("Subscribed successfully") },
                        onFailure = { sysLogError("Failed to subscribe", it.cause) },
                    )
                }

                "3" -> setScreen(Screen.TopicAddComment(screen.topic))

                "4" -> setScreen(Screen.TopicAddReaction(screen.topic))

                "" -> setScreen(Screen.Main)

                else -> sysLog("Invalid Choice")
            }
        }

        is Screen.TopicAddComment -> {
            if (line.isEmpty()) {
                setScreen(Screen.TopicDetail(screen.topic))
            } else {
                sysLog("Adding Comment...")
                sessionManager.createTopic(ObjectType.TOPIC, screen.topic.id, line).fold(
                    onSuccess = { sysLog("Comment added successfully") },
                    onFailure = { sysLogError("Failed to add comment", it.cause) },
                )
                setScreen(Screen.TopicDetail(screen.topic))
            }
        }

        is Screen.TopicAddReaction -> {
            if (line.isEmpty()) {
                setScreen(Screen.TopicDetail(screen.topic))
            } else {
                sysLog("Adding Reaction...")
                val emoji = decodeEmojiInput(line)
                if (emoji == null) {
                    sysLog("Invalid Unicode code points")
                    setScreen(Screen.TopicDetail(screen.topic))
                    return
                }
                sessionManager.addReaction(screen.topic.id, emoji).fold(
                    onSuccess = { sysLog("Reaction added successfully") },
                    onFailure = { sysLogError("Failed to add reaction", it.cause) },
                )
                setScreen(Screen.TopicDetail(screen.topic))
            }
        }

        is Screen.CommunityList -> {
            if (line == "") {
                setScreen(Screen.Main)
            } else {
                val idx = line.toIntOrNull()
                if (idx != null && idx in screen.communities.indices) {
                    setScreen(Screen.CommunityDetail(screen.communities[idx]))
                }
            }
        }

        is Screen.CommunityDetail -> {
            when (line) {
                "1" -> {
                    sysLog("Fetching Community Topics...")
                    sessionManager.getCommunityTopics(
                        screen.community.id,
                        null,
                        PaginationQuery(size = 10),
                    ).fold(
                        onSuccess = { setScreen(Screen.TopicList(it.data)) },
                        onFailure = { sysLogError("Failed", it.cause) },
                    )
                }

                "2" -> {
                    sysLog("Fetching Community Rooms...")
                    sessionManager.getCommunityRooms(
                        screen.community.id,
                        CustomApi.Communities.Id.Rooms.CommunityRoomQuery(size = 10),
                    ).fold(
                        onSuccess = { setScreen(Screen.RoomList(it.data)) },
                        onFailure = { sysLogError("Failed", it.cause) },
                    )
                }

                "3" -> {
                    sysLog("Fetching Community Members...")
                    sessionManager.searchCommunityMembers(screen.community.id, null, 10, "").fold(
                        onSuccess = { setScreen(Screen.CommunityMemberList(it.data, screen)) },
                        onFailure = { sysLogError("Failed", it.cause) },
                    )
                }

                "4" -> {
                    sysLog("Joining Community...")
                    sessionManager.joinCommunity(screen.community.id).fold(
                        onSuccess = { sysLog("Joined Community successfully") },
                        onFailure = { sysLogError("Failed to join", it.cause) },
                    )
                }

                "5" -> {
                    sysLog("Exiting Community...")
                    sessionManager.exitCommunity(screen.community.id).fold(
                        onSuccess = { sysLog("Exited Community successfully") },
                        onFailure = { sysLogError("Failed to exit", it.cause) },
                    )
                }

                "6" -> {
                    sysLog("Favoriting Community...")
                    sessionManager.addFavorite(
                        NewFavorite(
                            ObjectType.COMMUNITY,
                            screen.community.id,
                        ),
                    ).fold(
                        onSuccess = { sysLog("Favorited successfully") },
                        onFailure = { sysLogError("Failed to favorite", it.cause) },
                    )
                }

                "7" -> {
                    sysLog("Subscribing Community...")
                    sessionManager.addSubscription(
                        NewSubscription(
                            screen.community.id,
                            ObjectType.COMMUNITY,
                        ),
                    ).fold(
                        onSuccess = { sysLog("Subscribed successfully") },
                        onFailure = { sysLogError("Failed to subscribe", it.cause) },
                    )
                }

                "" -> setScreen(Screen.Main)

                else -> sysLog("Invalid Choice")
            }
        }

        is Screen.CommunityMemberList -> {
            if (line == "") {
                setScreen(screen.parent)
            } else {
                val idx = line.toIntOrNull()
                if (idx != null && idx in screen.members.indices) {
                    setScreen(Screen.UserDetail(screen.members[idx].userInfo, screen))
                }
            }
        }

        is Screen.RoomMemberList -> {
            if (line == "") {
                setScreen(screen.parent)
            } else {
                val idx = line.toIntOrNull()
                if (idx != null && idx in screen.members.indices) {
                    setScreen(Screen.UserDetail(screen.members[idx].userInfo, screen))
                }
            }
        }

        is Screen.UserList -> {
            if (line == "") {
                setScreen(Screen.Main)
            } else {
                val idx = line.toIntOrNull()
                if (idx != null && idx in screen.users.indices) {
                    setScreen(Screen.UserDetail(screen.users[idx], screen))
                }
            }
        }

        is Screen.UserDetail -> {
            setScreen(screen.parent)
        }

        is Screen.RoomList -> {
            if (line == "") {
                setScreen(Screen.Main)
            } else {
                val idx = line.toIntOrNull()
                if (idx != null && idx in screen.rooms.indices) {
                    setScreen(Screen.RoomDetail(screen.rooms[idx]))
                }
            }
        }

        is Screen.RoomDetail -> {
            when (line) {
                "1" -> {
                    sysLog("Fetching Room Messages...")
                    sessionManager.getRoomTopics(screen.room.id, null, PaginationQuery(size = 10))
                        .fold(
                            onSuccess = { setScreen(Screen.TopicList(it.data)) },
                            onFailure = { sysLogError("Failed", it.cause) },
                        )
                }

                "2" -> {
                    sysLog("Fetching Room Members...")
                    sessionManager.searchRoomMembers(screen.room.id, null, 10, "").fold(
                        onSuccess = { setScreen(Screen.RoomMemberList(it.data, screen)) },
                        onFailure = { sysLogError("Failed", it.cause) },
                    )
                }

                "3" -> {
                    sysLog("Joining Room...")
                    sessionManager.joinRoom(screen.room.id).fold(
                        onSuccess = { sysLog("Joined Room successfully") },
                        onFailure = { sysLogError("Failed to join", it.cause) },
                    )
                }

                "4" -> {
                    sysLog("Exiting Room...")
                    sessionManager.exitRoom(screen.room.id).fold(
                        onSuccess = { sysLog("Exited Room successfully") },
                        onFailure = { sysLogError("Failed to exit", it.cause) },
                    )
                }

                "5" -> {
                    setScreen(Screen.RoomSendMessage(screen.room))
                }

                "6" -> {
                    sysLog("Favoriting Room...")
                    sessionManager.addFavorite(NewFavorite(ObjectType.ROOM, screen.room.id)).fold(
                        onSuccess = { sysLog("Favorited successfully") },
                        onFailure = { sysLogError("Failed to favorite", it.cause) },
                    )
                }

                "7" -> {
                    sysLog("Subscribing Room...")
                    sessionManager.addSubscription(NewSubscription(screen.room.id, ObjectType.ROOM))
                        .fold(
                            onSuccess = { sysLog("Subscribed successfully") },
                            onFailure = { sysLogError("Failed to subscribe", it.cause) },
                        )
                }

                "" -> setScreen(Screen.Main)

                else -> sysLog("Invalid Choice")
            }
        }

        is Screen.FavoriteList -> {
            if (line == "") {
                setScreen(Screen.Main)
            }
        }

        is Screen.SubscriptionList -> {
            if (line == "") {
                setScreen(Screen.Main)
            }
        }

        is Screen.RoomSendMessage -> {
            if (line.isEmpty()) {
                setScreen(Screen.RoomDetail(screen.room))
            } else {
                sysLog("Sending Message to Room...")
                val parentTarget = ObjectTuple(screen.room.id, ObjectType.ROOM)

                var keyData: List<com.storyteller_f.shared.model.UserPubKeyInfo>? = null
                if (screen.room.isPrivate) {
                    val res =
                        sessionManager.getRoomMembersPublicKeys(
                            screen.room.id,
                            PaginationQuery(size = 100),
                        )
                    if (res.isSuccess) {
                        keyData = res.getOrNull()?.data
                    }
                }

                try {
                    sessionManager.webSocketClient.useWebSocket {
                        sendMessage(parentTarget, screen.room.isPrivate, line, keyData.orEmpty())
                    }.getOrThrow()
                    sysLog("Message Sent")
                } catch (cancellation: kotlin.coroutines.cancellation.CancellationException) {
                    throw cancellation
                } catch (e: Exception) {
                    sysLogError("Failed to send message", e)
                }
                setScreen(Screen.RoomDetail(screen.room))
            }
        }
    }
}

@Suppress("ComplexMethod", "LongMethod")
@Composable
fun App(sessionManager: UserSessionManager, passHolder: SimplePassHolder, onExit: () -> Unit) {
    var screen by remember { mutableStateOf<Screen>(Screen.Main) }
    var inputBuffer by remember { mutableStateOf("") }
    val submitChannel = remember { Channel<String>(Channel.UNLIMITED) }

    LaunchedEffect(Unit) {
        var currentScreen: Screen = Screen.Main
        for (line in submitChannel) {
            handleInput(
                line = line,
                screen = currentScreen,
                setScreen = { next ->
                    currentScreen = next
                    screen = next
                },
                sessionManager = sessionManager,
                passHolder = passHolder,
                onExit = onExit,
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
                    append("STORYTELLER CLI")
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
                Text("3. Recommend Topics")
                Text("4. Joined Communities")
                Text("5. Joined Rooms")
                Text("6. Member List")
                Text("7. My Favorites")
                Text("8. My Subscriptions")
                Text("0. Exit")
                Text("\nChoice: ")
            }

            is Screen.PromptRegister -> {
                Text("Enter Private Key (leave empty for auto-generate): ")
            }

            is Screen.PromptLogin -> {
                Text("Enter Private Key: ")
            }

            is Screen.TopicList -> {
                Text("Topic/Message List:")
                s.topics.forEachIndexed { i, t ->
                    val content = t.content
                    val title = if (content is TopicContent.Plain) content.plain else "Encrypted"
                    Text("[$i] ${t.id}: $title")
                }
                Text("\nEnter index to view details (or hit enter to go back): ")
            }

            is Screen.TopicDetail -> {
                Text("=== Topic Detail ===")
                Text("ID: ${s.topic.id}")
                Text("Root ID: ${s.topic.rootId}")
                Text("Author: ${s.topic.authorInfo?.nickname ?: "<none>"}")
                Text("Content: ${s.topic.content}")
                Text("====================")
                Text("1. Favorite Topic")
                Text("2. Subscribe Topic")
                Text("3. Add Comment")
                Text("4. Add Reaction")
                Text("")
                Text("Hit enter to go back.")
            }

            is Screen.TopicAddComment -> {
                Text("=== Add Comment ===")
                Text("Topic: ${s.topic.id}")
                Text("Type your comment and hit Enter.")
                Text("Hit enter with empty input to go back.")
                Text("Comment: ")
            }

            is Screen.TopicAddReaction -> {
                Text("=== Add Reaction ===")
                Text("Topic: ${s.topic.id}")
                Text("Type Unicode code points (for example U+1F44D) and hit Enter.")
                Text("Hit enter with empty input to go back.")
                Text("Emoji: ")
            }

            is Screen.CommunityList -> {
                Text("Joined Communities:")
                s.communities.forEachIndexed { i, c ->
                    Text("[$i] ${c.id}: ${c.name}")
                }
                Text("\nEnter index to view details (or hit enter to go back): ")
            }

            is Screen.CommunityDetail -> {
                Text("=== Community Detail ===")
                Text("ID: ${s.community.id}")
                Text("Name: ${s.community.name}")
                Text("Member Count: ${s.community.memberCount}")
                Text("========================")
                Text("1. View Topics")
                Text("2. View Rooms")
                Text("3. View Members")
                Text("4. Join Community")
                Text("5. Exit Community")
                Text("6. Favorite Community")
                Text("7. Subscribe Community")
                Text("")
                Text("Enter choice (or hit enter to go back): ")
            }

            is Screen.CommunityMemberList -> {
                Text("=== Community Members ===")
                s.members.forEachIndexed { i, m ->
                    Text("[$i] User ID: ${m.uid} - ${m.userInfo.nickname}")
                }
                Text("========================")
                Text("\nEnter index to view user details (or hit enter to go back): ")
            }

            is Screen.RoomMemberList -> {
                Text("=== Room Members ===")
                s.members.forEachIndexed { i, m ->
                    Text("[$i] User ID: ${m.uid} - ${m.userInfo.nickname}")
                }
                Text("==================")
                Text("\nEnter index to view user details (or hit enter to go back): ")
            }

            is Screen.UserList -> {
                Text("=== Member List ===")
                s.users.forEachIndexed { i, u ->
                    Text("[$i] User ID: ${u.id} - ${u.nickname}")
                }
                Text("===================")
                Text("\nEnter index to view user details (or hit enter to go back): ")
            }

            is Screen.UserDetail -> {
                Text("=== User Detail ===")
                Text("ID: ${s.user.id}")
                Text("Nickname: ${s.user.nickname}")
                Text("Address: ${s.user.address}")
                Text("Aid: ${s.user.aid ?: "None"}")
                Text("Status: ${s.user.status}")
                Text("===================")
                Text("Hit enter to go back.")
            }

            is Screen.RoomList -> {
                Text("Joined Rooms:")
                s.rooms.forEachIndexed { i, r ->
                    Text("[$i] ${r.id}: ${r.name}")
                }
                Text("\nEnter index to view details (or hit enter to go back): ")
            }

            is Screen.RoomDetail -> {
                Text("=== Room Detail ===")
                Text("ID: ${s.room.id}")
                Text("Name: ${s.room.name}")
                Text("Member Count: ${s.room.memberCount}")
                Text("===================")
                Text("1. View Messages")
                Text("2. View Members")
                Text("3. Join Room")
                Text("4. Exit Room")
                Text("5. Send Message")
                Text("6. Favorite Room")
                Text("7. Subscribe Room")
                Text("")
                Text("Enter choice (or hit enter to go back): ")
            }

            is Screen.FavoriteList -> {
                Text("=== Favorites ===")
                s.favorites.forEachIndexed { i, f ->
                    val extra =
                        f.topicInfo?.content
                            ?: f.communityInfo?.name
                            ?: f.roomInfo?.name
                            ?: "Unknown"
                    Text("[$i] ${f.objectType} ${f.objectId}: $extra")
                }
                Text("\nHit enter to go back: ")
            }

            is Screen.SubscriptionList -> {
                Text("=== Subscriptions ===")
                s.subscriptions.forEachIndexed { i, sub ->
                    val extra = sub.topicInfo?.content ?: "Unknown"
                    Text("[$i] ${sub.objectType} ${sub.objectId}: $extra")
                }
                Text("\nHit enter to go back: ")
            }

            is Screen.RoomSendMessage -> {
                Text("=== Send Message ===")
                Text("Room: ${s.room.name}")
                Text("Type your message and hit Enter.")
                Text("Hit enter with empty input to go back.")
                Text("Message: ")
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

        val wsUrl = System.getenv("WS_SERVER_URL") ?: "ws://127.0.0.1:8080"
        val httpUrl = System.getenv("SERVER_URL") ?: "http://127.0.0.1:8080"

        val passHolder = SimplePassHolder()
        val sessionManager =
            createSimpleUserSessionManager(
                webSocketUrl = buildWebSocketUrl(wsUrl),
                cookieManager = AcceptAllCookiesStorage(),
                passHolder = passHolder,
                createClient = { model, cookieManager ->
                    HttpClient(OkHttp) {
                        defaultClientConfigure(
                            cookiesStorage = cookieManager,
                            manager = model,
                            httpUrl = httpUrl,
                        )
                    }
                },
                onReceiveFrame = { _, _, _ -> },
            )

        sessionManager.onBackgroundTask {
            runMosaic {
                App(sessionManager, passHolder) { kotlin.system.exitProcess(0) }
            }
        }
    }
}
