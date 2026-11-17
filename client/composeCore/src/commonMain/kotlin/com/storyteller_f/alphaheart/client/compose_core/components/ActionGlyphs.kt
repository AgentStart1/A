/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorPath
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.BadgePlus
import com.composables.icons.lucide.Bell
import com.composables.icons.lucide.BellRing
import com.composables.icons.lucide.Calendar
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Copy
import com.composables.icons.lucide.Dices
import com.composables.icons.lucide.Ellipsis
import com.composables.icons.lucide.ExternalLink
import com.composables.icons.lucide.FileDown
import com.composables.icons.lucide.FilePlus
import com.composables.icons.lucide.FolderOpen
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.History
import com.composables.icons.lucide.KeyRound
import com.composables.icons.lucide.Languages
import com.composables.icons.lucide.LogIn
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Maximize2
import com.composables.icons.lucide.Menu
import com.composables.icons.lucide.MessageCircle
import com.composables.icons.lucide.MessageSquare
import com.composables.icons.lucide.MessageSquarePlus
import com.composables.icons.lucide.Mic
import com.composables.icons.lucide.Paperclip
import com.composables.icons.lucide.Pause
import com.composables.icons.lucide.PenLine
import com.composables.icons.lucide.Phone
import com.composables.icons.lucide.Pin
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.RefreshCw
import com.composables.icons.lucide.RotateCw
import com.composables.icons.lucide.Scan
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.Send
import com.composables.icons.lucide.Settings
import com.composables.icons.lucide.Signal
import com.composables.icons.lucide.Smile
import com.composables.icons.lucide.SquarePlus
import com.composables.icons.lucide.ThumbsUp
import com.composables.icons.lucide.Type
import com.composables.icons.lucide.UserRound
import com.composables.icons.lucide.UserRoundCog
import com.composables.icons.lucide.Video
import com.composables.icons.lucide.Volume2
import com.composables.icons.lucide.VolumeX
import com.composables.icons.lucide.Wallet
import com.composables.icons.lucide.Wifi
import com.composables.icons.lucide.WifiOff
import com.composables.icons.lucide.X

/** Roles correspond to approved menu entries; unrelated Material icons are never rewritten. */
enum class ActionGlyph {
    CHEVRON_RIGHT,
    CALENDAR,
    PEN_LINE,
    USER_ROUND,
    SCAN,
    KEY_ROUND,
    HISTORY,
    VIDEO,
    MIC,
    DICES,
    FOLDER_OPEN,
    PLUS,
    WALLET,
    HEART,
    BELL,
    THUMBS_UP,
    MESSAGE_CIRCLE,
    BELL_RING,
    WIFI_OFF,
    USER_ROUND_COG,
    WIFI,
    SIGNAL,
    COPY,
    FILE_DOWN,
    PIN,
    LANGUAGES,
    PHONE,
    SETTINGS,
    MESSAGE_SQUARE_PLUS,
    BADGE_PLUS,
    LOG_IN,
    TYPE,
    CHECK,
    REFRESH_CW,
    SEARCH,
    ARROW_LEFT,
    X,
    SEND,
    PAPERCLIP,
    MAXIMIZE_2,
    SMILE,
    MESSAGE_SQUARE,
    ELLIPSIS,
    EXTERNAL_LINK,
    PLAY,
    PAUSE,
    VOLUME_X,
    VOLUME_2,
    ROTATE_CW,
    MENU,
    FILE_PLUS,
    SQUARE_PLUS,
}

fun actionIcon(glyph: ActionGlyph, selected: Boolean = false): ImageVector =
    when {
        selected && glyph == ActionGlyph.HEART -> selectedHeart
        selected && glyph == ActionGlyph.BELL -> selectedBell
        else -> actionVectors.getValue(glyph)
    }

