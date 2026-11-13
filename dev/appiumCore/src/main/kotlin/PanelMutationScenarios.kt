/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import com.storyteller_f.alphaheart.api.PaginationQuery
import com.storyteller_f.alphaheart.client.core.PanelSessionManager
import com.storyteller_f.alphaheart.client.core.ServerErrorException
import com.storyteller_f.alphaheart.client.core.UploadData
import com.storyteller_f.alphaheart.client.core.createTopic
import com.storyteller_f.alphaheart.client.core.getFileInfo
import com.storyteller_f.alphaheart.client.core.getRoomTopics
import com.storyteller_f.alphaheart.client.core.getTaskRecords
import com.storyteller_f.alphaheart.client.core.getUserInfo
import com.storyteller_f.alphaheart.client.core.getWorkerTasks
import com.storyteller_f.alphaheart.client.core.updateWorkerTask
import com.storyteller_f.alphaheart.client.core.upload
import com.storyteller_f.result.exceptionOrNull
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.model.TaskRecordType
import com.storyteller_f.shared.model.WorkerTask
import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.UserStatus
import com.storyteller_f.shared.utils.sha256
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.io.Buffer
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

fun AppiumTestBase<PanelAppiumHelper, WasmAppiumHelper>.testPanelTaskRetry() =
    withPanelUser { scope, data ->
        val user = data.user.sessionManager
        val uid = requireNotNull(user.model.uid)
        val query = requireNotNull(data.ports.queryFixtureDatabase)
        query(
            """
            INSERT INTO taskrecords
            (id, created_time, type, object_id, is_success, failure_type, failure_reason, is_retry_requested)
            VALUES ($uid, CURRENT_TIMESTAMP, 'INTRO', $uid, false,
            'DATA_ACCESS_FAILURE', 'Synthetic prior task failure', false)
            """.trimIndent(),
        )
        val driver = scope.driver as WasmAppTestDriver
        driver.openIntroRecords()
        driver.clickByText("Retry")
        driver.assertVisibleByText("Retry requested")
        val panel = data.panel.sessionManager
        val requested = panel.getTaskRecords(TaskRecordType.INTRO, PaginationQuery()).getOrThrow().data
        assertEquals(true, requested.single().isRetryRequested)
        scope.restart()
        val restarted = scope.driver as WasmAppTestDriver
        restarted.openIntroRecords()
        restarted.assertVisibleByText("Retry requested")
        panel.updateWorkerTask(WorkerTask(TaskRecordType.INTRO, true, 10, 1000)).getOrThrow()
        withTimeout(30_000) {
            while (true) {
                val records = panel.getTaskRecords(TaskRecordType.INTRO, PaginationQuery()).getOrThrow().data
                if (records.any { it.objectId == uid && it.isSuccess } &&
                    records.single { it.id == uid }.isRetryRequested == false
                ) {
                    break
                }
                delay(100)
            }
        }
        val room = query("SELECT notification_id FROM users WHERE id = $uid").toLong()
        val topics = user.getRoomTopics(room, paginationQuery = PaginationQuery()).getOrThrow().data
        assertEquals(1, topics.size, "The requested retry must produce the welcome message exactly once")
    }

private suspend fun WasmAppTestDriver.openIntroRecords() {
    clickByDescription("Menu")
    scrollToText("Worker records")
    clickByText("Worker records")
    clickByText("INTRO")
}

fun AppiumTestBase<PanelAppiumHelper, WasmAppiumHelper>.testPanelFileStatus() =
    withPanelUser { scope, data ->
        val user = data.user.sessionManager
        val uid = requireNotNull(user.model.uid)
        val bytes = "panel-managed-file-content".encodeToByteArray()
        val name = "panel-status.txt"
        val upload =
            UploadData(
                bytes.size.toLong(),
                name,
                ContentType.Text.Plain,
                sha256(Buffer().apply { write(bytes) }),
            ) {
                Buffer().apply { write(bytes) }
            }
        val file = user.upload(ObjectTuple(uid, ObjectType.USER), upload).getOrThrow().data.single()
        val driver = scope.driver as WasmAppTestDriver
        driver.openManagedFile(name)
        driver.scrollToText("Set to ReadOnly")
        driver.clickByText("Set to ReadOnly")
        driver.assertVisibleByText("Set to Writable")
        assertEquals(true, user.getFileInfo(file.id).getOrThrow().readOnly)
        assertContentEquals(bytes, user.client.get(file.url).body<ByteArray>())
        scope.restart()
        val restarted = scope.driver as WasmAppTestDriver
        restarted.openManagedFile(name)
        restarted.scrollToText("Set to Writable")
        restarted.clickByText("Set to Writable")
        restarted.assertVisibleByText("Set to ReadOnly")
        val restored = user.getFileInfo(file.id).getOrThrow()
        assertEquals(false, restored.readOnly)
        assertEquals(uid, restored.owner)
        assertEquals(name, restored.name)
        assertContentEquals(bytes, user.client.get(restored.url).body<ByteArray>())
    }

private suspend fun WasmAppTestDriver.openManagedFile(name: String) {
    clickByDescription("Menu")
    scrollToText("All files")
    clickByText("All files")
    clickByText(name)
}

