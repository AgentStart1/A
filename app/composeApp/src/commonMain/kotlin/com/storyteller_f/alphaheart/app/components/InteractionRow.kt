/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.SubcomposeMeasureScope
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.LocalGlobalTask
import com.storyteller_f.alphaheart.app.LocalSessionManager
import com.storyteller_f.alphaheart.app.pages.topic.TopicDialog
import com.storyteller_f.alphaheart.app.pages.topic.addReaction
import com.storyteller_f.alphaheart.app.pages.topic.deleteReaction
import com.storyteller_f.alphaheart.client.compose_core.components.Pill
import com.storyteller_f.alphaheart.client.compose_core.components.PillMeasurementScope
import com.storyteller_f.alphaheart.client.core.LoadingState
import com.storyteller_f.shared.model.ReactionInfo
import com.storyteller_f.shared.model.TopicDetail

@Composable
fun InteractionRow(
    topicInfo: TopicDetail,
    startAddReaction: () -> Unit,
    presentation: TopicActionsPresentation = TopicActionsPresentation.FEED,
    alignEnd: Boolean = false,
    startAddComment: (() -> Unit)? = null,
) {
    InteractionRowInternal(topicInfo.reactions, topicInfo, startAddComment, startAddReaction, alignEnd, presentation)
}

@Composable
fun InteractionRowInternal(
    data: List<ReactionInfo>,
    topicInfo: TopicDetail,
    startAddComment: (() -> Unit)?,
    startAddReaction: () -> Unit,
    alignEnd: Boolean = false,
    presentation: TopicActionsPresentation = TopicActionsPresentation.FEED,
) {
    val appNavFactory = LocalAppNavFactory.current
    val userSessionManager = LocalSessionManager.current
    var showMenu by remember { mutableStateOf(false) }
    TopicActionsRow(
        TopicActionState(data, topicInfo.commentCount, topicInfo.hasComment, presentation, alignEnd),
        onComment = startAddComment,
        onReaction = {
            if (userSessionManager.currentIsAlreadySignUp) {
                startAddReaction()
            } else {
                appNavFactory.newAppNav().requestSignIn()
            }
        },
        onMore = { showMenu = true },
        onOverflow = { appNavFactory.newAppNav().gotoReactionListPage(topicInfo.id) },
        emojiContent = { index -> EmojiCell(data[index], topicInfo) },
    )
    TopicDialog(topicInfo, showMenu) { showMenu = false }
}

@Composable
internal fun EmojiRow(
    data: List<ReactionInfo>,
    alignEnd: Boolean,
    overflow: @Composable (Int) -> Unit,
    content: @Composable (Int, Boolean) -> Unit,
    horizontalSpacing: Dp = 8.dp,
) {
    val verticalPx = 4.dp
    val horizontalPx = horizontalSpacing
    SubcomposeLayout { constraints ->
        val maxWidth = constraints.maxWidth
        val firstMeasureResult =
            measureFirstStage(data, constraints, maxWidth, horizontalPx, content)
        val currentRow = firstMeasureResult.first.last()
        var currentWidth = firstMeasureResult.second
        var emojiUsed = firstMeasureResult.third

        while (true) {
            // 提前计算overflow 的尺寸
            val overflowPlaceable =
                subcompose("overflow$emojiUsed") {
                    PillMeasurementScope { overflow(emojiUsed) }
                }.map {
                    it.measure(constraints)
                }
            val groupGap = if (overflowPlaceable.isNotEmpty() && currentRow.isNotEmpty()) 1 else 0
            val spacing = horizontalPx.roundToPx() * ((overflowPlaceable.size - 1).coerceAtLeast(0) + groupGap)
            if (currentWidth + overflowPlaceable.sumOf { it.width } + spacing <= maxWidth) {
                currentRow.addAll(overflowPlaceable)
                break
            } else if (firstMeasureResult.first.size < 2) {
                // 超过了当前行，但是现在只有一行，选择跳到下一行
                firstMeasureResult.first.add(
                    mutableListOf<Placeable>().apply {
                        addAll(overflowPlaceable)
                    },
                )
                break
            }
            // 不停的删除最后一行的emoji 直到overflow 可以加入进来
            // 现在已经有两行了，只能选择删除后面的emoji
            currentWidth = (currentWidth - currentRow.last().width - horizontalPx.roundToPx()).coerceAtLeast(0)
            currentRow.removeAt(currentRow.size - 1)
            emojiUsed--
        }

        val visible =
            subcompose("visible") {
                repeat(emojiUsed.coerceAtLeast(1)) { index -> content(index, index < emojiUsed) }
                overflow(emojiUsed)
            }.map { it.measure(constraints) }
        var visibleIndex = 0
        val rows =
            firstMeasureResult.first.map { measuredRow ->
                List(measuredRow.size) { visible[visibleIndex++] }
            }
        check(visibleIndex == visible.size) { "Reaction layout candidate and visible control counts differ" }
        val height =
            rows.sumOf {
                it.maxOf { placeable -> placeable.height }
            } + verticalPx.roundToPx() * (firstMeasureResult.first.size - 1)

        layout(maxWidth, height) {
            placeEmojiRows(rows, maxWidth, alignEnd, horizontalPx.roundToPx(), verticalPx.roundToPx())
        }
    }
}

private fun Placeable.PlacementScope.placeEmojiRows(
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

private fun SubcomposeMeasureScope.measureFirstStage(
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

@Composable
private fun EmojiCell(info: ReactionInfo, topicInfo: TopicDetail) {
    val topicId = topicInfo.id
    val emoji = info.emoji
    val hasReacted = info.hasReacted
    val globalTask = LocalGlobalTask.current
    val key = "$topicId $emoji"
    val loadingState = globalTask.stateMap[key]

    Pill(
        "${info.count} ${
            if (loadingState is LoadingState.Loading) {
                "*"
            } else {
                ""
            }
        }",
        emoji = emoji,
        selected = hasReacted,
    ) {
        globalTask.launch(key) {
            use {
                if (hasReacted) {
                    deleteReaction(topicInfo, emoji, info)
                } else {
                    addReaction(topicInfo, emoji)
                }
            }
        }
    }
}
