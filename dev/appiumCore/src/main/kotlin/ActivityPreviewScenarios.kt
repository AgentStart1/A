/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import com.storyteller_f.alphaheart.api.NewCommunity
import com.storyteller_f.alphaheart.api.NewRoom
import com.storyteller_f.alphaheart.api.PaginationQuery
import com.storyteller_f.alphaheart.client.core.addReadLog
import com.storyteller_f.alphaheart.client.core.createCommunity
import com.storyteller_f.alphaheart.client.core.createRoom
import com.storyteller_f.alphaheart.client.core.createTopic
import com.storyteller_f.alphaheart.client.core.getRoomMembersPublicKeys
import com.storyteller_f.alphaheart.client.core.getUserRooms
import com.storyteller_f.alphaheart.client.core.joinCommunity
import com.storyteller_f.alphaheart.client.core.joinRoom
import com.storyteller_f.alphaheart.client.core.pinTopic
import com.storyteller_f.alphaheart.client.core.sendMessage
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.model.TopicContent
import com.storyteller_f.shared.obj.ObjectTuple
import com.storyteller_f.shared.obj.UpdateUserRead
import com.storyteller_f.shared.type.ObjectType
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.milliseconds

private data class ActivityScenario(
    val viewer: AuthenticatedSession,
    val roomId: Long,
    val roomName: String,
    val latestMessage: String,
    val pinnedTopic: String?,
)

fun AppiumTestBase<AppAppiumHelper, PlatformAppiumHelper>.testActivityReadByHelper(privateRoom: Boolean = false) =
    runActivityScenario(privateRoom, scenario = ::scenarioReadActivity)

fun AppiumTestBase<AppAppiumHelper, PlatformAppiumHelper>.testActivitySendByHelper(privateRoom: Boolean = false) =
    runActivityScenario(privateRoom, scenario = ::scenarioSendActivity)

fun AppiumTestBase<AppAppiumHelper, PlatformAppiumHelper>.testActivityNavigationByHelper() =
    runActivityScenario { driver, data ->
        driver.clickByText("Rooms")
        scenarioRevisitActivityRoom(driver, data)
    }

fun AppiumTestBase<AppAppiumHelper, PlatformAppiumHelper>.testActivityLayoutByHelper() =
    runActivityScenario { driver, data ->
        driver.clickByText("Rooms")
        scenarioResizeActivityRooms(driver, data, activityCompactWidth(driver))
    }

fun AppiumTestBase<AppAppiumHelper, PlatformAppiumHelper>.testActivityContextByHelper() =
    runActivityScenario(includeContext = true) { driver, data ->
        if (driver is AndroidAppTestDriver) {
            driver.withWideViewport { scenarioOpenActivityContext(driver, data) }
        } else {
            driver.resizeActivityViewport(1600, 1000)
            scenarioOpenActivityContext(driver, data)
        }
    }

fun AppiumTestBase<AppAppiumHelper, PlatformAppiumHelper>.testActivityDraftByHelper() =
    runActivityScenario { driver, data ->
        driver.clickByText("Rooms")
        driver.clickByTextContaining(data.roomName)
        driver.assertVisibleByDescription("Send")
        val draft = "activity-draft-${System.currentTimeMillis()}"
        driver.inputText(draft)
        listOf(activityCompactWidth(driver) to 800, 900 to 800, 1600 to 500, 1600 to 1000).forEach { (width, height) ->
            driver.resizeActivityViewport(width, height)
            if (driver is WasmAppTestDriver) driver.assertInputText(draft) else driver.assertVisibleByText(draft)
            driver.assertVisibleByDescription("Send")
            driver.saveSnapshot("activity-draft-$width-$height")
        }
    }

