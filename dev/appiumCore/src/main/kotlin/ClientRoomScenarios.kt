/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import com.storyteller_f.alphaheart.api.NewCommunity
import com.storyteller_f.alphaheart.api.NewRoom
import com.storyteller_f.alphaheart.api.NewTitle
import com.storyteller_f.alphaheart.api.PaginationQuery
import com.storyteller_f.alphaheart.api.RoomActivityInfo
import com.storyteller_f.alphaheart.client.core.ServerErrorException
import com.storyteller_f.alphaheart.client.core.createCommunity
import com.storyteller_f.alphaheart.client.core.createRoom
import com.storyteller_f.alphaheart.client.core.createTitle
import com.storyteller_f.alphaheart.client.core.getRoomTopics
import com.storyteller_f.alphaheart.client.core.getUserRooms
import com.storyteller_f.alphaheart.client.core.joinCommunity
import com.storyteller_f.alphaheart.client.core.joinRoom
import com.storyteller_f.alphaheart.client.core.updateUserStatus
import com.storyteller_f.result.exceptionOrNull
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.model.TitleType
import com.storyteller_f.shared.model.TopicContent
import com.storyteller_f.shared.obj.UpdateUserStatusBody
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.UserStatus
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlin.test.assertEquals
import kotlin.test.assertIs

fun AppiumTestBase<AppAppiumHelper, WasmAppiumHelper>.testTwoClientMessages() =
    runRoomClients(privateRoom = false, scenario = ::exchangeRoomMessages)

fun AppiumTestBase<AppAppiumHelper, WasmAppiumHelper>.testPrivateClientIsolation() =
    runRoomClients(privateRoom = true) { clients, data ->
        val sender = clients.getValue("sender").driver
        val recipient = clients.getValue("recipient").driver
        sender.openClientRoom(data.roomName)
        recipient.clickByText("Rooms")
        val message = "private-peer-${System.currentTimeMillis()}"
        sender.sendClientMessage(message)
        recipient.assertVisibleByTextContaining(message)
        recipient.clickByTextContaining(data.roomName)
        recipient.assertVisibleByText(message)
        awaitClientActivity(data.recipient, data.roomId, message, 0)
        val recipientScope = clients.getValue("recipient")
        recipientScope.restart()
        recipientScope.driver.openClientRoom(data.roomName)
        recipientScope.driver.assertVisibleByText(message)
    }

fun AppiumTestBase<AppAppiumHelper, WasmAppiumHelper>.testPrivateOutsiderIsolation() =
    runRoomClients(privateRoom = true, outsider = true) { clients, data ->
        val sender = clients.getValue("sender").driver
        val outsider = clients.getValue("outsider").driver
        sender.openClientRoom(data.roomName)
        val message = "private-isolated-${System.currentTimeMillis()}"
        sender.sendClientMessage(message)
        outsider.clickByText("Rooms")
        outsider.assertNotVisibleByText(data.roomName)
        outsider.assertNotVisibleByText(message)
        val error =
            data.accounts.getValue("outsider").sessionManager.getRoomTopics(
                data.roomId,
                paginationQuery = PaginationQuery(),
            ).exceptionOrNull()
        assertEquals(HttpStatusCode.Forbidden, assertIs<ServerErrorException>(error).status)
        val probe = (outsider as WasmAppTestDriver).socketProbe()
        probe.awaitOpen()
        probe.assertRoomWriteRejected(data.roomId, "forbidden-private-write")
        assertEquals(
            1,
            data.sender.sessionManager.getRoomTopics(
                data.roomId,
                paginationQuery = PaginationQuery(),
            ).getOrThrow().data.size,
        )
    }

