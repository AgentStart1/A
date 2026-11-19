/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.storyteller_f.alphaheart.client.compose_core.media_replay
import com.storyteller_f.alphaheart.client.compose_core.media_retry
import com.storyteller_f.alphaheart.client.compose_core.media_unmute
import com.storyteller_f.alphaheart.client.compose_core.media_volume
import com.storyteller_f.alphaheart.client.compose_core.preview
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.alphaheart.client.compose_core.utils.readingColorScheme
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingDialog as Dialog
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon

internal val LocalMediaPreviewAction = staticCompositionLocalOf<(() -> Unit)?> { null }
private val LocalMediaDownloadEnabled = staticCompositionLocalOf { true }
private val LocalMediaHeadingVisible = staticCompositionLocalOf { true }

/** Avoids a duplicate download entry when the surrounding file page already provides that action. */
@Composable
fun MediaMenuScope(showDownload: Boolean = true, showHeading: Boolean = true, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalMediaDownloadEnabled provides showDownload,
        LocalMediaHeadingVisible provides showHeading,
        content = content,
    )
}

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
    val durationKnown: Boolean = true,
    val showTime: Boolean = false,
    val ended: Boolean = false,
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

/** Embedded video reserves its picture ratio independently of the heading and transport controls. */
sealed interface MediaSurfaceLayout {
    data object Bounded : MediaSurfaceLayout
    data class EmbeddedVideo(val aspectRatio: Float = 16f / 9f) : MediaSurfaceLayout
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
    layout: MediaSurfaceLayout = MediaSurfaceLayout.Bounded,
    menuContent: @Composable (() -> Unit) -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var embeddedSize by remember { mutableStateOf(IntSize.Zero) }
    var playbackRequested by remember(sourceUrl, title) { mutableStateOf(false) }
    fun recordPlayback(action: () -> Unit): () -> Unit =
        {
            playbackRequested = true
            action()
        }
    val surfaceActions =
        actions.copy(
            togglePlayback = recordPlayback(actions.togglePlayback),
            retry = actions.retry?.let(::recordPlayback),
        )
    val showTime = state.showTime || playbackRequested || state.playing || state.ended || state.position > 0
    val surfaceState = state.copy(showTime = showTime)
    val density = LocalDensity.current
    if (expanded) {
        Dialog(
            onDismissRequest = { expanded = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                MediaSurfaceContent(
                    state.title ?: title, surfaceState, surfaceActions,
                    if (video) Modifier.widthIn(max = 900.dp).fillMaxWidth() else Modifier.fillMaxSize(),
                    video, controlsVisible, true,
                    { expanded = false }, sourceUrl, artwork, layout, menuContent, content,
                )
            }
        }
        Box(
            modifier.size(
                with(density) { embeddedSize.width.toDp() },
                with(density) { embeddedSize.height.toDp() },
            ).background(Color.Black, RoundedCornerShape(16.dp)),
        )
    } else {
        MediaSurfaceContent(
            state.title ?: title, surfaceState, surfaceActions,
            modifier.onSizeChanged {
                embeddedSize = it
            },
            video, controlsVisible, false, {
                expanded = true
            }, sourceUrl, artwork, layout, menuContent, content,
        )
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
    layout: MediaSurfaceLayout,
    menuContent: @Composable (() -> Unit) -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    if (!video && !expanded && artwork == null) {
        CompactAudioSurface(title, state, actions, modifier, controlsVisible, toggleExpanded, sourceUrl, menuContent)
        return
    }
    val showEmbeddedHeading = controlsVisible && LocalMediaHeadingVisible.current
    val showHeading = expanded || showEmbeddedHeading
    val pictureRatio =
        if (video) {
            if (expanded) 16f / 9f else (layout as? MediaSurfaceLayout.EmbeddedVideo)?.aspectRatio
        } else {
            null
        }
    Column(
        modifier.testTag("media-player-shell")
            .then(
                if (expanded) {
                    Modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(20.dp)
                } else {
                    Modifier
                },
            ),
    ) {
        if (showHeading) MediaSurfaceHeader(title, expanded, toggleExpanded, sourceUrl, menuContent)
        if (expanded) Spacer(Modifier.height(18.dp))
        Column(
            Modifier.fillMaxWidth()
                .then(if (pictureRatio != null) Modifier else Modifier.weight(1f))
                .testTag("media-player-frame"),
        ) {
            MaterialTheme(colorScheme = readingColorScheme(darkTheme = true)) {
                val stageModifier =
                    if (pictureRatio != null) {
                        Modifier.fillMaxWidth().aspectRatio(pictureRatio)
                    } else {
                        Modifier.fillMaxWidth().weight(1f)
                    }
                Box(
                    stageModifier.clip(
                        RoundedCornerShape(10.dp),
                    ).background(Color(0xFF14241A)).testTag("media-player-stage"),
                ) {
                    if (video) content() else MediaAudioArtwork(title, artwork, !expanded && !controlsVisible)
                }
            }
            if (controlsVisible) {
                Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    MediaTransport(state, actions, expanded, toggleExpanded) {
                        if (!showHeading && sourceUrl != null) MediaOptions(sourceUrl, menuContent)
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaSurfaceHeader(
    title: String,
    expanded: Boolean,
    collapse: () -> Unit,
    sourceUrl: String?,
    menuContent: @Composable (() -> Unit) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth()
            .then(if (expanded) Modifier else Modifier.padding(bottom = 8.dp))
            .testTag("media-player-heading"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            Modifier.weight(1f),
            fontSize = if (expanded) 17.sp else 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (sourceUrl != null) MediaOptions(sourceUrl, menuContent)
        if (expanded) {
            MediaIcon(
                actionIcon(ActionGlyph.X),
                stringResource(Res.string.media_exit_fullscreen),
                true,
                action = collapse,
            )
        }
    }
}

@Composable
private fun CompactAudioSurface(
    title: String,
    state: MediaControlState,
    actions: MediaControlActions,
    modifier: Modifier,
    controlsVisible: Boolean,
    toggleExpanded: () -> Unit,
    sourceUrl: String?,
    menuContent: @Composable (() -> Unit) -> Unit,
) {
    Column(modifier.widthIn(max = 740.dp).fillMaxWidth()) {
        if (LocalMediaHeadingVisible.current) {
            MediaSurfaceHeader(title, false, toggleExpanded, sourceUrl.takeIf { controlsVisible }, menuContent)
        }
        if (controlsVisible) {
            MediaTransport(state, actions, false, toggleExpanded) {
                if (!LocalMediaHeadingVisible.current && sourceUrl != null) MediaOptions(sourceUrl, menuContent)
            }
        }
    }
}

@Composable
private fun MediaPosition(
    state: MediaControlState,
    actions: MediaControlActions,
    modifier: Modifier,
    enabled: Boolean,
) {
    var seekDraft by remember { mutableStateOf<Float?>(null) }
    val duration = state.duration.takeIf { it.isFinite() && it > 0 } ?: 0.0
    val position = state.position.takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0
    val label = stringResource(Res.string.media_position)
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MediaSlider(
            value = seekDraft ?: position.coerceAtMost(duration).toFloat(),
            onValueChange = { seekDraft = it },
            onValueChangeFinished = {
                seekDraft?.let { actions.seek(it.toDouble()) }
                seekDraft = null
            },
            enabled = enabled && state.showTime && duration > 0,
            valueRange = 0f..duration.toFloat().coerceAtLeast(1f),
            modifier =
            Modifier.weight(1f).height(32.dp)
                .appiumSemantics(testTag = "media-position", description = label)
                .semantics { contentDescription = label },
        )
        val durationLabel = if (state.durationKnown && duration > 0) formatMediaTime(duration) else "--:--"
        val progress = "${formatMediaTime(seekDraft?.toDouble() ?: position)} / $durationLabel"
        if (state.showTime && !state.failed && (!state.loading || position > 0)) {
            Text(
                progress,
                Modifier.appiumSemantics(text = progress),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun MediaAudioArtwork(title: String, artwork: String?, showTitle: Boolean) {
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
                    Icon(Icons.Default.MusicNote, null, Modifier.size(32.dp), tint = Color(0xFF8FC4A5))
                }
            }
            if (showTitle) {
                Spacer(Modifier.height(12.dp))
                Text(title, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun MediaTransport(
    state: MediaControlState,
    actions: MediaControlActions,
    expanded: Boolean,
    toggleExpanded: () -> Unit,
    endActions: @Composable () -> Unit,
) {
    val enabled = state.enabled && !state.failed && !state.loading
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            MediaPlaybackButton(state, actions, enabled)
        }
        MediaPosition(state, actions, Modifier.weight(1f), enabled)
        MediaVolume(state, actions, enabled && state.volumeEnabled)
        if (!expanded) {
            MediaIcon(
                actionIcon(ActionGlyph.MAXIMIZE_2),
                stringResource(Res.string.media_fullscreen),
                state.expansionEnabled,
                action = toggleExpanded,
            )
        }
        endActions()
    }
}

@Composable
private fun MediaPlaybackButton(state: MediaControlState, actions: MediaControlActions, enabled: Boolean) {
    when {
        state.failed -> {
            val retry = actions.retry
            val label = stringResource(if (retry != null) Res.string.media_retry else Res.string.media_playback_error)
            MediaIcon(actionIcon(ActionGlyph.ROTATE_CW), label, retry != null) { retry?.invoke() }
        }

        state.loading -> {
            val label = stringResource(Res.string.media_loading)
            CircularProgressIndicator(
                Modifier.size(18.dp).semantics { contentDescription = label },
                strokeWidth = 2.dp,
            )
        }

        else -> {
            val glyph =
                when {
                    state.ended -> ActionGlyph.ROTATE_CW
                    state.playing -> ActionGlyph.PAUSE
                    else -> ActionGlyph.PLAY
                }
            val label =
                when {
                    state.ended -> Res.string.media_replay
                    state.playing -> Res.string.media_pause
                    else -> Res.string.media_play
                }
            MediaIcon(actionIcon(glyph), stringResource(label), enabled, accent = true, action = actions.togglePlayback)
        }
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
            actionIcon(ActionGlyph.VOLUME_X)
        } else {
            actionIcon(ActionGlyph.VOLUME_2)
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
private fun MediaIcon(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    accent: Boolean = false,
    action: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val containerColor = if (accent) scheme.primary else Color.Transparent
    val tint = if (accent && enabled) scheme.onPrimary else scheme.onSurface
    IconButton(
        onClick = action,
        enabled = enabled,
        modifier = Modifier.appiumSemantics(text = label, onClick = if (enabled) action else null),
        colors = IconButtonDefaults.iconButtonColors(containerColor = containerColor),
    ) {
        Icon(icon, label, Modifier.size(17.dp), tint = tint.copy(alpha = if (enabled) 1f else 0.38f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaOptions(sourceUrl: String, content: @Composable (() -> Unit) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val downloadResource = LocalResourceDownloadAction.current
    val downloadFile = LocalResourceDownloadFile.current
    val clipboard = LocalClipboard.current
    val toast = LocalToaster.current
    val scope = rememberCoroutineScope()
    val openPreview = LocalMediaPreviewAction.current
    val showDownload = LocalMediaDownloadEnabled.current
    MediaIcon(actionIcon(ActionGlyph.ELLIPSIS), stringResource(Res.string.media_options), true) { expanded = true }
    if (expanded) {
        BasicAlertDialog(onDismissRequest = { expanded = false }) {
            ReadingActionMenuSurface(
                onDismiss = { expanded = false },
                heading = { Text(stringResource(Res.string.media_options), fontSize = 17.sp) },
            ) {
                IconActionMenu {
                    if (openPreview != null) {
                        ButtonNav(actionIcon(ActionGlyph.EXTERNAL_LINK), stringResource(Res.string.preview)) {
                            expanded = false
                            openPreview()
                        }
                    }
                    if (showDownload) {
                        ButtonNav(navigationIcon(NavigationGlyph.DOWNLOAD), stringResource(Res.string.media_download)) {
                            expanded = false
                            downloadResource(mediaDownloadRequest(sourceUrl, downloadFile))
                        }
                    }
                    ButtonNav(actionIcon(ActionGlyph.COPY), stringResource(Res.string.copy)) {
                        scope.launch(start = CoroutineStart.UNDISPATCHED) {
                            clipboard.copyText(sourceUrl, toast)
                            expanded = false
                        }
                    }
                    content { expanded = false }
                }
            }
        }
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
        valueRange = valueRange,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
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
