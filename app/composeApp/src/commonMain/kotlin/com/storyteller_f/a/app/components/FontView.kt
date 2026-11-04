/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.a.app.components

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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.storyteller_f.a.app.Res
import com.storyteller_f.a.app.font_loaded
import com.storyteller_f.a.app.pages.community.LocalCommunityFontStore
import com.storyteller_f.a.app.pages.community.fontFileKey
import com.storyteller_f.a.app.pages.community.observeCommunityFont
import com.storyteller_f.a.app.retry_font_loading
import com.storyteller_f.a.app.ui.MaterialSymbolsOutlined
import com.storyteller_f.shared.model.FileInfo
import dev.tclement.fonticons.FontIcon
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.stringResource

private enum class FontLoadStatus { LOADING, READY, FAILED }

@Composable
fun FontView(info: FileInfo) {
    val store = LocalCommunityFontStore.current
    val fileKey = fontFileKey(info)
    var retry by remember(store, fileKey) { mutableStateOf(0) }
    val status =
        key(store, fileKey, retry) {
            val statuses =
                remember {
                    observeCommunityFont(info, store, retry = retry > 0).map {
                        if (it.family != null) FontLoadStatus.READY else FontLoadStatus.FAILED
                    }
                }
            val current by statuses.collectAsState(FontLoadStatus.LOADING)
            current
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
                IconButton(onClick = { retry++ }) {
                    FontIcon(MaterialSymbolsOutlined.Refresh, stringResource(Res.string.retry_font_loading))
                }
        }
    }
}
