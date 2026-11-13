/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.rtc

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.LocalUiViewModel
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.rtc_audio_only
import com.storyteller_f.alphaheart.app.rtc_camera_off
import com.storyteller_f.alphaheart.app.rtc_camera_on
import com.storyteller_f.alphaheart.app.rtc_connected
import com.storyteller_f.alphaheart.app.rtc_connecting
import com.storyteller_f.alphaheart.app.rtc_disconnected
import com.storyteller_f.alphaheart.app.rtc_failed
import com.storyteller_f.alphaheart.app.rtc_join_audio
import com.storyteller_f.alphaheart.app.rtc_join_video
import com.storyteller_f.alphaheart.app.rtc_joining
import com.storyteller_f.alphaheart.app.rtc_leave
import com.storyteller_f.alphaheart.app.rtc_media_failed
import com.storyteller_f.alphaheart.app.rtc_mute
import com.storyteller_f.alphaheart.app.rtc_muted
import com.storyteller_f.alphaheart.app.rtc_participant
import com.storyteller_f.alphaheart.app.rtc_participants
import com.storyteller_f.alphaheart.app.rtc_ready
import com.storyteller_f.alphaheart.app.rtc_room
import com.storyteller_f.alphaheart.app.rtc_switch_camera
import com.storyteller_f.alphaheart.app.rtc_unmute
import com.storyteller_f.alphaheart.app.rtc_waiting
import com.storyteller_f.alphaheart.app.rtc_you
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.alphaheart.client.core.sendFrame
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.type.PrimaryKey
import kotlinx.coroutines.Dispatchers
import org.jetbrains.compose.resources.stringResource

// Application-owned serial coordination, separate from rendering on native platforms.
internal val rtcCoordination = Dispatchers.Default.limitedParallelism(1)

@Composable
internal fun RtcCallPage(roomId: PrimaryKey) {
    val scope = rememberCoroutineScope()
    val account by LocalUiViewModel.current.instance.collectAsStateWithLifecycle()
    val nav = LocalAppNavFactory.current.newAppNav()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val host =
        remember(account, roomId) {
            val socket = account.sessionManager.webSocketClient
            RtcCallHost(roomId, scope, rtcCoordination, createRtcEngine(), socket.frameFlow) { frame ->
                socket.useWebSocket { sendFrame(frame) }.getOrThrow()
            }
        }
    DisposableEffect(host) { onDispose { host.close() } }
    LaunchedEffect(host, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            host.effects.collect { nav.back() }
        }
    }
    NavigationBackHandler(rememberNavigationEventState(NavigationEventInfo.None)) { host.leave() }
    val state by host.state.collectAsStateWithLifecycle()
    RtcCallContent(roomId, state, host)
}

@Composable
private fun RtcCallContent(roomId: PrimaryKey, state: RtcCallState, host: RtcCallHost) {
    Scaffold { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = host::leave,
                    modifier = Modifier.appiumSemantics(description = "rtc-back", onClick = host::leave),
                ) {
                    Icon(Icons.AutoMirrored.Default.ArrowBack, stringResource(Res.string.rtc_leave))
                }
                Column {
                    Text(stringResource(Res.string.rtc_room, roomId), style = MaterialTheme.typography.headlineSmall)
                    Text(
                        when (state) {
                            is RtcCallState.Active ->
                                stringResource(
                                    Res.string.rtc_participants,
                                    state.participants.size + 1,
                                )

                            RtcCallState.Joining, RtcCallState.Leaving -> stringResource(Res.string.rtc_joining)

                            is RtcCallState.Ready -> stringResource(Res.string.rtc_ready)
                        },
                    )
                }
            }
            when (state) {
                is RtcCallState.Active -> {
                    ParticipantGrid(state, Modifier.weight(1f))
                    if (state.mediaFailed) Text(stringResource(Res.string.rtc_media_failed))
                    ActiveCallControls(state, host)
                }

                RtcCallState.Joining, RtcCallState.Leaving -> {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    LeaveCallButton(host::leave)
                }

                is RtcCallState.Ready -> {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(stringResource(if (state.failed) Res.string.rtc_failed else Res.string.rtc_ready))
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        RtcJoinButton(video = true, enabled = true) { host.join(video = true) }
                        RtcJoinButton(video = false, enabled = true) { host.join(video = false) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParticipantGrid(state: RtcCallState.Active, modifier: Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(220.dp),
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "local") {
            ParticipantTile(
                stringResource(Res.string.rtc_you),
                state.localVideo,
                state.audioMuted,
                state.videoMuted,
                stringResource(Res.string.rtc_connected),
                local = true,
            )
        }
        items(state.participants, key = { it.uid }) { peer ->
            ParticipantTile(
                stringResource(Res.string.rtc_participant, peer.uid),
                peer.video,
                peer.audioMuted,
                peer.videoMuted,
                stringResource(
                    when (peer.connection) {
                        RtcConnectionState.Connecting -> Res.string.rtc_connecting
                        RtcConnectionState.Connected -> Res.string.rtc_connected
                        RtcConnectionState.Disconnected -> Res.string.rtc_disconnected
                        RtcConnectionState.Failed -> Res.string.rtc_failed
                    },
                ),
            )
        }
        if (state.participants.isEmpty()) {
            item(key = "waiting") {
                Card(Modifier.fillMaxWidth().aspectRatio(4f / 3f)) {
                    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(Res.string.rtc_waiting))
                    }
                }
            }
        }
    }
}

