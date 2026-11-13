/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import com.storyteller_f.alphaheart.api.PaginationQuery
import com.storyteller_f.alphaheart.client.core.createTopic
import com.storyteller_f.alphaheart.client.core.getRoomInfo
import com.storyteller_f.alphaheart.client.core.getRoomTopics
import com.storyteller_f.alphaheart.client.core.updateWorkerTask
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.model.TaskRecordType
import com.storyteller_f.shared.model.WorkerTask
import com.storyteller_f.shared.type.ObjectType
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlin.test.assertEquals
import kotlin.test.assertFalse

fun AppiumTestBase<AppAppiumHelper, WasmAppiumHelper>.testNotificationPermissions() =
    runAppiumBlockingTest {
        platformHelper.runClients(
            testName,
            targetHelper,
            setup = { prepareNotificationRecipient(it) },
        ) { clients, (user, room) ->
            val scope = clients.getValue("recipient")
            val driver = scope.driver as WasmAppTestDriver
            scenarioSignIn(driver, user.session.pemPrivateKey)
            driver.clickByTextContaining(user.session.address)
            driver.clickByDescription("Notification, room reference $room")
            val model = user.sessionManager.model
            val nickname = requireNotNull(model.userHandler.data.value).nickname
            driver.assertVisibleByText("Hello, $nickname")
            driver.assertNotVisibleByDescription("Send")
            val probe = driver.socketProbe()
            probe.awaitOpen()
            probe.assertRoomWriteRejected(room, "forbidden-notification-reply")
            val topics = user.sessionManager.getRoomTopics(room, paginationQuery = PaginationQuery()).getOrThrow().data
            assertEquals(1, topics.size)
            scope.restart()
            val restarted = scope.driver as WasmAppTestDriver
            restarted.awaitAuthenticatedSession()
            restarted.clickByDescription("avatar")
            restarted.assertVisibleByTextContaining(user.session.address)
        }
    }

private suspend fun AppiumTestBase<AppAppiumHelper, WasmAppiumHelper>.prepareNotificationRecipient(
    ports: AppiumPorts,
): WasmClientsSetup<Pair<AuthenticatedSession, Long>> {
    val user = targetHelper.createAuthenticatedSession(ports)
    var retained = false
    try {
        val query = requireNotNull(ports.queryFixtureDatabase)
        val uid = requireNotNull(user.sessionManager.model.uid)
        val room = query("SELECT notification_id FROM users WHERE id = $uid").toLong()
        val admin = PanelAppiumHelper().createAuthenticatedSession(ports)
        try {
            admin.sessionManager.updateWorkerTask(WorkerTask(TaskRecordType.INTRO, true, 100, 100)).getOrThrow()
            awaitNotificationMessage(user, room)
        } finally {
            admin.sessionManager.client.close()
        }
        assertFalse(user.sessionManager.getRoomInfo(room).getOrThrow().isJoined)
        assertEquals(
            "INVITED:true",
            query(
                "SELECT status || ':' || (joined_time IS NULL)::text FROM members " +
                    "WHERE uid = $uid AND object_id = $room AND object_type = 'ROOM'",
            ),
        )
        val reference =
            """
            notification-entry

            ```csa
            /room/$room
            ```
            """.trimIndent()
        user.sessionManager.createTopic(ObjectType.USER, uid, reference).getOrThrow()
        val prepared = WasmClientsSetup(user to room, mapOf("recipient" to null)) { user.sessionManager.client.close() }
        retained = true
        return prepared
    } finally {
        if (!retained) user.sessionManager.client.close()
    }
}

private suspend fun awaitNotificationMessage(user: AuthenticatedSession, room: Long) =
    withTimeout(30_000) {
        while (true) {
            val topics = user.sessionManager.getRoomTopics(room, paginationQuery = PaginationQuery()).getOrNull()?.data
            if (topics?.size == 1) return@withTimeout
            delay(100)
        }
    }