fun AppiumTestBase<AppAppiumHelper, WasmAppiumHelper>.testReadOnlyClientPermissions() =
    runAppiumBlockingTest {
        platformHelper.runClients(testName, targetHelper, setup = { ports ->
            val room = prepareClientRoom(ports, privateRoom = false)
            var retained = false
            try {
                val admin = PanelAppiumHelper().createAuthenticatedSession(ports)
                val prepared =
                    WasmClientsSetup(room to admin, room.accounts.keys.associateWith { null }) {
                        admin.sessionManager.client.close()
                        room.close()
                    }
                retained = true
                prepared
            } finally {
                if (!retained) room.close()
            }
        }) { clients, (data, admin) ->
            signInClientRoles(clients, data)
            clients.getValue("recipient").driver.clickByText("Rooms")
            val client = clients.getValue("sender")
            client.driver.openClientRoom(data.roomName)
            val text = "readable-before-restriction-${System.currentTimeMillis()}"
            client.driver.sendClientMessage(text)
            admin.sessionManager.updateUserStatus(
                requireNotNull(data.sender.sessionManager.model.uid),
                UpdateUserStatusBody(UserStatus.READ_ONLY),
            ).getOrThrow()
            client.restart()
            client.driver.openClientRoom(data.roomName)
            client.driver.assertVisibleByText(text)
            client.driver.assertNotVisibleByDescription("Send")
            val probe = (client.driver as WasmAppTestDriver).socketProbe()
            probe.awaitOpen()
            probe.assertRoomWriteRejected(data.roomId, "forbidden-readonly-write", "User is read only")
            val topics =
                data.recipient.sessionManager.getRoomTopics(
                    data.roomId,
                    paginationQuery = PaginationQuery(),
                ).getOrThrow().data
            assertEquals(1, topics.size)
            assertEquals(text, (topics.single().content as TopicContent.Plain).plain)
        }
    }

fun AppiumTestBase<AppAppiumHelper, WasmAppiumHelper>.testNonmemberClientPermissions() =
    runAppiumBlockingTest {
        platformHelper.runClients(testName, targetHelper, setup = { ports ->
            val data = prepareClientRoom(ports, privateRoom = false, outsider = true)
            WasmClientsSetup(data, data.accounts.keys.associateWith { null }, data::close)
        }) { clients, data ->
            signInClientRoles(clients, data)
            val sender = clients.getValue("sender").driver
            sender.openClientRoom(data.roomName)
            val text = "public-nonmember-readable-${System.currentTimeMillis()}"
            sender.sendClientMessage(text)
            val outsider = clients.getValue("outsider").driver
            scenarioOpenCommunity(outsider, requireNotNull(data.communityName))
            outsider.clickByText("Rooms")
            outsider.clickByTextContaining(data.roomName)
            outsider.assertVisibleByText(text)
            outsider.assertNotVisibleByDescription("Send")
            val probe = (outsider as WasmAppTestDriver).socketProbe()
            probe.awaitOpen()
            probe.assertRoomWriteRejected(data.roomId, "forbidden-nonmember-write")
            val topics =
                data.sender.sessionManager.getRoomTopics(
                    data.roomId,
                    paginationQuery = PaginationQuery(),
                ).getOrThrow().data
            assertEquals(1, topics.size)
        }
    }

private fun AppiumTestBase<AppAppiumHelper, WasmAppiumHelper>.runRoomClients(
    privateRoom: Boolean,
    outsider: Boolean = false,
    scenario: suspend (Map<String, AppiumTestScope>, ClientRoomFixture) -> Unit,
) = runAppiumBlockingTest {
    platformHelper.runClients(testName, targetHelper, setup = { ports ->
        val data = prepareClientRoom(ports, privateRoom, outsider)
        WasmClientsSetup(
            data,
            data.accounts.keys.associateWith { null },
            data::close,
        )
    }) { clients, data ->
        signInClientRoles(clients, data)
        scenario(clients, data)
    }
}

internal suspend fun signInClientRoles(clients: Map<String, AppiumTestScope>, data: ClientRoomFixture) {
    clients.forEach { (role, client) ->
        val account = data.accounts.getValue(role)
        scenarioSignIn(client.driver, account.session.pemPrivateKey)
        client.driver.assertVisibleByTextContaining(account.session.address)
        client.driver.dismissDialog()
    }
}

