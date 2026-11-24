/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorPath
import com.storyteller_f.alphaheart.client.compose_core.icons.IconRes
import com.storyteller_f.alphaheart.client.compose_core.icons.readingIcon

internal val LocalIconActionMenu = staticCompositionLocalOf { false }

internal fun menuActionIcon(icon: IconRes): IconRes {
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
    return icon.copy(vector = thinMenuStroke(vector))
}

private fun thinMenuStroke(vector: ImageVector): ImageVector {
    val paths = vector.root.filterIsInstance<VectorPath>()
    if (paths.size != vector.root.size || paths.all { it.stroke == null }) return vector
    return ImageVector.Builder(
        vector.name + "Menu",
        vector.defaultWidth,
        vector.defaultHeight,
        vector.viewportWidth,
        vector.viewportHeight,
        autoMirror = vector.autoMirror,
    ).apply {
        paths.forEach { path ->
            addPath(
                pathData = path.pathData,
                pathFillType = path.pathFillType,
                fill = path.fill,
                fillAlpha = path.fillAlpha,
                stroke = path.stroke,
                strokeAlpha = path.strokeAlpha,
                strokeLineWidth = path.strokeLineWidth * 0.75f,
                strokeLineCap = path.strokeLineCap,
                strokeLineJoin = path.strokeLineJoin,
                strokeLineMiter = path.strokeLineMiter,
                trimPathStart = path.trimPathStart,
                trimPathEnd = path.trimPathEnd,
                trimPathOffset = path.trimPathOffset,
            )
        }
    }.build()
}