@Composable
private fun ParticipantTile(
    label: String,
    video: RtcVideo?,
    audioMuted: Boolean,
    videoMuted: Boolean,
    status: String,
    local: Boolean = false,
) {
    Card(
        Modifier.fillMaxWidth().appiumSemantics(
            text = "$label, $status, audioMuted=$audioMuted, videoMuted=$videoMuted",
        ),
    ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(4f / 3f).background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            if (video != null && !videoMuted) {
                RtcVideoView(video, Modifier.fillMaxSize(), local)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Person, null)
                    Text(stringResource(if (videoMuted) Res.string.rtc_camera_off else Res.string.rtc_audio_only))
                }
            }
        }
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                Text(label, style = MaterialTheme.typography.titleSmall)
                Text(
                    if (audioMuted) "$status · ${stringResource(Res.string.rtc_muted)}" else status,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ActiveCallControls(state: RtcCallState.Active, host: RtcCallHost) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilledTonalButton(
            onClick = host::toggleAudio,
            modifier = Modifier.appiumSemantics(description = "rtc-toggle-audio", onClick = host::toggleAudio),
        ) {
            Icon(if (state.audioMuted) Icons.Default.MicOff else Icons.Default.Mic, null)
            Text(stringResource(if (state.audioMuted) Res.string.rtc_unmute else Res.string.rtc_mute))
        }
        FilledTonalButton(
            onClick = host::toggleVideo,
            enabled = state.localVideo != null,
            modifier =
            Modifier.appiumSemantics(
                description = "rtc-toggle-video",
                onClick = if (state.localVideo != null) host::toggleVideo else null,
            ),
        ) {
            Icon(if (state.videoMuted) Icons.Default.VideocamOff else Icons.Default.Videocam, null)
            Text(stringResource(if (state.videoMuted) Res.string.rtc_camera_on else Res.string.rtc_camera_off))
        }
        if (state.localVideo != null && state.media.canSwitchCamera) {
            FilledTonalButton(
                onClick = host::switchCamera,
                modifier =
                Modifier.appiumSemantics(
                    description = "rtc-switch-camera",
                    onClick = host::switchCamera,
                ),
            ) {
                Icon(Icons.Default.Cameraswitch, null)
                Text(stringResource(Res.string.rtc_switch_camera))
            }
        }
        LeaveCallButton(host::leave)
    }
}

@Composable
private fun LeaveCallButton(onLeave: () -> Unit) {
    Button(onClick = onLeave, modifier = Modifier.appiumSemantics(description = "rtc-leave", onClick = onLeave)) {
        Icon(Icons.Default.CallEnd, null)
        Text(stringResource(Res.string.rtc_leave))
    }
}

@Composable
internal fun DefaultRtcJoinButton(video: Boolean, enabled: Boolean, onJoin: () -> Unit) {
    Button(
        onClick = onJoin,
        enabled = enabled,
        modifier =
        Modifier.appiumSemantics(
            description = if (video) "rtc-join-video" else "rtc-join-audio",
            onClick = if (enabled) onJoin else null,
        ),
    ) {
        Text(stringResource(if (video) Res.string.rtc_join_video else Res.string.rtc_join_audio))
    }
}
