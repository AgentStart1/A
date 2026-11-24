/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.client.compose_core.components.CachedRefreshOverlay
import com.storyteller_f.alphaheart.client.compose_core.components.ExceptionView
import com.storyteller_f.alphaheart.client.core.LoadingHandler
import com.storyteller_f.alphaheart.client.core.LoadingState

@Composable
fun <T : Any> RefCellStateView(
    handler: LoadingHandler<T>,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    val data by handler.data.collectAsState()
    val state by handler.state.collectAsState()
    Box(modifier = modifier) {
        data.let { value ->
            if (value != null) {
                content(value)
                CachedRefreshOverlay(state, handler::refresh)
            } else {
                val localState = state
                if (localState is LoadingState.Error) {
                    Box(
                        modifier =
                        Modifier.fillMaxSize().clickable {
                            handler.refresh()
                        }.padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        ExceptionView(localState.e)
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}