private val selectedHeart by lazy { filledActionIcon(Lucide.Heart) }
private val selectedBell by lazy { filledActionIcon(Lucide.Bell) }
private val actionVectors by lazy {
    mapOf(
        ActionGlyph.CHEVRON_RIGHT to Lucide.ChevronRight,
        ActionGlyph.CALENDAR to Lucide.Calendar,
        ActionGlyph.PEN_LINE to Lucide.PenLine,
        ActionGlyph.USER_ROUND to Lucide.UserRound,
        ActionGlyph.SCAN to Lucide.Scan,
        ActionGlyph.KEY_ROUND to Lucide.KeyRound,
        ActionGlyph.HISTORY to Lucide.History,
        ActionGlyph.VIDEO to Lucide.Video,
        ActionGlyph.MIC to Lucide.Mic,
        ActionGlyph.DICES to Lucide.Dices,
        ActionGlyph.FOLDER_OPEN to Lucide.FolderOpen,
        ActionGlyph.PLUS to Lucide.Plus,
        ActionGlyph.WALLET to Lucide.Wallet,
        ActionGlyph.HEART to Lucide.Heart,
        ActionGlyph.BELL to Lucide.Bell,
        ActionGlyph.THUMBS_UP to Lucide.ThumbsUp,
        ActionGlyph.MESSAGE_CIRCLE to Lucide.MessageCircle,
        ActionGlyph.BELL_RING to Lucide.BellRing,
        ActionGlyph.WIFI_OFF to Lucide.WifiOff,
        ActionGlyph.USER_ROUND_COG to Lucide.UserRoundCog,
        ActionGlyph.WIFI to Lucide.Wifi,
        ActionGlyph.SIGNAL to Lucide.Signal,
        ActionGlyph.COPY to Lucide.Copy,
        ActionGlyph.FILE_DOWN to Lucide.FileDown,
        ActionGlyph.PIN to Lucide.Pin,
        ActionGlyph.LANGUAGES to Lucide.Languages,
        ActionGlyph.PHONE to Lucide.Phone,
        ActionGlyph.SETTINGS to Lucide.Settings,
        ActionGlyph.MESSAGE_SQUARE_PLUS to Lucide.MessageSquarePlus,
        ActionGlyph.BADGE_PLUS to Lucide.BadgePlus,
        ActionGlyph.LOG_IN to Lucide.LogIn,
        ActionGlyph.TYPE to Lucide.Type,
        ActionGlyph.CHECK to Lucide.Check,
        ActionGlyph.REFRESH_CW to Lucide.RefreshCw,
        ActionGlyph.SEARCH to Lucide.Search,
        ActionGlyph.ARROW_LEFT to Lucide.ArrowLeft,
        ActionGlyph.X to Lucide.X,
        ActionGlyph.SEND to Lucide.Send,
        ActionGlyph.PAPERCLIP to Lucide.Paperclip,
        ActionGlyph.MAXIMIZE_2 to Lucide.Maximize2,
        ActionGlyph.SMILE to Lucide.Smile,
        ActionGlyph.MESSAGE_SQUARE to Lucide.MessageSquare,
        ActionGlyph.ELLIPSIS to Lucide.Ellipsis,
        ActionGlyph.EXTERNAL_LINK to Lucide.ExternalLink,
        ActionGlyph.PLAY to Lucide.Play,
        ActionGlyph.PAUSE to Lucide.Pause,
        ActionGlyph.VOLUME_X to Lucide.VolumeX,
        ActionGlyph.VOLUME_2 to Lucide.Volume2,
        ActionGlyph.ROTATE_CW to Lucide.RotateCw,
        ActionGlyph.MENU to Lucide.Menu,
        ActionGlyph.FILE_PLUS to Lucide.FilePlus,
        ActionGlyph.SQUARE_PLUS to Lucide.SquarePlus,
    )
}

/** Selected object actions keep the library geometry while retaining their existing filled state. */
private fun filledActionIcon(source: ImageVector): ImageVector =
    ImageVector.Builder(
        source.name + "Selected",
        source.defaultWidth,
        source.defaultHeight,
        source.viewportWidth,
        source.viewportHeight,
    ).apply {
        for (index in 0 until source.root.size) {
            val path = source.root[index] as VectorPath
            addPath(
                pathData = path.pathData,
                pathFillType = path.pathFillType,
                fill = SolidColor(Color.Black),
                fillAlpha = path.fillAlpha,
                stroke = path.stroke,
                strokeAlpha = path.strokeAlpha,
                strokeLineWidth = path.strokeLineWidth,
                strokeLineCap = path.strokeLineCap,
                strokeLineJoin = path.strokeLineJoin,
            )
        }
    }.build()
