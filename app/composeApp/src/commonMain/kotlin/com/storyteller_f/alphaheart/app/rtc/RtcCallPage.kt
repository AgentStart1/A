/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.rtc

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
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
import com.storyteller_f.alphaheart.app.LocalUserInfo
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.common.createRoomViewModel
import com.storyteller_f.alphaheart.app.common.createUserViewModel
import com.storyteller_f.alphaheart.app.pages.user.UserIconWithDialog
import com.storyteller_f.alphaheart.app.rtc_audio_only
import com.storyteller_f.alphaheart.app.rtc_call_title
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
import com.storyteller_f.alphaheart.app.rtc_leaving_status
import com.storyteller_f.alphaheart.app.rtc_media_failed
import com.storyteller_f.alphaheart.app.rtc_mute
import com.storyteller_f.alphaheart.app.rtc_muted
import com.storyteller_f.alphaheart.app.rtc_participant
import com.storyteller_f.alphaheart.app.rtc_ready_badge
import com.storyteller_f.alphaheart.app.rtc_switch_camera
import com.storyteller_f.alphaheart.app.rtc_unmute
import com.storyteller_f.alphaheart.app.rtc_waiting
import com.storyteller_f.alphaheart.app.rtc_you
import com.storyteller_f.alphaheart.client.compose_core.components.CustomAlertDialog
import com.storyteller_f.alphaheart.client.compose_core.components.rememberAlertDialogController
import com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.alphaheart.client.core.sendFrame
import com.storyteller_f.result.getOrThrow
import com.storyteller_f.shared.type.PrimaryKey
import kotlinx.coroutines.Dispatchers
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIconButton as IconButton
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingTextButton as TextButton

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
    val room by createRoomViewModel(roomId).handler.data.collectAsStateWithLifecycle()
    RtcCallContent(room?.name.orEmpty(), state, host)
}

@Composable
private fun RtcCallContent(roomName: String, state: RtcCallState, host: RtcCallHost) {
    Scaffold(topBar = { RtcToolbar(roomName, host::leave) }) { padding ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(padding)) {
            val compact = maxWidth <= 600.dp
            Column(
                Modifier.fillMaxSize().padding(
                    horizontal = if (compact) 14.dp else 30.dp,
                    vertical = if (compact) 20.dp else 30.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                when (state) {
                    is RtcCallState.Active -> {
                        ParticipantGrid(state, Modifier.weight(1f))
                        ActiveCallControls(state, host)
                    }

                    is RtcCallState.Ready -> RtcReadyContent(state.failed, host::join, Modifier.weight(1f))

                    RtcCallState.Joining, RtcCallState.Leaving -> {
                        Box(Modifier.fillMaxWidth().heightIn(min = 320.dp), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(20.dp),
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    stringResource(
                                        if (state == RtcCallState.Leaving) {
                                            Res.string.rtc_leaving_status
                                        } else {
                                            Res.string.rtc_joining
                                        },
                                    ),
                                )
                            }
                        }
                        Box(
                            Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) { LeaveCallButton(host::leave) }
                    }
                }
            }
        }
    }
}

