/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.utils

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.DpSize

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun CurrentWindowSizeClass(): WindowSizeClass {
    val size = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    return WindowSizeClass.calculateFromSize(with(density) { DpSize(size.width.toDp(), size.height.toDp()) })
}
