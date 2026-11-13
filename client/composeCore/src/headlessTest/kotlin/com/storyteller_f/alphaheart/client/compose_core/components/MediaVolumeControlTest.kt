/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import kotlin.test.Test
import kotlin.test.assertEquals

internal class MediaVolumeControlTest {
    @Test
    fun `mute restores volume with either native or browser snapshots`() {
        listOf(0f, 0.4f).forEach { mutedVolume ->
            val commands = mutableListOf<Pair<Float, Boolean>>()
            val control = MediaVolumeControl { volume, muted -> commands += volume to muted }
            control.setVolume(0.4f)
            control.toggleMute(0.4f, false)
            control.toggleMute(mutedVolume, true)
            assertEquals(listOf(0.4f to false, 0.4f to true, 0.4f to false), commands)
        }
    }

    @Test
    fun `zero volume restores the last audible slider value`() {
        val commands = mutableListOf<Pair<Float, Boolean>>()
        val control = MediaVolumeControl { volume, muted -> commands += volume to muted }
        control.setVolume(0.35f)
        control.setVolume(0f)
        control.toggleMute(0f, false)
        assertEquals(listOf(0.35f to false, 0f to false, 0.35f to false), commands)
    }

    @Test
    fun `slider adjustment unmutes playback`() {
        val commands = mutableListOf<Pair<Float, Boolean>>()
        val control = MediaVolumeControl { volume, muted -> commands += volume to muted }
        control.toggleMute(1f, false)
        control.setVolume(0.6f)
        assertEquals(listOf(1f to true, 0.6f to false), commands)
    }
}