fun AppiumTestBase<PanelAppiumHelper, WasmAppiumHelper>.testPanelUserStatus() =
    withPanelUser { scope, data ->
        val driver = scope.driver
        scenarioOpenAllUsersFromOverview(driver, expandedNavigation = false)
        driver.clickByText(data.user.session.address)
        driver.clickByText("Set to ReadOnly")
        val user = data.user.sessionManager
        val uid = requireNotNull(user.model.uid)
        driver.assertVisibleByText("Set to Normal")
        assertEquals(UserStatus.READ_ONLY, user.getUserInfo(uid).getOrThrow().status)
        val failure = user.createTopic(ObjectType.USER, uid, "forbidden-panel-status-write").exceptionOrNull()
        assertEquals(HttpStatusCode.Forbidden, assertIs<ServerErrorException>(failure).status)
        driver.clickByText("Set to Normal")
        driver.assertVisibleByText("Set to ReadOnly")
        assertEquals(UserStatus.NORMAL, user.getUserInfo(uid).getOrThrow().status)
        val topic = user.createTopic(ObjectType.USER, uid, "restored-panel-status-write").getOrThrow()
        assertEquals(uid, topic.author)
    }

fun AppiumTestBase<PanelAppiumHelper, WasmAppiumHelper>.testPanelWorkerConfiguration() =
    withPanelUser { scope, data ->
        val panel = data.panel.sessionManager
        val driver = scope.driver as WasmAppTestDriver
        driver.openWorkerConfiguration()
        driver.scrollToDescription("worker INTRO fetch size")
        driver.inputTextByDescription("worker INTRO fetch size", "17")
        driver.scrollToDescription("worker INTRO wait duration")
        driver.inputTextByDescription("worker INTRO wait duration", "1000")
        driver.scrollToDescription("worker INTRO enabled false", upward = true)
        driver.clickByDescription("worker INTRO enabled false")
        driver.scrollToDescription("save worker INTRO")
        driver.clickByDescription("save worker INTRO")
        val expected = WorkerTask(TaskRecordType.INTRO, true, 17, 1000)
        panel.awaitWorkerConfiguration(expected)
        val uid = requireNotNull(data.user.sessionManager.model.uid)
        val query = requireNotNull(data.ports.queryFixtureDatabase)
        val room = query("SELECT notification_id FROM users WHERE id = $uid").toLong()
        withTimeout(30_000) {
            while (data.user.sessionManager.getRoomTopics(
                    room,
                    paginationQuery = PaginationQuery(),
                ).getOrNull()?.data?.size != 1
            ) {
                delay(100)
            }
        }
        withTimeout(30_000) {
            while (true) {
                val records =
                    panel.getTaskRecords(
                        TaskRecordType.INTRO,
                        PaginationQuery(),
                        isSuccess = true,
                    ).getOrThrow().data
                if (records.any { it.objectId == uid }) break
                delay(100)
            }
        }
        scope.restart()
        val restarted = scope.driver as WasmAppTestDriver
        restarted.openWorkerConfiguration()
        restarted.scrollToDescription("worker INTRO enabled true")
        restarted.assertVisibleByDescription("worker INTRO enabled true")
        restarted.scrollToDescription("worker INTRO fetch size")
        restarted.assertInputByDescription("worker INTRO fetch size", "17")
        restarted.scrollToDescription("worker INTRO wait duration")
        restarted.assertInputByDescription("worker INTRO wait duration", "1000")
        assertEquals(
            expected,
            panel.getWorkerTasks().getOrThrow().data.single {
                it.type == TaskRecordType.INTRO
            },
        )
    }

private suspend fun PanelSessionManager.awaitWorkerConfiguration(expected: WorkerTask) =
    withTimeout(30_000) {
        while (getWorkerTasks().getOrThrow().data.single { it.type == expected.type } != expected) {
            delay(100)
        }
    }

private class PanelUserFixture(
    val panel: PanelAuthenticatedSession,
    val user: AuthenticatedSession,
    val ports: AppiumPorts,
) : AutoCloseable {
    override fun close() {
        panel.sessionManager.client.close()
        user.sessionManager.client.close()
    }
}

private fun AppiumTestBase<PanelAppiumHelper, WasmAppiumHelper>.withPanelUser(
    scenario: suspend (AppiumTestScope, PanelUserFixture) -> Unit,
) = runAppiumBlockingTest {
    platformHelper.runClients(testName, targetHelper, setup = { ports ->
        val panel = targetHelper.createAuthenticatedSession(ports)
        var retained = false
        try {
            val user = AppAppiumHelper().createAuthenticatedSession(ports)
            val data = PanelUserFixture(panel, user, ports)
            try {
                panel.sessionManager.updateWorkerTask(WorkerTask(TaskRecordType.INTRO, false, 10, 1000)).getOrThrow()
                val setup = WasmClientsSetup(data, mapOf("panel" to null), data::close)
                retained = true
                setup
            } finally {
                if (!retained) user.sessionManager.client.close()
            }
        } finally {
            if (!retained) panel.sessionManager.client.close()
        }
    }) { clients, data ->
        val scope = clients.getValue("panel")
        scenarioPanelSignin(scope.driver, data.panel.session.pemPrivateKey)
        scenario(scope, data)
    }
}

private suspend fun AppTestDriver.openWorkerConfiguration() {
    clickByDescription("Menu")
    (this as WasmAppTestDriver).scrollToText("Worker task configurations")
    clickByText("Worker task configurations")
}
