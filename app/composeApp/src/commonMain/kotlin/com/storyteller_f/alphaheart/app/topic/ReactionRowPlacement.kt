/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.topic

import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeMeasureScope
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import com.storyteller_f.alphaheart.app.components.PillMeasurementScope
import com.storyteller_f.shared.model.ReactionInfo

internal fun Placeable.PlacementScope.placeEmojiRows(
    rows: List<List<Placeable>>,
    maxWidth: Int,
    alignEnd: Boolean,
    horizontalSpace: Int,
    verticalSpace: Int,
) {
    var yOffset = 0
    rows.forEach { row ->
        val rowHeight = row.maxOf { it.height }
        var xOffset =
            if (alignEnd) {
                (maxWidth - row.sumOf { it.width } - horizontalSpace * (row.size - 1)).coerceAtLeast(0)
            } else {
                0
            }
        row.forEach {
            it.placeRelative(xOffset, yOffset + (rowHeight - it.height) / 2)
            xOffset += it.width + horizontalSpace
        }
        yOffset += rowHeight + verticalSpace
    }
}

internal fun SubcomposeMeasureScope.measureFirstStage(
    data: List<ReactionInfo>,
    constraints: Constraints,
    maxWidth: Int,
    horizontalPx: Dp,
    content: @Composable (Int, Boolean) -> Unit,
): Triple<MutableList<MutableList<Placeable>>, Int, Int> {
    val rows = mutableListOf<MutableList<Placeable>>()
    var currentRow: List<Placeable> = emptyList()
    var currentWidth = 0
    var index = 0
    var emojiUsedCount = 0

    while (index < data.size.coerceAtLeast(1) && rows.size < 2) {
        val placeableList =
            subcompose("emoji_$index") {
                PillMeasurementScope { content(index, true) }
            }.map {
                it.measure(constraints)
            }
        val newWidth =
            placeableList.sumOf {
                it.width
            } + horizontalPx.roundToPx() * (placeableList.size - 1).coerceAtLeast(0)

        val nextWidth =
            if (currentRow.isEmpty()) newWidth else currentWidth + horizontalPx.roundToPx() + newWidth

        when {
            nextWidth <= maxWidth -> {
                currentRow += placeableList
                currentWidth = nextWidth
                if (data.isNotEmpty()) {
                    emojiUsedCount++
                }
                // index++ 会导致退出循环，需要循环后面手动补上最后一行
                index++
            }

            rows.isEmpty() -> {
                // 当前还没有完整的一行，并且需要折行
                rows.add(currentRow.toMutableList())
                // 新的一行作为第二行，并且把计算好的placeable 放进list 中，防止重复测量
                index++
                emojiUsedCount++
                currentRow = placeableList
                currentWidth = placeableList.sumOf {
                    it.width
                } + horizontalPx.roundToPx() * (placeableList.size - 1).coerceAtLeast(0)
            }

            else -> {
                // 再加一行就成了第二行，后面会退出循环，清空防止再次加一行
                rows.add(currentRow.toMutableList())
                currentRow = emptyList()
                currentWidth = 0
            }
        }
    }

    if (currentRow.isNotEmpty()) {
        rows.add(currentRow.toMutableList())
    }
    if (rows.isEmpty()) rows.add(mutableListOf())
    val lastRowWidth =
        rows.last().let { list ->
            list.sumOf {
                it.width
            } + (list.size - 1).coerceAtLeast(0) * horizontalPx.roundToPx()
        }
    return Triple(rows, lastRowWidth, emojiUsedCount)
}
