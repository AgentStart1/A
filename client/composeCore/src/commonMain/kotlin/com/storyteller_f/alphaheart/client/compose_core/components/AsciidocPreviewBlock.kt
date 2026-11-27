/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.mikepenz.markdown.compose.components.MarkdownComponentModel
import com.storyteller_f.alphaheart.client.asciidoc_parser.buildAsciidocPreviewHtml
import com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.icons.actionIcon
import com.storyteller_f.alphaheart.client.compose_core.notifications.LocalToaster
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.alphaheart.client.compose_core.utils.copyText
import com.storyteller_f.shared.utils.readCodeFence
import dev.nucleusframework.webview.web.WebView
import dev.nucleusframework.webview.web.rememberWebViewStateWithHTMLData
import kotlinx.coroutines.launch
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingDialog as Dialog
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIcon as Icon
import com.storyteller_f.alphaheart.client.compose_core.components.ReadingIconButton as IconButton

@Composable
fun AsciidocPreviewBlock(modal: MarkdownComponentModel) {
    val source =
        remember(modal.node, modal.content) {
            readCodeFence(modal.node, modal.content)
        }
    val scope = rememberCoroutineScope()
    var previewHtml by remember { mutableStateOf<String?>(null) }
    fun openPreview() {
        scope.launch {
            previewHtml = buildAsciidocPreviewHtml(source)
        }
    }
    AsciidocPreviewCard(source, ::openPreview)
    previewHtml?.let { preview ->
        val previewTitle =
            source.lineSequence()
                .firstOrNull { it.startsWith("= ") }
                ?.removePrefix("= ")
                .orEmpty()
        AsciidocPreviewDialog(
            preview,
            previewTitle,
            onDismissRequest = { previewHtml = null },
        )
    }
}

@Composable
private fun AsciidocPreviewCard(source: String, openPreview: () -> Unit) {
    val scope = rememberCoroutineScope()
    val toasterState = LocalToaster.current
    val clipboard = LocalClipboard.current
    val firstLine = source.lineSequence().firstOrNull().orEmpty()
    val title = firstLine.takeIf { it.startsWith("= ") }?.removePrefix("= ")
    DocumentMarker(
        title = title ?: "AsciiDoc",
        details = firstLine,
        glyph = ActionGlyph.FILE_TEXT_RTL,
        modifier = Modifier.appiumSemantics(description = "asciidoc", text = "AsciiDoc preview", onClick = openPreview),
        onOpen = openPreview,
        openDescription = "open",
        trailing = {
            IconButton({
                scope.launch { clipboard.copyText(source, toasterState) }
            }) {
                Icon(actionIcon(ActionGlyph.COPY), "copy source", Modifier.size(16.dp))
            }
        },
    )
}

@Composable
private fun AsciidocPreviewDialog(html: String, previewTitle: String, onDismissRequest: () -> Unit) {
    val state = rememberWebViewStateWithHTMLData(data = html, mimeType = "text/html")
    state.webSettings.isJavaScriptEnabled = true
    state.webSettings.desktopWebSettings.incognito = true
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier =
            Modifier.fillMaxWidth().fillMaxSize().appiumSemantics(
                description = "asciidoc-preview",
                text = previewTitle,
            ),
        ) {
            Scaffold(
                topBar = {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = onDismissRequest) {
                                Icon(actionIcon(ActionGlyph.ARROW_LEFT_RTL), "close preview", Modifier.size(16.dp))
                            }
                            Text("AsciiDoc preview")
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                },
            ) { paddingValues ->
                WebView(
                    state = state,
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                )
            }
        }
    }
}
