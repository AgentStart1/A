/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

internal fun readingIcon(icon: ImageVector): ImageVector =
    when (icon.name.substringAfterLast('.')) {
        "Public" -> navigationIcon(NavigationGlyph.EARTH)
        "Diversity3" -> navigationIcon(NavigationGlyph.USERS)
        "ChatBubble" -> navigationIcon(NavigationGlyph.MESSAGES_SQUARE)
        "Groups" -> navigationIcon(NavigationGlyph.USERS)
        "Forum" -> navigationIcon(NavigationGlyph.MESSAGES_SQUARE)
        "Folder" -> navigationIcon(NavigationGlyph.FOLDER)
        "Settings" -> actionIcon(ActionGlyph.SETTINGS)
        "Home" -> navigationIcon(NavigationGlyph.HOUSE)
        "Info" -> navigationIcon(NavigationGlyph.INFO)
        "Description" -> navigationIcon(NavigationGlyph.FILE_TEXT)
        "Person" -> actionIcon(ActionGlyph.USER_ROUND)
        "Notifications" -> actionIcon(ActionGlyph.BELL)
        "Menu" -> actionIcon(ActionGlyph.MENU)
        "Search" -> actionIcon(ActionGlyph.SEARCH)
        "WorkspacePremium" -> navigationIcon(NavigationGlyph.AWARD)
        "VpnKey" -> actionIcon(ActionGlyph.KEY_ROUND)
        "History" -> actionIcon(ActionGlyph.HISTORY)
        "Videocam" -> actionIcon(ActionGlyph.VIDEO)
        "Mic" -> actionIcon(ActionGlyph.MIC)
        "Casino" -> actionIcon(ActionGlyph.DICES)
        "FileOpen" -> actionIcon(ActionGlyph.FOLDER_OPEN)
        "Edit" -> actionIcon(ActionGlyph.PEN_LINE)
        "ContentCopy" -> actionIcon(ActionGlyph.COPY)
        else -> icon
    }

@Composable
fun ReadingIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    androidx.compose.material3.Icon(readingIcon(imageVector), contentDescription, modifier.size(17.dp), tint)
}

@Composable
fun ReadingIcon(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    androidx.compose.material3.Icon(painter, contentDescription, modifier.size(17.dp), tint)
}

@Composable
fun ReadingIcon(
    bitmap: ImageBitmap,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    androidx.compose.material3.Icon(bitmap, contentDescription, modifier.size(17.dp), tint)
}
