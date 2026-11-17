/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon

private val LocalPillInteractive = staticCompositionLocalOf { true }

/** Measures candidate controls without publishing actions or hidden accessibility descendants. */
@Composable
fun PillMeasurementScope(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalPillInteractive provides false, content = content)
}

enum class PillAppearance { DEFAULT, FEED_ACTION, CONTEXT_ACTION }

private data class PillMetrics(
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val borderPadding: Dp,
    val gap: Dp,
    val fontSize: TextUnit,
    val lineHeight: TextUnit,
)

private val defaultPill = PillMetrics(10.dp, 2.dp, 0.dp, 4.dp, 14.sp, TextUnit.Unspecified)
private val feedActionPill = PillMetrics(4.dp, 4.dp, 0.dp, 7.dp, 12.sp, 19.2.sp)

// Context quiet buttons retain the reference base 1px transparent border in their measured box.
private val contextActionPill = PillMetrics(6.dp, 4.dp, 1.dp, 7.dp, 12.sp, 19.2.sp)

private fun pillMetrics(appearance: PillAppearance): PillMetrics =
    when (appearance) {
        PillAppearance.DEFAULT -> defaultPill
        PillAppearance.FEED_ACTION -> feedActionPill
        PillAppearance.CONTEXT_ACTION -> contextActionPill
    }

@Composable
fun Pill(
    text: String? = null,
    icon: ImageVector? = null,
    emoji: String? = null,
    selected: Boolean = false,
    description: String? = if (emoji == null) text else listOfNotNull(emoji, text?.trim()).joinToString(" "),
    appearance: PillAppearance = PillAppearance.DEFAULT,
    onClick: () -> Unit,
) {
    val metrics = pillMetrics(appearance)
    val interactive = LocalPillInteractive.current
    val shape = RoundedCornerShape(8.dp)
    val background =
        when {
            selected -> MaterialTheme.colorScheme.secondaryContainer
            else -> androidx.compose.ui.graphics.Color.Transparent
        }
    val textColor =
        when {
            selected -> MaterialTheme.colorScheme.onSecondaryContainer
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    Row(
        modifier =
        (
            if (interactive) {
                Modifier.semantics {
                    this.selected = selected
                    if (description != null) contentDescription = description
                }.appiumSemantics(description = description, onClick = onClick)
            } else {
                Modifier.clearAndSetSemantics {}
            }
            ).background(background, shape)
            .then(if (appearance == PillAppearance.DEFAULT) Modifier.height(32.dp) else Modifier)
            .clip(shape)
            .then(if (interactive) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(metrics.borderPadding)
            .padding(horizontal = metrics.horizontalPadding, vertical = metrics.verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(metrics.gap),
    ) {
        when {
            icon != null ->
                Icon(
                    icon,
                    contentDescription = description,
                    modifier = Modifier.size(17.dp),
                    tint = textColor,
                )

            emoji != null -> {
                Text(emoji)
            }
        }
        if (text != null) {
            Text(text, color = textColor, fontSize = metrics.fontSize, lineHeight = metrics.lineHeight)
        }
    }
}
