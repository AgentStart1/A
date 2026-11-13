/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

object LayoutDefaults {
    val contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)

    val pagingVerticalArrangement = Arrangement.spacedBy(10.dp)
    val pagingHorizontalArrangement = Arrangement.spacedBy(10.dp)
}

fun Modifier.safeArea(paddingValues: PaddingValues, layoutDirection: LayoutDirection): Modifier =
    padding(
        top = paddingValues.calculateTopPadding(),
        start = paddingValues.calculateStartPadding(layoutDirection),
        end = paddingValues.calculateEndPadding(layoutDirection),
    )

fun Modifier.horizontalSafeArea(paddingValues: PaddingValues, layoutDirection: LayoutDirection): Modifier =
    padding(
        start = paddingValues.calculateStartPadding(layoutDirection),
        end = paddingValues.calculateEndPadding(layoutDirection),
    )