private fun AppiumTestBase<AppAppiumHelper, PlatformAppiumHelper>.runActivityScenario(
    privateRoom: Boolean = false,
    includeContext: Boolean = false,
    scenario: suspend (AppTestDriver, ActivityScenario) -> Unit,
) = runAppiumBlockingTest {
    platformHelper.runTest(
        testName = testName,
        target = targetHelper,
        setup = { ports ->
            val data =
                if (privateRoom) {
                    preparePrivateActivityScenario { targetHelper.createAuthenticatedSession(ports) }
                } else {
                    prepareActivityScenario(includeContext) { targetHelper.createAuthenticatedSession(ports) }
                }
            AppiumTestSetup(data)
        },
    ) { scope, data ->
        try {
            scenarioSignIn(scope.driver, data.viewer.session.pemPrivateKey)
            scope.driver.dismissDialog()
            scenario(scope.driver, data)
        } catch (cancelled: kotlin.coroutines.cancellation.CancellationException) {
            throw cancelled
        } catch (failure: IllegalStateException) {
            scope.driver.saveSnapshot("activity-failed")
            throw failure
        } finally {
            data.viewer.sessionManager.client.close()
        }
    }
}

private suspend fun prepareActivityScenario(
    includeContext: Boolean = false,
    createSession: suspend () -> AuthenticatedSession,
): ActivityScenario {
    val owner = createSession()
    try {
        val viewer = createSession()
        var retained = false
        try {
            val suffix = (System.currentTimeMillis() % 1_000_000).toString().padStart(6, '0')
            val communityId =
                owner.sessionManager.createCommunity(NewCommunity("activity-$suffix", "ac$suffix"))
                    .getOrThrow().id
            val roomName = "activity-room-$suffix"
            val roomId =
                owner.sessionManager.createRoom(
                    NewRoom(roomName, "ar$suffix", communityId = communityId),
                )
                    .getOrThrow().id
            viewer.sessionManager.joinCommunity(communityId).getOrThrow()
            viewer.sessionManager.joinRoom(roomId).getOrThrow()
            val pinnedTopic =
                if (includeContext) {
                    val content = "activity-pinned-$suffix"
                    val topicId =
                        owner.sessionManager.createTopic(ObjectType.COMMUNITY, communityId, content)
                            .getOrThrow().id
                    owner.sessionManager.pinTopic(topicId).getOrThrow()
                    content
                } else {
                    null
                }
            val messages = (1..3).map { "activity-message-$suffix-$it" }
            sendActivityMessages(owner, roomId, messages)
            val scenario = ActivityScenario(viewer, roomId, roomName, messages.last(), pinnedTopic)
            retained = true
            return scenario
        } finally {
            if (!retained) viewer.sessionManager.client.close()
        }
    } finally {
        owner.sessionManager.client.close()
    }
}

private fun activityCompactWidth(driver: AppTestDriver): Int = if (driver is WasmAppTestDriver) 504 else 480

private suspend fun scenarioReadActivity(driver: AppTestDriver, data: ActivityScenario) {
    driver.clickByText("Rooms")
    driver.assertVisibleByTextContaining(data.latestMessage)
    driver.assertVisibleByText("3")
    driver.assertActivityUnreadIndicator(true)
    driver.clickByTextContaining(data.roomName)
    driver.assertVisibleByDescription("Send")
    driver.assertVisibleByText(data.latestMessage)
    waitUntilRoomRead(data, data.latestMessage)
    driver.dismissKeyboard()
    driver.navigateBack()
    driver.assertVisibleByTextContaining(data.latestMessage)
    driver.assertNotVisibleByText("3")
    driver.assertActivityUnreadIndicator(false)
}

private suspend fun scenarioSendActivity(driver: AppTestDriver, data: ActivityScenario) {
    driver.clickByText("Rooms")
    driver.clickByTextContaining(data.roomName)
    driver.assertVisibleByDescription("Send")
    val sentMessage = "activity-sent-${System.currentTimeMillis()}"
    driver.inputText(sentMessage)
    driver.clickByDescription("Send")
    driver.assertVisibleByText(sentMessage)
    waitUntilRoomRead(data, sentMessage)
    driver.dismissKeyboard()
    driver.navigateBack()
    driver.assertVisibleByTextContaining(sentMessage)
    driver.assertActivityUnreadIndicator(false)
    driver.saveSnapshot("activity-after-send")
}

