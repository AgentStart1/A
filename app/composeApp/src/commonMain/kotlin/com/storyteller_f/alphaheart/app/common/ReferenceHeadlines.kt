/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.common

import com.storyteller_f.shared.model.TopicContent
import com.storyteller_f.shared.model.TopicDetail
import com.storyteller_f.shared.utils.extractMarkdownHeadline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext

/** Projects existing cached topic data without parsing Markdown on the native rendering thread. */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun referenceHeadlines(topics: Flow<TopicDetail?>): Flow<String?> =
    topics.mapLatest { topic ->
        withContext(Dispatchers.Default) {
            when (val content = topic?.content) {
                null -> null
                is TopicContent.Extracted -> content.plain
                is TopicContent.Plain -> extractMarkdownHeadline(content.plain)
                else -> ""
            }
        }
    }
