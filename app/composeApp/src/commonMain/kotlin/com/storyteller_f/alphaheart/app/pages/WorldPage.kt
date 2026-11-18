/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages

import androidx.compose.runtime.Composable
import com.storyteller_f.alphaheart.app.common.createWorldViewModel
import com.storyteller_f.alphaheart.app.pages.topic.TopicList

@Composable
fun WorldPage() {
    TopicList(createWorldViewModel())
}