private suspend fun exchangeRoomMessages(clients: Map<String, AppiumTestScope>, data: ClientRoomFixture) {
    val sender = clients.getValue("sender").driver
    val recipient = clients.getValue("recipient").driver
    sender.openClientRoom(data.roomName)
    recipient.clickByText("Rooms")
    val message = "peer-message-${System.currentTimeMillis()}"
    sender.sendClientMessage(message)
    recipient.assertVisibleByTextContaining(message)
    recipient.assertVisibleByText("1")
    awaitClientActivity(data.recipient, data.roomId, message, 1)
    recipient.clickByTextContaining(data.roomName)
    recipient.assertVisibleByText(message)
    awaitClientActivity(data.recipient, data.roomId, message, 0)
    sender.navigateBack()
    val reply = "peer-reply-${System.currentTimeMillis()}"
    recipient.sendClientMessage(reply)
    sender.assertVisibleByTextContaining(reply)
    sender.assertVisibleByText("1")
    awaitClientActivity(data.sender, data.roomId, reply, 1)
    sender.clickByTextContaining(data.roomName)
    sender.assertVisibleByText(reply)
    awaitClientActivity(data.sender, data.roomId, reply, 0)
    listOf("sender", "recipient").forEach { role ->
        val client = clients.getValue(role)
        client.restart()
        client.driver.openClientRoom(data.roomName)
        client.driver.assertVisibleByText(message)
        client.driver.assertVisibleByText(reply)
    }
    awaitClientActivity(data.sender, data.roomId, reply, 0)
    awaitClientActivity(data.recipient, data.roomId, reply, 0)
}

internal suspend fun AppTestDriver.openClientRoom(name: String) {
    clickByText("Rooms")
    clickByTextContaining(name)
}

private suspend fun AppTestDriver.sendClientMessage(message: String) {
    assertVisibleByDescription("Send")
    inputText(message)
    clickByDescription("Send")
    assertVisibleByText(message)
    dismissKeyboard()
}

private suspend fun awaitClientActivity(session: AuthenticatedSession, room: Long, text: String, unread: Long) {
    withTimeout(30_000) {
        while (true) {
            val activity =
                session.sessionManager.getUserRooms(PaginationQuery()).getOrThrow()
                    .activities.single { it.room.id == room }
            if (activity.previewText() == text && activity.unreadMessageCount == unread) return@withTimeout
            delay(500)
        }
    }
}

private fun RoomActivityInfo.previewText(): String? =
    when (val content = latestMessage?.content) {
        is TopicContent.Plain -> content.plain
        is TopicContent.Extracted -> content.plain
        else -> null
    }

internal class ClientRoomFixture(
    val accounts: Map<String, AuthenticatedSession>,
    val roomId: Long,
    val roomName: String,
    val communityName: String?,
) : AutoCloseable {
    val sender get() = accounts.getValue("sender")
    val recipient get() = accounts.getValue("recipient")

    override fun close() = accounts.values.forEach { it.sessionManager.client.close() }
}

internal suspend fun AppiumTestBase<AppAppiumHelper, WasmAppiumHelper>.prepareClientRoom(
    ports: AppiumPorts,
    privateRoom: Boolean,
    outsider: Boolean = false,
): ClientRoomFixture {
    val accounts = mutableListOf<AuthenticatedSession>()
    var retained = false
    try {
        repeat(2) { accounts.add(targetHelper.createAuthenticatedSession(ports)) }
        val sender = accounts[0]
        val peer = accounts[1]
        val suffix = (System.currentTimeMillis() % 1_000_000).toString().padStart(6, '0')
        val community =
            if (privateRoom) {
                null
            } else {
                sender.sessionManager.createCommunity(
                    NewCommunity("peers-$suffix", "pc$suffix"),
                ).getOrThrow().id
            }
        val name = "peers-room-$suffix"
        val room = sender.sessionManager.createRoom(NewRoom(name, "pr$suffix", communityId = community)).getOrThrow()
        if (community != null) peer.sessionManager.joinCommunity(community).getOrThrow()
        if (!outsider) {
            if (community == null) {
                sender.sessionManager.createTitle(
                    NewTitle(
                        "peer invitation",
                        TitleType.JOIN,
                        requireNotNull(peer.sessionManager.model.uid),
                        room.id,
                        ObjectType.ROOM,
                        "Authorized peer",
                    ),
                ).getOrThrow()
            }
            peer.sessionManager.joinRoom(room.id).getOrThrow()
        }
        val roles = mapOf("sender" to sender, (if (outsider) "outsider" else "recipient") to peer)
        val result = ClientRoomFixture(roles, room.id, name, if (community == null) null else "peers-$suffix")
        retained = true
        return result
    } finally {
        if (!retained) accounts.forEach { it.sessionManager.client.close() }
    }
}
