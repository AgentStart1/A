/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.storyteller_f.alphaheart.client.compose_core.Res
import com.storyteller_f.alphaheart.client.compose_core.copy
import com.storyteller_f.alphaheart.client.compose_core.media_download
import com.storyteller_f.alphaheart.client.compose_core.media_exit_fullscreen
import com.storyteller_f.alphaheart.client.compose_core.media_fullscreen
import com.storyteller_f.alphaheart.client.compose_core.media_loading
import com.storyteller_f.alphaheart.client.compose_core.media_mute
import com.storyteller_f.alphaheart.client.compose_core.media_options
import com.storyteller_f.alphaheart.client.compose_core.media_pause
import com.storyteller_f.alphaheart.client.compose_core.media_play
import com.storyteller_f.alphaheart.client.compose_core.media_playback_error
import com.storyteller_f.alphaheart.client.compose_core.media_position
import com.storyteller_f.alphaheart.client.compose_core.media_retry
import com.storyteller_f.alphaheart.client.compose_core.media_unmute
import com.storyteller_f.alphaheart.client.compose_core.media_volume
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Times are seconds; volume is normalized to 0..1. Unknown duration disables seeking. */
internal data class MediaControlState(
    val playing: Boolean = false,
    val loading: Boolean = false,
    val failed: Boolean = false,
    val enabled: Boolean = true,
    val volumeEnabled: Boolean = true,
    val expansionEnabled: Boolean = true,
    val position: Double = 0.0,
    val duration: Double = 0.0,
    val volume: Float = 1f,
    val muted: Boolean = false,
    val title: String? = null,
)

internal data class MediaControlActions(
    val togglePlayback: () -> Unit,
    val seek: (Double) -> Unit,
    val setVolume: (Float) -> Unit,
    val toggleMute: () -> Unit,
    val retry: (() -> Unit)? = null,
)

internal fun formatMediaTime(seconds: Double): String {
    val total = if (seconds.isFinite()) seconds.toLong().coerceAtLeast(0) else 0
    val minutes = (total / 60 % 60).toString().padStart(2, '0')
    val remainder = (total % 60).toString().padStart(2, '0')
    return if (total >= 3600) "${total / 3600}:$minutes:$remainder" else "${total / 60}:$remainder"
}

/** Owns presentation only; the caller retains the playback engine across expansion. */
@Composable
internal fun MediaPlayerSurface(
    title: String,
    state: MediaControlState,
    actions: MediaControlActions,
    modifier: Modifier = Modifier,
    video: Boolean = true,
    controlsVisible: Boolean = true,
    sourceUrl: String? = null,
    artwork: String? = null,
    menuContent: @Composable (() -> Unit) -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    if (expanded) {
        Dialog(
            onDismissRequest = { expanded = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            MediaSurfaceContent(
                state.title ?: title, state, actions, Modifier.fillMaxSize(), video, controlsVisible, true,
                { expanded = false }, sourceUrl, artwork, menuContent, content,
            )
        }
        Box(modifier.background(Color.Black, RoundedCornerShape(16.dp)))
    } else {
        MediaSurfaceContent(state.title ?: title, state, actions, modifier, video, controlsVisible, false, {
            expanded = true
        }, sourceUrl, artwork, menuContent, content)
    }
}

@Composable
private fun MediaSurfaceContent(
    title: String,
    state: MediaControlState,
    actions: MediaControlActions,
    modifier: Modifier,
    video: Boolean,
    controlsVisible: Boolean,
    expanded: Boolean,
    toggleExpanded: () -> Unit,
    sourceUrl: String?,
    artwork: String?,
    menuContent: @Composable (() -> Unit) -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFFBFCBFF))) {
        Box(modifier.clip(RoundedCornerShape(if (expanded) 0.dp else 16.dp)).background(Color(0xFF101218))) {
            if (video) {
                content()
            } else {
                MediaAudioArtwork(title, artwork)
            }
            if (controlsVisible) {
                MediaStatus(state, actions.retry, Modifier.align(Alignment.Center).padding(bottom = 72.dp))
                if (sourceUrl != null) {
                    Box(Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                        MediaOptions(sourceUrl, menuContent)
                    }
                }
                Column(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE6101218))))
                        .padding(start = 12.dp, end = 12.dp, top = 20.dp, bottom = 4.dp),
                ) {
                    if (video) {
                        Text(
                            title,
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    MediaTransport(state, actions, expanded, toggleExpanded)
                }
            }
        }
    }
}

