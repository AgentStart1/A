/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics

private val LocalIconActionMenu = staticCompositionLocalOf { false }

@Composable
fun IconActionMenu(twoActions: Boolean = false, topSpacing: Dp = 22.dp, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalIconActionMenu provides true) {
        Layout(content = content, modifier = Modifier.fillMaxWidth()) { children, constraints ->
            val gap = 12.dp.roundToPx()
            val columns = if (twoActions) 2 else 4
            val availableWidth = (constraints.maxWidth - gap * (columns - 1)).coerceAtLeast(0)
            val widths =
                IntArray(columns) { column ->
                    if (twoActions) {
                        60.dp.roundToPx()
                    } else {
                        availableWidth / columns + if (column < availableWidth % columns) 1 else 0
                    }
                }
            val tileHeight = 52.dp.roundToPx()
            val placeables =
                children.mapIndexed { index, child ->
                    child.measure(Constraints.fixed(widths[index % columns], tileHeight))
                }
            val rows = (placeables.size + columns - 1) / columns
            val top = if (rows == 0) 0 else topSpacing.roundToPx()
            val height = if (rows == 0) 0 else top + rows * tileHeight + (rows - 1) * gap
            val start = if (twoActions) ((constraints.maxWidth - widths.sum() - gap) / 2).coerceAtLeast(0) else 0
            layout(constraints.maxWidth, height) {
                placeables.forEachIndexed { index, child ->
                    val column = index % columns
                    val x = start + (0 until column).sumOf { widths[it] + gap }
                    child.placeRelative(x, top + index / columns * (tileHeight + gap))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IconMenuAction(
    icon: IconRes,
    title: String,
    semanticDescription: String?,
    suffix: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    val interactions = remember { MutableInteractionSource() }
    val hovered by interactions.collectIsHoveredAsState()
    val focused by interactions.collectIsFocusedAsState()
    val shape = RoundedCornerShape(10.dp)
    val tileBackground = if (hovered) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(title) } },
        state = rememberTooltipState(),
    ) {
        Box(
            Modifier.fillMaxWidth().height(52.dp).clip(shape)
                .background(tileBackground)
                .then(if (focused) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, shape) else Modifier)
                .semantics(mergeDescendants = true) { contentDescription = semanticDescription ?: title }
                .appiumSemantics(description = semanticDescription ?: title, text = title, onClick = onClick)
                .clickable(interactionSource = interactions, role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) {
                CustomIcon(menuActionIcon(icon), iconSize = 21.dp)
            }
            Box(Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 3.dp)) { suffix() }
        }
    }
}

private fun menuActionIcon(icon: IconRes): IconRes {
    if (icon !is IconRes.Vector) return icon
    val vector =
        when (icon.vector.name.substringAfterLast('.')) {
            "AccountBalanceWallet" -> Icons.Outlined.AccountBalanceWallet
            "Favorite" -> Icons.Outlined.Favorite
            "FavoriteBorder" -> Icons.Outlined.FavoriteBorder
            "NotificationsOff" -> Icons.Outlined.NotificationsOff
            "NotificationsActive" -> Icons.Outlined.NotificationsActive
            "ThumbUp" -> Icons.Outlined.ThumbUp
            "ChatBubble" -> Icons.Outlined.ChatBubbleOutline
            "CardMembership" -> Icons.Outlined.Groups
            "SwitchAccount" -> Icons.Outlined.SwitchAccount
            "ContentCopy" -> Icons.Outlined.ContentCopy
            "PictureAsPdf" -> Icons.Outlined.PictureAsPdf
            "Phone" -> Icons.Outlined.Phone
            "Logout" -> Icons.AutoMirrored.Outlined.Logout
            else -> readingIcon(icon.vector)
        }
    return icon.copy(vector = vector)
}

@Composable
fun ButtonNav(icon: ImageVector, title: String, semanticDescription: String? = null, onClick: () -> Unit = {}) {
    ButtonNav(IconRes.Vector(icon), title, semanticDescription = semanticDescription, onClick = onClick)
}

@Composable
fun ButtonNav(icon: Char, title: String, semanticDescription: String? = null, onClick: () -> Unit = {}) {
    ButtonNav(IconRes.Font(icon), title, semanticDescription = semanticDescription, onClick = onClick)
}

@Composable
fun ButtonNav(
    icon: IconRes,
    title: String,
    suffix: @Composable () -> Unit = {},
    semanticDescription: String? = null,
    onClick: () -> Unit = {},
) {
    if (LocalIconActionMenu.current) {
        IconMenuAction(icon, title, semanticDescription, suffix, onClick)
        return
    }
    val shape = RoundedCornerShape(8.dp)
    val semanticsModifier =
        Modifier.appiumSemantics(
            description = semanticDescription,
            text = title,
            onClick = onClick,
        )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier =
        Modifier.fillMaxWidth().then(semanticsModifier).clip(shape).clickable {
            onClick()
        }.padding(horizontal = 8.dp, vertical = 12.dp),
    ) {
        CustomIcon(icon)
        Text(title)
        Spacer(modifier = Modifier.weight(1f))
        suffix()
    }
}
