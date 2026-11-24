/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.utils

import com.storyteller_f.shared.type.PrimaryKey

internal data class TopicReadingAnchor(val topicId: PrimaryKey?, val index: Int, val offset: Int)
