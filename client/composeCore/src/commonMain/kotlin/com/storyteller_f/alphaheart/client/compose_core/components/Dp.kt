/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun convertPxToDp(px: Int): Dp {
    // 获取当前屏幕密度
    val density = LocalDensity.current.density
    // 将像素值转换为 dp
    return pxToDp(px, density)
}

fun pxToDp(px: Int, density: Float) = (px / density).dp

@Composable
fun convertPxToSp(px: Int): TextUnit {
    // 获取当前屏幕密度
    val density = LocalDensity.current.density
    // 将像素值转换为 dp
    return pxToSp(px, density)
}

fun pxToSp(px: Int, density: Float): TextUnit = (px / density).sp

@Composable
fun textUnitToPx(textUnit: TextUnit): Float {
    val density = LocalDensity.current

    return textUnitToPx(textUnit, density)
}

fun textUnitToPx(textUnit: TextUnit, density: Density): Float =
    if (textUnit.isSp) {
        with(density) {
            textUnit.toPx()
        }
    } else {
        0f
    }
