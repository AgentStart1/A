/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.dev.appium

import com.storyteller_f.alphaheart.api.NewCommunity
import com.storyteller_f.alphaheart.api.NewRoom
import com.storyteller_f.alphaheart.client.core.UserSessionManager
import com.storyteller_f.alphaheart.client.core.createCommunity
import com.storyteller_f.alphaheart.client.core.createRoom
import com.storyteller_f.alphaheart.client.core.searchCurrentUserRooms
import com.storyteller_f.alphaheart.client.core.updateRoomInfo
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.model.RoomInfo
import com.storyteller_f.shared.obj.UpdateRoomBody
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

fun AppiumTestBase<AppAppiumHelper, WasmAppiumHelper>.testClientSearchOrder() =
    withSignedInUser { scope, user, _ ->
        val expected = user.sessionManager.prepareSearchRooms(5).map { it.name }
        val driver = scope.driver as WasmAppTestDriver
        driver.resizeViewport(1600, 1000)
        val probe = driver.searchProbe()
        probe.observe()
        driver.searchRooms()
        probe.assertQueryResult("voyage", expected)
        expected.forEach { driver.assertVisibleByText(it) }
        driver.assertNotVisibleByText("unrelated room")
        probe.assertVisibleOrder(expected)
        driver.assertInputByDescription("search query", "voyage")
    }

fun AppiumTestBase<AppAppiumHelper, WasmAppiumHelper>.testClientSearchPagingRefresh() =
    withSignedInUser { scope, user, _ ->
        val rooms = user.sessionManager.prepareSearchRooms(65)
        val expected = rooms.map { it.name }
        val driver = scope.driver as WasmAppTestDriver
        driver.resizeViewport(1600, 1000)
        val probe = driver.searchProbe()
        probe.observe()
        driver.searchRooms()
        expected.filterIndexed { index, _ -> index > 0 && index % 10 == 0 }.forEach { name ->
            driver.scrollToText(name)
            probe.assertVisibleOrder(expected)
        }
        driver.assertVisibleByText(expected[60])
        probe.awaitContinuation("voyage")
        probe.assertVisibleOrder(expected)
        val anchor = probe.firstVisibleName()
        val top = probe.rowTop(anchor)
        val pages = probe.pageCount()
        val updatedName = "voyage zz"
        user.sessionManager.updateRoomInfo(rooms[60].id, UpdateRoomBody(name = updatedName)).getOrThrow()
        withTimeout(30_000) {
            while (true) {
                val indexed = user.sessionManager.searchCurrentUserRooms("zz", 100, null).getOrThrow().data
                if (indexed.any { it.id == rooms[60].id }) break
                delay(100)
            }
        }
        val refreshed = user.sessionManager.searchCurrentUserRooms("voyage", 100, null).getOrThrow().data
        driver.clickByText("Refresh")
        probe.awaitAdditionalPage(pages)
        probe.assertVisibleOrder(refreshed.map { it.name })
        driver.assertVisibleByText(anchor)
        driver.assertInputByDescription("search query", "voyage")
        assertTrue(abs(top - probe.rowTop(anchor)) <= 1.0, "Refresh must preserve the first visible room anchor")
        driver.scrollToText(updatedName)
        driver.assertVisibleByText(updatedName)
        driver.assertNotVisibleByText(expected[60])
    }

private suspend fun WasmAppTestDriver.searchRooms() {
    clickByText("Rooms")
    clickByDescription("search query")
    inputTextByDescription("search query", "voyage")
    submitInputByDescription("search query")
}

private suspend fun UserSessionManager.prepareSearchRooms(count: Int): List<RoomInfo> {
    val community = createCommunity(NewCommunity("search fixture", "searchfixture")).getOrThrow()
    createRoom(NewRoom("unrelated room", "unrelated", communityId = community.id)).getOrThrow()
    repeat(count) { index ->
        val label = (index * 37 % 67).toString().padStart(2, '0')
        createRoom(NewRoom("voyage $label", "voyage$label", communityId = community.id)).getOrThrow()
    }
    return withTimeout(30_000) {
        var rooms: List<RoomInfo>
        do {
            rooms = searchCurrentUserRooms("voyage", 100, null).getOrThrow().data
            if (rooms.size != count) delay(100)
        } while (rooms.size != count)
        assertEquals(count, rooms.map { it.id }.distinct().size)
        rooms
    }
}
