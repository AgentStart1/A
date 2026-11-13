/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.media3.common.Player
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.lang.reflect.Proxy
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
internal class AndroidMediaControlsTest {
    @Test
    fun retryPreparesIdlePlayerBeforePlaying() {
        val calls = mutableListOf<String>()
        val player = player(Player.STATE_IDLE, calls)
        assertNotNull(createMediaActions(player).retry).invoke()
        assertEquals(listOf("prepare", "play"), calls)
    }

    @Test
    fun playPreparesIdlePlayerWithRetainedPlayIntent() {
        val calls = mutableListOf<String>()
        createMediaActions(player(Player.STATE_IDLE, calls)).togglePlayback()
        assertEquals(listOf("prepare", "play"), calls)
    }

    @Test
    fun replaySeeksBeforePlayingEndedMedia() {
        val calls = mutableListOf<String>()
        createMediaActions(player(Player.STATE_ENDED, calls)).togglePlayback()
        assertEquals(listOf("seekToDefaultPosition", "play"), calls)
    }

    private fun player(state: Int, calls: MutableList<String>): Player =
        Proxy.newProxyInstance(Player::class.java.classLoader, arrayOf(Player::class.java)) { _, method, _ ->
            when (method.name) {
                "getPlaybackState" -> state

                "getPlayWhenReady", "isCommandAvailable" -> true

                "prepare", "play", "seekToDefaultPosition" -> {
                    calls += method.name
                    null
                }

                else -> error("Unexpected player call: ${method.name}")
            }
        } as Player
}
