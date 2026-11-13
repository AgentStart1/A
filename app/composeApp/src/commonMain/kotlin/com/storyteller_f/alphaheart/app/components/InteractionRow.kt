/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.outlined.AddReaction
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.SubcomposeMeasureScope
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.LocalGlobalTask
import com.storyteller_f.alphaheart.app.LocalSessionManager
import com.storyteller_f.alphaheart.app.pages.topic.addReaction
import com.storyteller_f.alphaheart.app.pages.topic.deleteReaction
import com.storyteller_f.alphaheart.client.compose_core.components.Pill
import com.storyteller_f.alphaheart.client.core.LoadingState
import com.storyteller_f.shared.model.ReactionInfo
import com.storyteller_f.shared.model.TopicDetail

@Composable
fun InteractionRow(topicInfo: TopicDetail, startAddReaction: () -> Unit, startAddComment: () -> Unit) {
    val reactions = topicInfo.reactions
    InteractionRowInternal(reactions, topicInfo, startAddComment, startAddReaction)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractionRowInternal(
    data: List<ReactionInfo>,
    topicInfo: TopicDetail,
    startAddComment: () -> Unit,
    startAddReaction: () -> Unit,
) {
    val appNavFactory = LocalAppNavFactory.current
    val hasComment = topicInfo.hasComment
    val commentCount = topicInfo.commentCount
    val userSessionManager = LocalSessionManager.current
    EmojiRow(
        data,
        {
            if (it < data.size) {
                Pill(text = "+${data.size - it}") {
                    appNavFactory.newAppNav().gotoReactionListPage(topicInfo.id)
                }
            }
            Pill(icon = Icons.Outlined.AddReaction) {
                if (userSessionManager.currentIsAlreadySignUp) {
                    startAddReaction()
                } else {
                    appNavFactory.newAppNav().requestSignIn()
                }
            }
        },
        { index ->
            if (index == 0) {
                Pill(
                    commentCount.toString(),
                    selected = hasComment,
                    icon = Icons.AutoMirrored.Outlined.Comment,
                ) {
                    startAddComment()
                }
            }
            if (data.isNotEmpty()) {
                data.getOrNull(index)?.let { info ->
                    EmojiCell(info, topicInfo)
                }
            }
        },
    )
}

@Composable
private fun EmojiRow(
    data: List<ReactionInfo>,
    overflow: @Composable (Int) -> Unit,
    content: @Composable (Int) -> Unit,
) {
    val verticalPx = 4.dp
    val horizontalPx = 8.dp
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
                    overflow(emojiUsed)
                }.map {
                    it.measure(constraints)
                }
            val spacing = horizontalPx.roundToPx() * (overflowPlaceable.size - 1)
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
            currentWidth -= currentRow.last().width + horizontalPx.roundToPx()
            currentRow.removeAt(currentRow.size - 1)
            emojiUsed--
        }

        val height =
            firstMeasureResult.first.sumOf {
                it.maxOf { placeable -> placeable.height }
            } + verticalPx.roundToPx() * (firstMeasureResult.first.size - 1)

        layout(maxWidth, height) {
            var yOffset = 0
            firstMeasureResult.first.forEach { row ->
                var xOffset = 0
                row.forEach {
                    it.placeRelative(xOffset, yOffset)
                    xOffset += it.width + horizontalPx.roundToPx()
                }
                yOffset += row.maxOf { it.height } + verticalPx.roundToPx()
            }
        }
    }
}

private fun SubcomposeMeasureScope.measureFirstStage(
    data: List<ReactionInfo>,
    constraints: Constraints,
    maxWidth: Int,
    horizontalPx: Dp,
    content: @Composable (Int) -> Unit,
): Triple<MutableList<MutableList<Placeable>>, Int, Int> {
    val rows = mutableListOf<MutableList<Placeable>>()
    var currentRow: List<Placeable> = emptyList()
    var currentWidth = 0
    var index = 0
    var emojiUsedCount = 0

    while (index < data.size.coerceAtLeast(1) && rows.size < 2) {
        val placeableList =
            subcompose("emoji_$index") {
                content(index)
            }.map {
                it.measure(constraints)
            }
        val newWidth =
            placeableList.sumOf {
                it.width
            } + horizontalPx.roundToPx() * (placeableList.size - 1)

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
                } + horizontalPx.roundToPx() * (placeableList.size - 1)
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
