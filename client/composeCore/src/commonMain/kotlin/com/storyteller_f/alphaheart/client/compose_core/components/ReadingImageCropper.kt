/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.attafitamim.krop.core.crop.CropState
import com.attafitamim.krop.core.crop.CropperStyle
import com.attafitamim.krop.core.crop.LocalCropperStyle
import com.attafitamim.krop.core.crop.flipHorizontal
import com.attafitamim.krop.core.crop.flipVertical
import com.attafitamim.krop.core.crop.getTransformedImageRect
import com.attafitamim.krop.core.crop.rotLeft
import com.attafitamim.krop.core.crop.rotRight
import com.attafitamim.krop.core.utils.coerceAtMost
import com.attafitamim.krop.core.utils.constrainOffset
import com.attafitamim.krop.ui.CropperPreview
import com.storyteller_f.alphaheart.client.compose_core.Res
import com.storyteller_f.alphaheart.client.compose_core.cancel
import com.storyteller_f.alphaheart.client.compose_core.close
import com.storyteller_f.alphaheart.client.compose_core.crop_confirm
import com.storyteller_f.alphaheart.client.compose_core.crop_flip_horizontal
import com.storyteller_f.alphaheart.client.compose_core.crop_flip_vertical
import com.storyteller_f.alphaheart.client.compose_core.crop_reset
import com.storyteller_f.alphaheart.client.compose_core.crop_rotate_left
import com.storyteller_f.alphaheart.client.compose_core.crop_rotate_right
import com.storyteller_f.alphaheart.client.compose_core.crop_title
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingDialog as Dialog

/** Uses Krop's real preview, transform controller and completion pipeline with reading chrome. */
@Composable
fun ReadingImageCropperDialog(state: CropState, style: CropperStyle) {
    val background = MaterialTheme.colorScheme.background
    val accent = MaterialTheme.colorScheme.primary
    val soft = MaterialTheme.colorScheme.primaryContainer
    val readingStyle =
        remember(style, background, accent, soft) {
            object : CropperStyle by style {
                override val backgroundColor = background
                override val overlayColor = soft.copy(alpha = .55f)

                override fun DrawScope.drawCropRect(region: Rect) {
                    drawRect(
                        color = accent,
                        topLeft = region.topLeft,
                        size = region.size,
                        style =
                        Stroke(
                            2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
                        ),
                    )
                    handles.forEach { handle ->
                        val center = region.topLeft + Offset(region.width * handle.x, region.height * handle.y)
                        drawCircle(accent, 3.dp.toPx(), center)
                    }
                }
            }
        }
    LaunchedEffect(state) { state.setInitialState(style) }
    val ratio = style.aspects.firstOrNull()
    val width =
        when {
            ratio == null || ratio.x == ratio.y -> 220.dp
            ratio.x > ratio.y -> 280.dp
            else -> 190.dp
        }
    val height = if (ratio == null) width else width * ratio.y.toFloat() / ratio.x.toFloat()
    Dialog(onDismissRequest = { state.done(false) }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        ReadingCropSurface(
            Modifier.padding(18.dp).widthIn(max = 620.dp).fillMaxWidth(),
            cancel = { state.done(false) },
            confirm = { state.done(true) },
            accepted = state.accepted,
            tools = { ReadingCropTools(state) },
        ) {
            CompositionLocalProvider(LocalCropperStyle provides readingStyle) {
                CropperPreview(state, Modifier.size(width, height).testTag("reading-crop-preview"))
            }
        }
    }
}

@Composable
internal fun ReadingCropSurface(
    modifier: Modifier = Modifier,
    cancel: () -> Unit,
    confirm: () -> Unit,
    accepted: Boolean = false,
    tools: @Composable () -> Unit,
    preview: @Composable () -> Unit,
) {
    Surface(
        modifier.testTag("reading-crop-dialog"),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(Res.string.crop_title),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                )
                ReadingIconButton(cancel) { ReadingIcon(actionIcon(ActionGlyph.X), stringResource(Res.string.close)) }
            }
            Column(
                Modifier.fillMaxWidth().heightIn(min = 300.dp).testTag("reading-crop-stage")
                    .background(MaterialTheme.colorScheme.background).padding(25.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) { tools() }
                Box(contentAlignment = Alignment.Center) { preview() }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
                ReadingOutlinedButton(cancel) { Text(stringResource(Res.string.cancel)) }
                ReadingButton(confirm, enabled = !accepted) {
                    ReadingIcon(Icons.Default.Check, null)
                    Text(stringResource(Res.string.crop_confirm))
                }
            }
        }
    }
}

@Composable
internal fun ReadingCropTools(state: CropState) {
    CropTool(
        stringResource(Res.string.crop_rotate_left),
        Icons.Default.RotateLeft,
        { state.rotateReadingCrop(clockwise = false) },
    )
    CropTool(
        stringResource(Res.string.crop_rotate_right),
        Icons.Default.RotateRight,
        { state.rotateReadingCrop(clockwise = true) },
    )
    CropTool(stringResource(Res.string.crop_flip_horizontal), Icons.Default.Flip, state::flipHorizontal)
    CropTool(
        stringResource(Res.string.crop_flip_vertical),
        Icons.Default.Flip,
        state::flipVertical,
        Modifier.rotate(90f),
    )
    CropTool(stringResource(Res.string.crop_reset), Icons.Default.Restore, state::reset)
}

@Composable
private fun CropTool(label: String, icon: ImageVector, action: () -> Unit, iconModifier: Modifier = Modifier) {
    ReadingIconButton(action, Modifier.size(32.dp)) { ReadingIcon(icon, label, iconModifier) }
}

/** Rotation changes image orientation, while a locked crop keeps its required output orientation. */
private fun CropState.rotateReadingCrop(clockwise: Boolean) {
    val previousSize = region.size
    if (clockwise) rotRight() else rotLeft()
    if (aspectLock) {
        val bounds = getTransformedImageRect(transform, src.size)
        val size = previousSize.coerceAtMost(bounds.size)
        val aligned = Rect(region.center - Offset(size.width / 2, size.height / 2), size).constrainOffset(bounds)
        aspectLock = false
        try {
            region = aligned
        } finally {
            aspectLock = true
        }
    }
}
