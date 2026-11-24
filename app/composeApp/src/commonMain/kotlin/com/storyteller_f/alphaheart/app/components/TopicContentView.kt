/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.compose.components.MarkdownComponent
import com.mikepenz.markdown.model.State
import com.storyteller_f.alphaheart.app.topic.topicContentLabel
import com.storyteller_f.alphaheart.client.compose_core.CoreStrings
import com.storyteller_f.alphaheart.client.compose_core.components.CustomCodeFence
import com.storyteller_f.alphaheart.client.compose_core.components.CustomMarkdown
import com.storyteller_f.alphaheart.client.compose_core.markdown.CustomCoil3ImageTransformerImpl
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.model.TopicContent
import com.storyteller_f.shared.model.TopicInfo
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap

@Composable
fun TopicContentView(
    topicInfo: TopicInfo,
    onClick: (FileInfo) -> Unit,
    isEmbed: Boolean,
    refBlock: MarkdownComponent,
    parsed: State? = null,
    blockIndex: Int? = null,
) {
    val content = topicInfo.content
    if (content is TopicContent.DecryptFailed) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(content.message)
        }
        return
    }
    if (content is TopicContent.Encrypted) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(CoreStrings.permission_denied())
        }
        return
    }
    val (plain, fileInfos) =
        when (content) {
            is TopicContent.Plain -> content.plain to content.fileInfos
            is TopicContent.Extracted -> content.plain to content.fileInfos
            else -> return
        }
    val mediaMap =
        remember(fileInfos.toImmutableList()) {
            fileInfos.toImmutableList().associateBy { it.name }.toImmutableMap()
        }
    val imageTransformer =
        remember(mediaMap) {
            CustomCoil3ImageTransformerImpl(mediaMap, onClick)
        }
    val dimensionMap =
        remember(mediaMap) {
            mediaMap.mapValues { it.value.dimension }.toImmutableMap()
        }
    Box(
        modifier =
        Modifier.appiumSemantics(
            text = topicContentLabel(plain, parsed, blockIndex),
        ),
    ) {
        CustomMarkdown(plain, isEmbed, imageTransformer, dimensionMap, parsed, blockIndex) {
            CustomCodeFence(it, mediaMap, refBlock, onClick)
        }
    }
}