private suspend fun waitUntilRoomRead(data: ActivityScenario, expectedMessage: String) {
    val deadline = System.currentTimeMillis() + java.time.Duration.ofSeconds(30).toMillis()
    while (System.currentTimeMillis() < deadline) {
        val activity =
            data.viewer.sessionManager.getUserRooms(PaginationQuery()).getOrThrow()
                .activities.single { it.room.id == data.roomId }
        val latestMessage =
            when (val content = activity.latestMessage?.content) {
                is TopicContent.Plain -> content.plain
                is TopicContent.Extracted -> content.plain
                else -> null
            }
        // An optimistic UI echo can precede persistence; zero unread on the old message is insufficient.
        if (latestMessage == expectedMessage && activity.unreadMessageCount == 0L) return
        delay(500.milliseconds)
    }
    error("Expected latest room message to be persisted and read")
}

private suspend fun sendActivityMessages(
    owner: AuthenticatedSession,
    roomId: Long,
    messages: List<String>,
    privateRoom: Boolean = false,
) = coroutineScope {
    val keys =
        if (privateRoom) {
            owner.sessionManager.getRoomMembersPublicKeys(roomId, PaginationQuery()).getOrThrow()
                .data.toList()
        } else {
            emptyList()
        }
    val socket = owner.sessionManager.webSocketClient
    val connection = launch { socket.connectWebSocket() }
    try {
        withTimeout(30_000) {
            socket.connectionHandler.data.filterNotNull().first()
            socket.useWebSocket {
                messages.forEach { sendMessage(ObjectTuple(roomId, ObjectType.ROOM), privateRoom, it, keys) }
            }.getOrThrow()
            while (owner.sessionManager.getUserRooms(PaginationQuery()).getOrThrow()
                    .activities.single { it.room.id == roomId }.unreadMessageCount != messages.size.toLong()
            ) {
                delay(500.milliseconds)
            }
        }
    } finally {
        connection.cancelAndJoin()
    }
}

private suspend fun scenarioOpenActivityContext(driver: AppTestDriver, data: ActivityScenario) {
    val pinnedTopic = requireNotNull(data.pinnedTopic)
    driver.clickByText("World")
    driver.assertVisibleByText("Unread rooms")
    driver.assertVisibleByText("Pinned topics")
    driver.clickByTextContaining(data.roomName)
    driver.assertVisibleByDescription("Send")
    driver.assertVisibleByText(data.latestMessage)
    waitUntilRoomRead(data, data.latestMessage)
    driver.navigateBack()
    driver.assertVisibleByText("Unread rooms")
    driver.assertVisibleByTextContaining(pinnedTopic)
    if (driver is WasmAppTestDriver) {
        driver.clickByDescription("pinned topic preview")
    } else {
        driver.clickByTextContaining(pinnedTopic)
    }
    driver.assertVisibleByDescription("topic")
    driver.navigateBack()
    driver.assertVisibleByText("Unread rooms")
    driver.clickByText("Rooms")
}

private suspend fun preparePrivateActivityScenario(
    createSession: suspend () -> AuthenticatedSession,
): ActivityScenario {
    val viewer = createSession()
    var retained = false
    try {
        val suffix = (System.currentTimeMillis() % 1_000_000).toString().padStart(6, '0')
        val roomName = "private-$suffix"
        val roomId = viewer.sessionManager.createRoom(NewRoom(roomName, "pa$suffix")).getOrThrow().id
        val messages = (1..3).map { "private-message-$suffix-$it" }
        sendActivityMessages(viewer, roomId, messages, privateRoom = true)
        val scenario = ActivityScenario(viewer, roomId, roomName, messages.last(), null)
        retained = true
        return scenario
    } finally {
        if (!retained) viewer.sessionManager.client.close()
    }
}

