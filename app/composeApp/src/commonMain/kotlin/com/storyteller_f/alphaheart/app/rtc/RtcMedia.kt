/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.rtc

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.storyteller_f.shared.obj.CustomCandidate
import kotlinx.coroutines.CoroutineScope

internal interface RtcVideo {
    suspend fun close() = Unit
}

internal enum class RtcConnectionState { Connecting, Connected, Disconnected, Failed }

internal interface RtcMedia {
    val video: RtcVideo?
    val canSwitchCamera: Boolean get() = false
    suspend fun mute(audioMuted: Boolean, videoMuted: Boolean)
    suspend fun switchCamera()
    suspend fun close()
}

internal interface RtcPeer {
    suspend fun createOffer(): String
    suspend fun createAnswer(offer: String): String
    suspend fun acceptAnswer(answer: String)
    suspend fun addCandidate(candidate: CustomCandidate)
    suspend fun close()
}

internal data class RtcPeerEvents(
    val candidate: (CustomCandidate) -> Unit,
    val video: (RtcVideo) -> Unit,
    val connection: (RtcConnectionState) -> Unit,
)

/** Media adapters own native resources
 callbacks may arrive on native worker threads. */
internal interface RtcEngine {
    suspend fun openMedia(video: Boolean): RtcMedia
    suspend fun createPeer(media: RtcMedia, events: RtcPeerEvents, scope: CoroutineScope): RtcPeer
}

internal expect fun createRtcEngine(): RtcEngine

@Composable
internal expect fun RtcVideoView(video: RtcVideo, modifier: Modifier, local: Boolean)

@Composable
internal expect fun RtcJoinButton(video: Boolean, enabled: Boolean, onJoin: () -> Unit)