@Composable
private fun MediaAudioArtwork(title: String, artwork: String?) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val artworkSize = if (maxHeight > 400.dp) 160.dp else 64.dp
        Column(
            Modifier.align(Alignment.Center).fillMaxWidth().padding(16.dp).padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(artworkSize).clip(RoundedCornerShape(16.dp)).background(Color(0xFF262B39)),
                contentAlignment = Alignment.Center,
            ) {
                if (artwork != null) {
                    AsyncImage(artwork, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Icon(Icons.Default.MusicNote, null, Modifier.size(32.dp), tint = Color(0xFFBFCBFF))
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(title, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun MediaStatus(state: MediaControlState, retry: (() -> Unit)?, modifier: Modifier) {
    val loadingLabel = stringResource(Res.string.media_loading)
    when {
        state.failed ->
            Column(modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(Res.string.media_playback_error),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (retry != null) TextButton(onClick = retry) { Text(stringResource(Res.string.media_retry)) }
            }

        state.loading ->
            CircularProgressIndicator(
                modifier.size(32.dp).semantics {
                    contentDescription = loadingLabel
                },
                color = Color.White,
                strokeWidth = 2.dp,
            )
    }
}

@Composable
private fun MediaTransport(
    state: MediaControlState,
    actions: MediaControlActions,
    expanded: Boolean,
    toggleExpanded: () -> Unit,
) {
    var seekDraft by remember { mutableStateOf<Float?>(null) }
    val duration = state.duration.takeIf { it.isFinite() && it > 0 } ?: 0.0
    val position = state.position.takeIf { it.isFinite() }?.coerceIn(0.0, duration) ?: 0.0
    val enabled = state.enabled && !state.failed
    val positionLabel = stringResource(Res.string.media_position)
    MediaSlider(
        value = seekDraft ?: position.toFloat(),
        onValueChange = { seekDraft = it },
        onValueChangeFinished = {
            seekDraft?.let { actions.seek(it.toDouble()) }
            seekDraft = null
        },
        valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
        enabled = enabled && duration > 0,
        modifier =
        Modifier.fillMaxWidth().height(28.dp)
            .appiumSemantics(testTag = "media-position", description = positionLabel)
            .semantics { contentDescription = positionLabel },
    )
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        MediaIcon(
            if (state.playing) Icons.Default.Pause else Icons.Default.PlayArrow,
            stringResource(if (state.playing) Res.string.media_pause else Res.string.media_play),
            enabled,
            actions.togglePlayback,
        )
        val progress = "${formatMediaTime(seekDraft?.toDouble() ?: position)} / ${formatMediaTime(duration)}"
        Text(
            progress,
            Modifier.weight(1f).appiumSemantics(text = progress),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            maxLines = 1,
        )
        MediaVolume(state, actions, enabled && state.volumeEnabled)
        MediaIcon(
            if (expanded) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
            stringResource(if (expanded) Res.string.media_exit_fullscreen else Res.string.media_fullscreen),
            state.expansionEnabled,
            toggleExpanded,
        )
    }
}

@Composable
private fun MediaVolume(state: MediaControlState, actions: MediaControlActions, enabled: Boolean) {
    var expanded by remember { mutableStateOf(false) }
    val label = stringResource(Res.string.media_volume)
    val icon =
        if (state.muted ||
            state.volume == 0f
        ) {
            Icons.AutoMirrored.Filled.VolumeOff
        } else {
            Icons.AutoMirrored.Filled.VolumeUp
        }
    Box {
        MediaIcon(icon, label, enabled) { expanded = !expanded }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Column(Modifier.width(192.dp).padding(horizontal = 16.dp)) {
                Text(label, style = MaterialTheme.typography.labelMedium)
                MediaSlider(
                    value = if (state.muted) 0f else state.volume.coerceIn(0f, 1f),
                    onValueChange = actions.setVolume,
                    enabled = enabled,
                    modifier = Modifier.semantics { contentDescription = label },
                )
                val muteLabel = stringResource(if (state.muted) Res.string.media_unmute else Res.string.media_mute)
                val toggleMute = {
                    actions.toggleMute()
                    expanded = false
                }
                TextButton(
                    onClick = toggleMute,
                    enabled = enabled,
                    modifier = Modifier.appiumSemantics(text = muteLabel, onClick = if (enabled) toggleMute else null),
                ) {
                    Text(muteLabel)
                }
            }
        }
    }
}

@Composable
private fun MediaIcon(icon: ImageVector, label: String, enabled: Boolean, action: () -> Unit) {
    IconButton(
        onClick = action,
        enabled = enabled,
        modifier = Modifier.appiumSemantics(text = label, onClick = if (enabled) action else null),
    ) {
        Icon(icon, label, tint = if (enabled) Color.White else Color.White.copy(alpha = 0.38f))
    }
}

@Composable
private fun MediaOptions(sourceUrl: String, content: @Composable (() -> Unit) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val clipboard = LocalClipboard.current
    val toast = LocalToaster.current
    val scope = rememberCoroutineScope()
    MediaIcon(Icons.Default.MoreVert, stringResource(Res.string.media_options), true) { expanded = true }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(text = { Text(stringResource(Res.string.media_download)) }, onClick = {
            expanded = false
            uriHandler.openUri(sourceUrl)
        }, leadingIcon = { Icon(Icons.Default.Download, null) })
        DropdownMenuItem(text = { Text(stringResource(Res.string.copy)) }, onClick = {
            expanded = false
            scope.launch { clipboard.copyText(sourceUrl, toast) }
        }, leadingIcon = { Icon(Icons.Default.ContentCopy, null) })
        content { expanded = false }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        onValueChangeFinished = onValueChangeFinished,
        thumb = {
            Box(
                Modifier.size(10.dp).background(
                    if (enabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.38f,
                        )
                    },
                    CircleShape,
                ),
            )
        },
        track = { slider ->
            SliderDefaults.Track(
                sliderState = slider,
                enabled = enabled,
                modifier = Modifier.height(3.dp),
                thumbTrackGapSize = 0.dp,
                drawStopIndicator = null,
            )
        },
    )
}