fun AppiumTestBase<AppAppiumHelper, PlatformAppiumHelper>.testRoomActivityRefreshByHelper() =
    runAppiumBlockingTest {
        platformHelper.runTest(
            testName = testName,
            target = targetHelper,
            setup = { ports ->
                AppiumTestSetup(
                    prepareActivityScenario { targetHelper.createAuthenticatedSession(ports) },
                )
            },
        ) { scope, data ->
            try {
                val driver = scope.driver
                scenarioSignIn(driver, data.viewer.session.pemPrivateKey)
                driver.dismissDialog()
                driver.clickByText("Rooms")
                driver.assertVisibleByTextContaining(data.latestMessage)
                driver.assertVisibleByText("3")
                val activity =
                    data.viewer.sessionManager.getUserRooms(
                        PaginationQuery(),
                    ).getOrThrow().activities.single { it.room.id == data.roomId }
                data.viewer.sessionManager.addReadLog(
                    UpdateUserRead(ObjectTuple(data.roomId, ObjectType.ROOM), requireNotNull(activity.latestMessageId)),
                ).getOrThrow()
                driver.refreshList()
                driver.assertVisibleByTextContaining(data.latestMessage)
                driver.assertNotVisibleByText("3")
                if (driver is AndroidAppTestDriver) {
                    driver.assertUnreadRoomsIndicator(false)
                } else if (driver is WasmAppTestDriver) {
                    driver.assertUnreadRoomsIndicator(false)
                } else {
                    driver.assertNotVisibleByText("Unread rooms")
                }
            } finally {
                data.viewer.sessionManager.client.close()
            }
        }
    }

private fun AppTestDriver.resizeActivityViewport(width: Int, height: Int) {
    when (this) {
        is DesktopAppTestDriver -> resizeWindow(width, height)
        is WasmAppTestDriver -> resizeViewport(width, height)
        else -> error("The current test platformHelper does not support window resizing")
    }
}

private fun AppTestDriver.assertActivityUnreadIndicator(visible: Boolean) {
    when (this) {
        is AndroidAppTestDriver -> assertUnreadRoomsIndicator(visible)
        is WasmAppTestDriver -> assertUnreadRoomsIndicator(visible)
    }
}

private suspend fun scenarioResizeActivityRooms(driver: AppTestDriver, data: ActivityScenario, compactWidth: Int) {
    if (driver is DesktopAppTestDriver || driver is WasmAppTestDriver) {
        val viewports =
            listOf(
                compactWidth to 800,
                900 to 800,
                1600 to 1000,
                1600 to 500,
                compactWidth to 800,
                1600 to 1000,
            )
        viewports.forEach { (width, height) ->
            driver.resizeActivityViewport(width, height)
            if (width >= 900) driver.assertVisibleByText("Members") else driver.assertNotVisibleByText("Members")
            driver.assertVisibleByTextContaining(data.latestMessage)
            driver.assertVisibleByText("3")
            if (driver is WasmAppTestDriver) driver.assertUnreadRoomsIndicator(true)
            driver.saveSnapshot("activity-rooms-$width-$height")
        }
    }
}

private suspend fun scenarioRevisitActivityRoom(driver: AppTestDriver, data: ActivityScenario) {
    repeat(3) {
        driver.clickByTextContaining(data.roomName)
        driver.assertVisibleByDescription("Send")
        driver.assertVisibleByText(data.latestMessage)
        waitUntilRoomRead(data, data.latestMessage)
        driver.dismissKeyboard()
        driver.navigateBack()
        driver.assertVisibleByTextContaining(data.latestMessage)
        driver.assertActivityUnreadIndicator(false)
    }
}
