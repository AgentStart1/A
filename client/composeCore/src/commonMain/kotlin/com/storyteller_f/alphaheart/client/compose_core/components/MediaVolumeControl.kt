/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

/** Owned by one engine's presentation. Commands run on its UI event loop. */
internal class MediaVolumeControl(private val writeVolume: (Float, Boolean) -> Unit) {
    private var audibleVolume = 1f

    fun setVolume(volume: Float) {
        val normalized = volume.coerceIn(0f, 1f)
        if (normalized > 0f) audibleVolume = normalized
        writeVolume(normalized, false)
    }

    fun toggleMute(volume: Float, muted: Boolean) {
        if (muted || volume == 0f) {
            writeVolume(audibleVolume, false)
        } else {
            audibleVolume = volume
            writeVolume(volume, true)
        }
    }
}
