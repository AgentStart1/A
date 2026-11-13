/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

fun AppiumTestBase<AppAppiumHelper, WasmAppiumHelper>.testTwoClientRtc() =
    runAppiumBlockingTest {
        platformHelper.runClients(testName, targetHelper, syntheticMedia = true, setup = { ports ->
            val data = prepareClientRoom(ports, privateRoom = false)
            WasmClientsSetup(data, data.accounts.keys.associateWith { null }, data::close)
        }) { clients, data ->
            signInClientRoles(clients, data)
            val sender = clients.getValue("sender").driver as WasmAppTestDriver
            val recipient = clients.getValue("recipient").driver as WasmAppTestDriver
            val senderProbe = sender.rtcProbe()
            val recipientProbe = recipient.rtcProbe()
            senderProbe.observeCapture()
            recipientProbe.observeCapture()
            sender.joinClientCall(data.roomName)
            recipient.joinClientCall(data.roomName)
            senderProbe.awaitCaptureStarted()
            recipientProbe.awaitCaptureStarted()
            val senderId = requireNotNull(data.sender.sessionManager.model.uid)
            val recipientId = requireNotNull(data.recipient.sessionManager.model.uid)
            sender.assertVisibleByTextContaining("Participant $recipientId, Connected")
            recipient.assertVisibleByTextContaining("Participant $senderId, Connected")
            senderProbe.awaitPresentedRemoteFrames()
            recipientProbe.awaitPresentedRemoteFrames()
            sender.clickByDescription("rtc-toggle-audio")
            recipient.assertVisibleByTextContaining("Participant $senderId, Connected, audioMuted=true")
            sender.clickByDescription("rtc-toggle-audio")
            recipient.assertVisibleByTextContaining("Participant $senderId, Connected, audioMuted=false")
            sender.clickByDescription("rtc-toggle-video")
            recipient.assertVisibleByTextContaining(
                "Participant $senderId, Connected, audioMuted=false, videoMuted=true",
            )
            sender.clickByDescription("rtc-toggle-video")
            recipient.assertVisibleByTextContaining(
                "Participant $senderId, Connected, audioMuted=false, videoMuted=false",
            )
            recipientProbe.awaitPresentedRemoteFrames()
            sender.clickByDescription("rtc-leave")
            sender.assertVisibleByDescription("Send")
            senderProbe.awaitCaptureReleased()
            recipient.assertNotVisibleByText("Participant $senderId, Connected, audioMuted=false, videoMuted=false")
            recipient.clickByDescription("rtc-leave")
            recipient.assertVisibleByDescription("Send")
            recipientProbe.awaitCaptureReleased()
        }
    }

private suspend fun AppTestDriver.joinClientCall(roomName: String) {
    openClientRoom(roomName)
    clickByDescription("room details")
    clickByText("Start Call")
    clickByDescription("rtc-join-video")
    assertVisibleByDescription("rtc-leave")
}

fun AppiumTestBase<AppAppiumHelper, WasmAppiumHelper>.testCancelRtcBeforeJoining() =
    withSingleRtcClient { driver, probe, roomName ->
        driver.openClientRoom(roomName)
        driver.clickByDescription("room details")
        driver.clickByText("Start Call")
        driver.assertVisibleByDescription("rtc-join-video")
        driver.clickByDescription("rtc-back")
        driver.assertVisibleByDescription("Send")
        probe.assertCaptureNotRequested()
    }

fun AppiumTestBase<AppAppiumHelper, WasmAppiumHelper>.testCancelRtcDuringCapture() =
    withSingleRtcClient(holdCapture = true) { driver, probe, roomName ->
        driver.openClientRoom(roomName)
        driver.clickByDescription("room details")
        driver.clickByText("Start Call")
        driver.clickByDescription("rtc-join-video")
        probe.awaitCaptureStarted(minimum = 1)
        driver.clickByDescription("rtc-leave")
        driver.assertVisibleByDescription("Send")
        probe.releaseCapture()
        probe.awaitCaptureReleased(minimum = 1)
        driver.assertNotVisibleByDescription("rtc-leave")
    }

private fun AppiumTestBase<AppAppiumHelper, WasmAppiumHelper>.withSingleRtcClient(
    holdCapture: Boolean = false,
    block: suspend (WasmAppTestDriver, WasmRtcProbe, String) -> Unit,
) = runAppiumBlockingTest {
    platformHelper.runClients(testName, targetHelper, syntheticMedia = true, setup = { ports ->
        val data = prepareClientRoom(ports, privateRoom = false)
        WasmClientsSetup(data, mapOf("sender" to null), data::close)
    }) { clients, data ->
        val driver = clients.getValue("sender").driver as WasmAppTestDriver
        scenarioSignIn(driver, data.sender.session.pemPrivateKey)
        val probe = driver.rtcProbe()
        probe.observeCapture(holdCapture)
        block(driver, probe, data.roomName)
    }
}