@Composable
internal fun RtcToolbar(roomName: String, leave: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Column {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(leave, Modifier.appiumSemantics(description = "rtc-back", onClick = leave)) {
                    Icon(actionIcon(ActionGlyph.ARROW_LEFT), stringResource(Res.string.rtc_leave))
                }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(stringResource(Res.string.rtc_call_title), style = MaterialTheme.typography.titleMedium)
                    if (roomName.isNotBlank()) {
                        Text(
                            roomName,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
internal fun RtcReadyContent(failed: Boolean, join: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().heightIn(min = 320.dp), contentAlignment = Alignment.Center) {
            Surface(
                color =
                if (failed) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            ) {
                Text(
                    stringResource(if (failed) Res.string.rtc_failed else Res.string.rtc_ready_badge),
                    Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                )
            }
        }
        FlowRow(
            Modifier.fillMaxWidth().padding(top = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        ) {
            RtcJoinButton(video = true, enabled = true) { join(true) }
            RtcJoinButton(video = false, enabled = true) { join(false) }
        }
    }
}

@Composable
private fun ParticipantGrid(state: RtcCallState.Active, modifier: Modifier) {
    val me = LocalUserInfo.current
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
                failure = state.mediaFailure,
                userId = me?.id,
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
                userId = peer.uid,
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
    failure: Throwable? = null,
    userId: PrimaryKey? = null,
) {
    val errorDialog = rememberAlertDialogController()
    CustomAlertDialog(errorDialog, errorDialog::close, errorDialog::close)
    Card(
        Modifier.fillMaxWidth().appiumSemantics(
            text = "$label, $status, audioMuted=$audioMuted, videoMuted=$videoMuted",
        ),
    ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(4f / 3f).background(Color(0xFF15231B)),
            contentAlignment = Alignment.Center,
        ) {
            if (video != null && !videoMuted) {
                RtcVideoView(video, Modifier.fillMaxSize(), local)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ParticipantAvatar(userId)
                    Text(
                        stringResource(if (videoMuted) Res.string.rtc_camera_off else Res.string.rtc_audio_only),
                        color = Color.White,
                    )
                }
            }
            if (failure != null) {
                TextButton(
                    { errorDialog.showErrorMessage(failure) },
                    Modifier.align(Alignment.BottomCenter).padding(12.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                ) { Text(stringResource(Res.string.rtc_media_failed)) }
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
private fun ParticipantAvatar(userId: PrimaryKey?) {
    val user = userId?.let { createUserViewModel(it).handler.data.collectAsStateWithLifecycle().value }
    UserIconWithDialog(user, size = 48.dp, setClickEvent = user != null)
}

@Composable
private fun ActiveCallControls(state: RtcCallState.Active, host: RtcCallHost) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RtcActionButton(
            glyph = if (state.audioMuted) ActionGlyph.MIC_OFF else ActionGlyph.MIC,
            label = stringResource(if (state.audioMuted) Res.string.rtc_unmute else Res.string.rtc_mute),
            action = "rtc-toggle-audio",
            onClick = host::toggleAudio,
        )
        RtcActionButton(
            glyph = if (state.videoMuted) ActionGlyph.VIDEO_OFF else ActionGlyph.VIDEO,
            label = stringResource(if (state.videoMuted) Res.string.rtc_camera_on else Res.string.rtc_camera_off),
            action = "rtc-toggle-video",
            enabled = state.localVideo != null,
            onClick = host::toggleVideo,
        )
        if (state.localVideo != null && state.media.canSwitchCamera) {
            RtcActionButton(
                ActionGlyph.SWITCH_CAMERA,
                stringResource(Res.string.rtc_switch_camera),
                "rtc-switch-camera",
                onClick = host::switchCamera,
            )
        }
        LeaveCallButton(host::leave)
    }
}

@Composable
private fun LeaveCallButton(onLeave: () -> Unit) {
    RtcActionButton(ActionGlyph.PHONE_OFF, stringResource(Res.string.rtc_leave), "rtc-leave", onClick = onLeave)
}

@Composable
internal fun DefaultRtcJoinButton(video: Boolean, enabled: Boolean, onJoin: () -> Unit) {
    RtcActionButton(
        glyph = if (video) ActionGlyph.VIDEO else ActionGlyph.MIC,
        label = stringResource(if (video) Res.string.rtc_join_video else Res.string.rtc_join_audio),
        action = if (video) "rtc-join-video" else "rtc-join-audio",
        enabled = enabled,
        onClick = onJoin,
    )
}

@Composable
private fun RtcActionButton(
    glyph: ActionGlyph,
    label: String,
    action: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    IconButton(
        onClick,
        Modifier.appiumSemantics(description = action, onClick = onClick.takeIf { enabled }),
        enabled = enabled,
    ) {
        Icon(actionIcon(glyph), label)
    }
}
