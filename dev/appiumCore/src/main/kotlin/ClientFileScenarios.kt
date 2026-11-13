/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import com.storyteller_f.alphaheart.client.core.getChunkStatus
import com.storyteller_f.alphaheart.client.core.getFileList
import com.storyteller_f.alphaheart.client.core.getMediaByName
import com.storyteller_f.alphaheart.client.core.getQuotaInfo
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.type.UploadRecordStatus
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

private const val RESUMABLE_FILE_SIZE = 12 * 1024 * 1024
private const val FIRST_CHUNK_SIZE = 10 * 1024 * 1024

fun AppiumTestBase<AppAppiumHelper, WasmAppiumHelper>.testClientUploadResume() =
    withSignedInUser { scope, user, _ ->
        val driver = scope.driver as WasmAppTestDriver
        val gate = driver.uploadGate()
        gate.install()
        val name = "ui-resumable.bin"
        val bytes = ByteArray(RESUMABLE_FILE_SIZE) { (it % 251).toByte() }
        driver.openUploadPicker()
        driver.selectUploadFile(name, bytes)
        val record = gate.awaitSecondChunk()
        assertEquals(listOf(0), user.sessionManager.getChunkStatus(record).getOrThrow().uploaded)
        driver.assertVisibleByDescription("upload progress $name: $FIRST_CHUNK_SIZE/$RESUMABLE_FILE_SIZE")
        driver.clickByDescription("pause upload $name")
        driver.assertVisibleByText("$name, PAUSED")
        gate.release()
        driver.clickByDescription("resume upload $name")
        driver.assertVisibleByText("$name, SUCCESS")
        gate.assertEachChunkSentOnce()
        val uid = requireNotNull(user.sessionManager.model.uid)
        val file = user.sessionManager.getMediaByName(name, uid, ObjectType.USER).getOrThrow()
        assertContentEquals(bytes, user.sessionManager.client.get(file.url).body<ByteArray>())
        val quota = user.sessionManager.getQuotaInfo(ObjectTuple(uid, ObjectType.USER)).getOrThrow()
        assertEquals(RESUMABLE_FILE_SIZE.toLong(), quota.used)
        assertEquals(null, quota.lockId)
    }

fun AppiumTestBase<AppAppiumHelper, WasmAppiumHelper>.testClientUploadCancel() =
    withSignedInUser { scope, user, _ ->
        val driver = scope.driver as WasmAppTestDriver
        val gate = driver.uploadGate()
        gate.install()
        val name = "ui-cancelled.bin"
        driver.openUploadPicker()
        driver.selectUploadFile(name, ByteArray(RESUMABLE_FILE_SIZE) { (it % 251).toByte() })
        val record = gate.awaitSecondChunk()
        driver.clickByDescription("pause upload $name")
        driver.assertVisibleByText("$name, PAUSED")
        driver.openContextMenuByText("$name, PAUSED")
        driver.clickByText("Abort Upload")
        driver.assertNotVisibleByText("$name, PAUSED")
        gate.release()
        val session = user.sessionManager
        assertEquals(UploadRecordStatus.ABORTED, session.getChunkStatus(record).getOrThrow().status)
        val uid = requireNotNull(session.model.uid)
        val quota = session.getQuotaInfo(ObjectTuple(uid, ObjectType.USER)).getOrThrow()
        assertEquals(null, quota.lockId)
        assertEquals(0L, quota.used)
        val files = session.getFileList(uid, ObjectType.USER, null, 10).getOrThrow().data
        assertEquals(0, files.size)
        scope.restart()
        val restarted = scope.driver as WasmAppTestDriver
        restarted.clickByDescription("avatar")
        restarted.clickByText("File Explorer")
        restarted.clickByText("Upload")
        restarted.assertNotVisibleByTextContaining(name)
    }

fun AppiumTestBase<AppAppiumHelper, WasmAppiumHelper>.testClientFileUpload() =
    withSignedInUser { scope, user, _ ->
        val driver = scope.driver as WasmAppTestDriver
        val expected = ByteArray(8192) { (it % 251).toByte() }
        val name = "ui-upload.bin"
        driver.openUploadPicker()
        driver.selectUploadFile(name, expected)
        driver.assertVisibleByText("$name, SUCCESS")
        val uid = requireNotNull(user.sessionManager.model.uid)
        val file = user.sessionManager.getMediaByName(name, uid, ObjectType.USER).getOrThrow()
        assertEquals(name, file.name)
        assertEquals(expected.size.toLong(), file.size)
        assertEquals(uid, file.owner)
        assertEquals(ObjectType.USER, file.ownerType)
        val stored = user.sessionManager.client.get(file.url).body<ByteArray>()
        assertContentEquals(expected, stored, "UI-selected file bytes must reach real filesystem storage")
        scope.restart()
        val restarted = scope.driver
        restarted.clickByDescription("avatar")
        restarted.clickByText("File Explorer")
        restarted.clickByText("Upload")
        restarted.assertVisibleByText("$name, SUCCESS")
    }

fun AppiumTestBase<AppAppiumHelper, WasmAppiumHelper>.testClientFileQuota() =
    withSignedInUser { scope, user, ports ->
        val uid = requireNotNull(user.sessionManager.model.uid)
        val target = ObjectTuple(uid, ObjectType.USER)
        val before = user.sessionManager.getQuotaInfo(target).getOrThrow()
        assertEquals(0L, before.used)
        val query = requireNotNull(ports.queryFixtureDatabase)
        query("UPDATE quotas SET total = 1024 WHERE owner_id = $uid AND owner_type = 'USER' AND quota_type = 'FILE'")
        assertEquals(1024L, user.sessionManager.getQuotaInfo(target).getOrThrow().total)
        val driver = scope.driver as WasmAppTestDriver
        val name = "ui-quota-rejected.bin"
        driver.openUploadPicker()
        driver.selectUploadFile(name, ByteArray(8192) { (it % 251).toByte() })
        driver.assertVisibleByText("$name, FAILED")
        driver.assertNotVisibleByText("$name, SUCCESS")
        assertEquals(0L, user.sessionManager.getQuotaInfo(target).getOrThrow().used)
        val files = user.sessionManager.getFileList(uid, ObjectType.USER, null, 10).getOrThrow().data
        assertEquals(0, files.size)
    }

private suspend fun WasmAppTestDriver.openUploadPicker() {
    clickByDescription("avatar")
    clickByText("File Explorer")
    clickByText("Upload")
    prepareUploadPicker()
    clickByDescription("upload file")
}
