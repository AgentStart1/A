/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.topic.LocalPillInteractive
import com.storyteller_f.alphaheart.app.topic.PillAppearance
import com.storyteller_f.alphaheart.app.topic.pillMetrics
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon

/** Measures candidate controls without publishing actions or hidden accessibility descendants. */
@Composable
fun PillMeasurementScope(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalPillInteractive provides false, content = content)
}

@Composable
fun Pill(
    text: String? = null,
    icon: ImageVector? = null,
    emoji: String? = null,
    selected: Boolean = false,
    description: String? = if (emoji == null) text else listOfNotNull(emoji, text?.trim()).joinToString(" "),
    appearance: PillAppearance = PillAppearance.DEFAULT,
    pending: Boolean = false,
    highlightWithLine: Boolean = appearance == PillAppearance.REACTION,
    onClick: (() -> Unit)?,
) {
    val metrics = pillMetrics(appearance)
    val interactive = LocalPillInteractive.current
    val showReactionLine = highlightWithLine && (selected || pending)
    val reactionColor = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(8.dp)
    val background =
        when {
            selected && !highlightWithLine -> MaterialTheme.colorScheme.secondaryContainer
            else -> Color.Transparent
        }
    val textColor =
        when {
            showReactionLine -> reactionColor
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
            .then(if (interactive && onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .then(if (showReactionLine) reactionLine(reactionColor, pending && interactive) else Modifier)
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

@Composable
private fun reactionLine(color: Color, pending: Boolean): Modifier {
    val phase =
        if (pending) {
            rememberInfiniteTransition(label = "reaction-pending").animateFloat(
                initialValue = 0f,
                targetValue = 6f,
                animationSpec = infiniteRepeatable(tween(durationMillis = 600, easing = LinearEasing)),
                label = "dash-phase",
            )
        } else {
            null
        }
    return Modifier.drawWithContent {
        drawContent()
        val centerY = size.height - 1.dp.toPx()
        drawLine(
            color = color,
            start = Offset(4.dp.toPx(), centerY),
            end = Offset(size.width - 4.dp.toPx(), centerY),
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Round,
            pathEffect =
            phase?.let {
                PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 3.dp.toPx()), it.value.dp.toPx())
            },
        )
    }
}
