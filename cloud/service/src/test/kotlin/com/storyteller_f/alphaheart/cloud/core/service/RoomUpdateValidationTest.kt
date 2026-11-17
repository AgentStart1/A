/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.cloud.core.service

import com.storyteller_f.result.CustomResult
import com.storyteller_f.shared.obj.UpdateRoomBody
import kotlin.test.Test
import kotlin.test.assertIs

internal class RoomUpdateValidationTest {
    @Test
    fun iconOnlyUpdatesAndResetDoNotRequireARename() {
        for (icon in listOf(42L, 0L)) {
            assertIs<CustomResult.Success<*>>(checkRoomName(UpdateRoomBody(icon = icon)))
        }
    }

    @Test
    fun explicitInvalidNamesRemainRejected() {
        for (name in listOf("", "\u0000", "x".repeat(1000))) {
            assertIs<CustomResult.Failure<*>>(checkRoomName(UpdateRoomBody(name = name)))
        }
        assertIs<CustomResult.Success<*>>(checkRoomName(UpdateRoomBody(name = "Visible room")))
    }
}
