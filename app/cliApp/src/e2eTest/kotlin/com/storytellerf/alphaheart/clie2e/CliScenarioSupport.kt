/*
 * This is a private project. All rights reserved.
 */

package com.storytellerf.alphaheart.clie2e

import com.storyteller_f.alphaheart.api.NewCommunity
import com.storyteller_f.alphaheart.api.NewRoom
import com.storyteller_f.alphaheart.api.PaginationQuery
import com.storyteller_f.alphaheart.client.core.createCommunity
import com.storyteller_f.alphaheart.client.core.createRoom
import com.storyteller_f.alphaheart.client.core.createTopic
import com.storyteller_f.alphaheart.client.core.getRecommendTopics
import com.storyteller_f.alphaheart.client.core.getRoomTopics
import com.storyteller_f.alphaheart.client.core.joinCommunity
import com.storyteller_f.alphaheart.client.core.joinRoom
import com.storyteller_f.alphaheart.client.core.sendMessage
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.model.CommunityInfo
import com.storyteller_f.shared.model.RoomInfo
import com.storyteller_f.shared.model.TopicContent
import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.type.ObjectType
import com.storytellerf.alphaheart.e2e.E2eCliTerminal
import com.storytellerf.alphaheart.e2e.E2eCliTerminalFactory
import com.storytellerf.alphaheart.e2e.E2ePorts
import com.storytellerf.alphaheart.e2e.E2eUser
import com.storytellerf.alphaheart.e2e.createE2eUser
import com.storytellerf.alphaheart.e2e.runE2eBlockingTest
import com.storytellerf.alphaheart.e2e.runE2eTestEnvironment
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.junit.Rule
import org.junit.rules.TestName
import kotlin.test.assertTrue

internal abstract class CliScenarioTest {
    protected abstract val suiteName: String

    @get:Rule
    val testName = TestName()
    private val factory = E2eCliTerminalFactory()

    protected fun withGuest(scenario: suspend (E2ePorts, E2eCliTerminal) -> Unit) {
        runE2eBlockingTest {
            runE2eTestEnvironment(suiteName, testName.methodName) { ports ->
                factory.useTerminal("cli.app.install.dir", ports, this) { terminal ->
                    terminal.awaitText("STORYTELLER CLI")
                    scenario(ports, terminal)
                }
            }
        }
    }

    protected fun withUser(scenario: suspend (E2eCliTerminal, E2eUser) -> Unit) =
        withFixture({ _, _ -> Unit }) { terminal, user, _ -> scenario(terminal, user) }

    protected fun <T> withFixture(
        setup: suspend (E2ePorts, E2eUser) -> T,
        scenario: suspend (E2eCliTerminal, E2eUser, T) -> Unit,
    ) = withGuest { ports, terminal ->
        createE2eUser(ports).use { user ->
            val data = setup(ports, user)
            terminal.sendLine("2")
            terminal.awaitText("Enter Private Key:")
            user.key.lineSequence().filter(String::isNotBlank).forEach { terminal.sendLine(it) }
            terminal.awaitText("Logged in as: ${user.id}")
            scenario(terminal, user, data)
        }
    }

    protected fun withTopic(scenario: suspend (E2eCliTerminal, E2eUser, CliTopic) -> Unit) =
        withFixture({ ports, user ->
            createE2eUser(ports).use { author ->
                val community =
                    author.session.createCommunity(
                        NewCommunity("cli-topic-community", "clitopic"),
                    ).getOrThrow()
                user.session.joinCommunity(community.id).getOrThrow()
                val content = "cli-contract-topic"
                val topic = author.session.createTopic(ObjectType.COMMUNITY, community.id, content).getOrThrow()
                withTimeout(30_000) {
                    while (true) {
                        val page = user.session.getRecommendTopics(PaginationQuery(size = 10)).getOrThrow()
                        if (page.data.any { it.id == topic.id }) {
                            break
                        }
                        delay(100)
                    }
                }
                CliTopic(topic.id, content)
            }
        }, scenario)

    protected fun withRoom(scenario: suspend (E2eCliTerminal, E2eUser, CliRoom) -> Unit) =
        withFixture({ ports, user -> createCliRoomForTest(ports, user) }, scenario)
}

internal data class CliTopic(val id: Long, val text: String)
internal data class CliRoom(val community: CommunityInfo, val room: RoomInfo)

internal suspend fun createCliRoomForTest(ports: E2ePorts, viewer: E2eUser): CliRoom =
    createE2eUser(ports).use { owner ->
        val community = owner.session.createCommunity(NewCommunity("cli-community", "clicommunity")).getOrThrow()
        val room = owner.session.createRoom(NewRoom("cli-room", "cliroom", communityId = community.id)).getOrThrow()
        viewer.session.joinCommunity(community.id).getOrThrow()
        viewer.session.joinRoom(room.id).getOrThrow()
        CliRoom(community, room)
    }

internal suspend fun E2eCliTerminal.openTopic(user: E2eUser, topic: CliTopic) {
    val index =
        user.session.getRecommendTopics(
            PaginationQuery(size = 10),
        ).getOrThrow().data.indexOfFirst { it.id == topic.id }
    assertTrue(index >= 0, "Seed topic must be in the recommendation page")
    sendLine("3")
    awaitText("Topic/Message List:")
    awaitText(topic.text)
    sendLine(index.toString())
    awaitText("=== Topic Detail ===")
    awaitText("ID: ${topic.id}")
}

internal suspend fun E2eCliTerminal.openCommunity(data: CliRoom) {
    sendLine("4")
    awaitText("Joined Communities:")
    awaitText(data.community.name)
    sendLine("0")
    awaitText("=== Community Detail ===")
    awaitText("ID: ${data.community.id}")
}

internal suspend fun E2eCliTerminal.openRoom(data: CliRoom) {
    sendLine("5")
    awaitText("Joined Rooms:")
    awaitText(data.room.name)
    sendLine("0")
    awaitText("=== Room Detail ===")
    awaitText("ID: ${data.room.id}")
}

internal suspend fun awaitCliMessage(user: E2eUser, room: Long, text: String) =
    withTimeout(30_000) {
        while (true) {
            val topics = user.session.getRoomTopics(room, paginationQuery = PaginationQuery()).getOrThrow().data
            if (topics.any { (it.content as? TopicContent.Plain)?.plain == text }) return@withTimeout
            delay(100)
        }
    }

internal suspend fun seedCliMessage(user: E2eUser, room: Long, text: String) =
    coroutineScope {
        val socket = user.session.webSocketClient
        val connection = launch { socket.connectWebSocket() }
        try {
            withTimeout(30_000) {
                socket.connectionHandler.data.filterNotNull().first()
                socket.useWebSocket {
                    sendMessage(
                        ObjectTuple(room, ObjectType.ROOM),
                        false,
                        text,
                        emptyList(),
                    )
                }.getOrThrow()
                awaitCliMessage(user, room, text)
            }
        } finally {
            connection.cancelAndJoin()
        }
    }
