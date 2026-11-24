/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.previews

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.storyteller_f.shared.model.TopicContent
import com.storyteller_f.shared.model.TopicDetail
import com.storyteller_f.shared.model.UserSubscriptionItem

class UserSubscriptionPreviewProvider : PreviewParameterProvider<UserSubscriptionItem> {
    override val values: Sequence<UserSubscriptionItem>
        get() =
            sequenceOf(
                UserSubscriptionItem.EMPTY.copy(
                    topicInfo =
                    TopicDetail.EMPTY.copy(
                        topicInfo = TopicDetail.EMPTY.topicInfo.copy(content = TopicContent.Plain("hello")),
                    ),
                ),
            )
}
