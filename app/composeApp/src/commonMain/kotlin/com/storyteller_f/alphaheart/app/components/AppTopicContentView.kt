/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.mikepenz.markdown.compose.components.MarkdownComponentModel
import com.storyteller_f.alphaheart.app.LocalAppNavFactory
import com.storyteller_f.alphaheart.app.pages.community.CommunityTopicContent
import com.storyteller_f.alphaheart.app.pages.community.topicCommunityId
import com.storyteller_f.alphaheart.app.pages.topic.TopicRoute
import com.storyteller_f.alphaheart.client.compose_core.components.TopicContentView
import com.storyteller_f.shared.model.TopicDetail
import com.storyteller_f.shared.type.PrimaryKey
import com.storyteller_f.shared.utils.readCodeFence

@Composable
fun AppTopicContentView(topicInfo: TopicDetail, isEmbed: Boolean = false, communityId: PrimaryKey? = null) {
    val appNavFactory = LocalAppNavFactory.current
    val ownerCommunity = communityId ?: topicCommunityId(topicInfo)
    CommunityTopicContent(ownerCommunity) {
        TopicContentView(topicInfo.topicInfo, {
            appNavFactory.newAppNav().gotoMedia(it)
        }, isEmbed, {
            RefBlock(it)
        })
    }
}

@Composable
fun RefBlock(modal: MarkdownComponentModel) {
    val (first, second) =
        remember(modal.node, modal.content) {
            val textInNode = readCodeFence(modal.node, modal.content)
            TopicRoute.parseRefUri(textInNode)
        }
    first?.let { it1 -> it1(second) }
}
