/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.app.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.storyteller_f.alphaheart.app.Res
import com.storyteller_f.alphaheart.app.font_loaded
import com.storyteller_f.alphaheart.app.pages.community.CommunityFontState
import com.storyteller_f.alphaheart.app.pages.community.LocalCommunityFontStore
import com.storyteller_f.alphaheart.app.pages.community.fontFileKey
import com.storyteller_f.alphaheart.app.retry_font_loading
import com.storyteller_f.alphaheart.app.ui.MaterialSymbolsOutlined
import com.storyteller_f.shared.model.FileInfo
import dev.tclement.fonticons.FontIcon
import org.jetbrains.compose.resources.stringResource

private enum class FontLoadStatus { LOADING, READY, FAILED }

@Composable
fun FontView(info: FileInfo) {
    val store = LocalCommunityFontStore.current
    val fileKey = fontFileKey(info)
    val states = remember(store, fileKey) { store.observeFont(info) }
    val state by states.collectAsState(CommunityFontState.Loading)
    val status =
        when (state) {
            CommunityFontState.Loading -> FontLoadStatus.LOADING
            is CommunityFontState.Ready -> FontLoadStatus.READY
            is CommunityFontState.Failed -> FontLoadStatus.FAILED
        }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
    ) {
        FontIcon(MaterialSymbolsOutlined.FontDownload, null)
        Text(info.name, modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()))
        when (status) {
            FontLoadStatus.LOADING -> CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)

            FontLoadStatus.READY ->
                FontIcon(
                    MaterialSymbolsOutlined.DownloadDone,
                    stringResource(Res.string.font_loaded),
                )

            FontLoadStatus.FAILED ->
                IconButton(onClick = { store.retry(info) }) {
                    FontIcon(MaterialSymbolsOutlined.Refresh, stringResource(Res.string.retry_font_loading))
                }
        }
    }
}
