/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.lucide.Activity
import com.composables.icons.lucide.Award
import com.composables.icons.lucide.CloudUpload
import com.composables.icons.lucide.Download
import com.composables.icons.lucide.Earth
import com.composables.icons.lucide.FileText
import com.composables.icons.lucide.Folder
import com.composables.icons.lucide.House
import com.composables.icons.lucide.Info
import com.composables.icons.lucide.LockKeyhole
import com.composables.icons.lucide.LogOut
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MessagesSquare
import com.composables.icons.lucide.PanelsTopLeft
import com.composables.icons.lucide.ScrollText
import com.composables.icons.lucide.Settings2
import com.composables.icons.lucide.TextAlignStart
import com.composables.icons.lucide.Users

/** Navigation roles are assigned at each destination, never inferred from a Material icon name. */
enum class NavigationGlyph {
    EARTH,
    USERS,
    MESSAGES_SQUARE,
    TEXT,
    AWARD,
    FOLDER,
    CLOUD_UPLOAD,
    DOWNLOAD,
    HOUSE,
    PANELS_TOP_LEFT,
    LOCK_KEYHOLE,
    ACTIVITY,
    SETTINGS_2,
    LOG_OUT,
    FILE_TEXT,
    SCROLL_TEXT,
    INFO,
}

fun navigationIcon(glyph: NavigationGlyph): ImageVector = navigationVectors.getValue(glyph)

internal fun compactNavigationIcon(route: NavRoute): ImageVector = readingIcon(route.icon)

private val navigationVectors by lazy {
    mapOf(
        NavigationGlyph.EARTH to Lucide.Earth,
        NavigationGlyph.USERS to Lucide.Users,
        NavigationGlyph.MESSAGES_SQUARE to Lucide.MessagesSquare,
        NavigationGlyph.TEXT to Lucide.TextAlignStart,
        NavigationGlyph.AWARD to Lucide.Award,
        NavigationGlyph.FOLDER to Lucide.Folder,
        NavigationGlyph.CLOUD_UPLOAD to Lucide.CloudUpload,
        NavigationGlyph.DOWNLOAD to Lucide.Download,
        NavigationGlyph.HOUSE to Lucide.House,
        NavigationGlyph.PANELS_TOP_LEFT to Lucide.PanelsTopLeft,
        NavigationGlyph.LOCK_KEYHOLE to Lucide.LockKeyhole,
        NavigationGlyph.ACTIVITY to Lucide.Activity,
        NavigationGlyph.SETTINGS_2 to Lucide.Settings2,
        NavigationGlyph.LOG_OUT to Lucide.LogOut,
        NavigationGlyph.FILE_TEXT to Lucide.FileText,
        NavigationGlyph.SCROLL_TEXT to Lucide.ScrollText,
        NavigationGlyph.INFO to Lucide.Info,
    )
}
