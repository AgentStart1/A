/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.navigation

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorPath
import com.storyteller_f.alphaheart.client.compose_core.icons.IconRes
import com.storyteller_f.alphaheart.client.compose_core.icons.readingIcon

internal val LocalIconActionMenu = staticCompositionLocalOf { false }

internal fun menuActionIcon(icon: IconRes): IconRes {
    if (icon !is IconRes.Vector) return icon
    return icon.copy(vector = thinMenuStroke(readingIcon(icon.vector)))
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
