/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import com.storyteller_f.alphaheart.client.core.getFileList
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.type.ObjectType
import com.storyteller_f.shared.utils.generateObjectMarkdownContent
import io.ktor.client.call.body
import io.ktor.client.request.get
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.assertEquals
import kotlin.test.assertTrue

fun AppiumTestBase<AppAppiumHelper, WasmAppiumHelper>.testRecordedAudioUpload(privateRoom: Boolean = false) =
    withRecorderClient(privateRoom = privateRoom) { driver, probe, data ->
        probe.observeRecordingSamples()
        driver.openRecorder(data.roomName)
        driver.clickByText("start record")
        driver.assertVisibleByText("stop record")
        probe.awaitCaptureStarted(minimum = 1)
        probe.awaitRecordingSamples()
        driver.clickByText("stop record")
        driver.assertInputContains(".wav")
        driver.assertVisibleByText("start record")
        probe.awaitCaptureReleased(minimum = 1)
        val session = data.sender.sessionManager
        val owner = data.recordingOwner()
        val file = session.getFileList(owner.objectId, owner.objectType, null, 10).getOrThrow().data.single()
        assertEquals(owner.objectId, file.owner)
        assertEquals(owner.objectType, file.ownerType)
        assertEquals("audio/vnd.wave", file.contentType)
        driver.assertInputText("\n" + generateObjectMarkdownContent(file))
        val bytes = session.client.get(file.url).body<ByteArray>()
        assertEquals(file.size, bytes.size.toLong())
        assertTrue(bytes.size > 44)
        assertEquals("RIFF", bytes.decodeToString(0, 4))
        assertEquals("WAVE", bytes.decodeToString(8, 12))
        assertTrue(ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt(24) > 0)
        assertEquals(bytes.size - 44, ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt(40))
        assertTrue(bytes.drop(44).any { it != 0.toByte() }, "Synthesized tone must reach the persisted WAV")
    }

fun AppiumTestBase<AppAppiumHelper, WasmAppiumHelper>.testRecordedAudioCancel() =
    withRecorderClient { driver, probe, data ->
        probe.observeRecordingSamples()
        driver.openRecorder(data.roomName)
        driver.clickByText("start record")
        driver.assertVisibleByText("stop record")
        probe.awaitRecordingSamples()
        driver.dismissDialog()
        driver.assertNotVisibleByText("stop record")
        probe.awaitCaptureReleased(minimum = 1)
        data.assertNoUploadedFiles()
    }

fun AppiumTestBase<AppAppiumHelper, WasmAppiumHelper>.testRecordingPermissionRecovery() =
    withRecorderClient { driver, probe, data ->
        probe.rejectNextCapture()
        driver.openRecorder(data.roomName)
        driver.clickByText("start record")
        driver.assertVisibleByTextContaining("Synthetic microphone permission denied")
        driver.dismissDialog()
        driver.assertVisibleByText("start record")
        driver.clickByText("start record")
        driver.assertVisibleByText("stop record")
        probe.awaitCaptureStarted(minimum = 1)
        driver.dismissDialog()
        probe.awaitCaptureReleased(minimum = 1)
        data.assertNoUploadedFiles()
    }

private suspend fun WasmAppTestDriver.openRecorder(roomName: String) {
    openClientRoom(roomName)
    clickByDescription("Add attachment")
    clickByText("audio recorder")
    assertVisibleByText("start record")
}

private fun AppiumTestBase<AppAppiumHelper, WasmAppiumHelper>.withRecorderClient(
    privateRoom: Boolean = false,
    block: suspend (WasmAppTestDriver, WasmMediaProbe, ClientRoomFixture) -> Unit,
) = runAppiumBlockingTest {
    platformHelper.runClients(testName, targetHelper, syntheticMedia = true, setup = { ports ->
        val data = prepareClientRoom(ports, privateRoom = privateRoom)
        WasmClientsSetup(data, mapOf("sender" to null), data::close)
    }) { clients, data ->
        val driver = clients.getValue("sender").driver as WasmAppTestDriver
        scenarioSignIn(driver, data.sender.session.pemPrivateKey)
        val probe = driver.mediaProbe()
        probe.observeCapture()
        block(driver, probe, data)
    }
}

private suspend fun ClientRoomFixture.assertNoUploadedFiles() {
    val owner = recordingOwner()
    val files = sender.sessionManager.getFileList(owner.objectId, owner.objectType, null, 10).getOrThrow()
    assertTrue(files.data.isEmpty())
}

private fun ClientRoomFixture.recordingOwner(): ObjectTuple =
    if (communityName == null) {
        ObjectTuple(roomId, ObjectType.ROOM)
    } else {
        ObjectTuple(requireNotNull(sender.sessionManager.model.uid), ObjectType.USER)
    }
