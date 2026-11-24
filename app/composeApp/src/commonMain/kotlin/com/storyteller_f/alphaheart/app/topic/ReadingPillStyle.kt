/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.topic

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val LocalPillInteractive = staticCompositionLocalOf { true }

enum class PillAppearance { DEFAULT, FEED_ACTION, CONTEXT_ACTION, REACTION }

internal data class PillMetrics(
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

internal fun pillMetrics(appearance: PillAppearance): PillMetrics =
    when (appearance) {
        PillAppearance.DEFAULT -> defaultPill
        PillAppearance.FEED_ACTION -> feedActionPill
        PillAppearance.CONTEXT_ACTION -> contextActionPill
        PillAppearance.REACTION -> feedActionPill
    }
