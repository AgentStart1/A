/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.pages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.common.createWorldViewModel
import com.storyteller_f.alphaheart.app.pages.topic.TopicList
import com.storyteller_f.alphaheart.app.world
import org.jetbrains.compose.resources.stringResource

@Composable
fun WorldPage() {
    val viewModel = createWorldViewModel()
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(Modifier.widthIn(max = 740.dp).fillMaxSize()) {
            Text(
                stringResource(Res.string.world),
                Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                style = MaterialTheme.typography.headlineSmall,
            )
            Box(Modifier.weight(1f)) { TopicList(viewModel) }
        }
    }
}
