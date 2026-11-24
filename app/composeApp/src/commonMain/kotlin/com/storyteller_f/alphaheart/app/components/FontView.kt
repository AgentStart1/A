/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.font_loaded
import com.storyteller_f.alphaheart.app.fonts.CommunityFontState
import com.storyteller_f.alphaheart.app.fonts.FontLoadStatus
import com.storyteller_f.alphaheart.app.fonts.LocalCommunityFontStore
import com.storyteller_f.alphaheart.app.fonts.fontFileKey
import com.storyteller_f.alphaheart.app.retry_font_loading
import com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon
import com.storyteller_f.shared.model.FileInfo
import org.jetbrains.compose.resources.stringResource
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIconButton as IconButton

@Composable
fun FontView(info: FileInfo) {
    val store = LocalCommunityFontStore.current
    if (store == null) {
        Text(info.name)
        return
    }
    val fileKey = fontFileKey(info)
    val states = remember(store, fileKey) { store.observeFont(info) }
    val state by states.collectAsState(CommunityFontState.Loading)
    val status =
        when (state) {
            CommunityFontState.Loading -> FontLoadStatus.LOADING
            is CommunityFontState.Ready -> FontLoadStatus.READY
            is CommunityFontState.Failed -> FontLoadStatus.FAILED
        }
    FontStatusRow(info.name, status) { store.retry(info) }
}

@Composable
internal fun FontStatusRow(name: String, status: FontLoadStatus, onRetry: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
    ) {
        Icon(actionIcon(ActionGlyph.TYPE), null, Modifier.size(17.dp).testTag("font-type-icon"))
        Text(name, modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()))
        Box(Modifier.size(44.dp).testTag("font-status-slot"), contentAlignment = Alignment.Center) {
            when (status) {
                FontLoadStatus.LOADING ->
                    CircularProgressIndicator(
                        Modifier.size(17.dp).testTag("font-state-icon"),
                        strokeWidth = 2.dp,
                    )

                FontLoadStatus.READY ->
                    Icon(
                        actionIcon(ActionGlyph.CHECK),
                        stringResource(Res.string.font_loaded),
                        modifier = Modifier.size(17.dp).testTag("font-state-icon"),
                    )

                FontLoadStatus.FAILED ->
                    IconButton(onRetry, Modifier.testTag("font-status-retry")) {
                        Icon(
                            actionIcon(ActionGlyph.REFRESH_CW),
                            stringResource(Res.string.retry_font_loading),
                            modifier = Modifier.size(17.dp).testTag("font-state-icon"),
                        )
                    }
            }
        }
    }
}
