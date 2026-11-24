/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.icons

import androidx.compose.ui.graphics.vector.ImageVector

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
