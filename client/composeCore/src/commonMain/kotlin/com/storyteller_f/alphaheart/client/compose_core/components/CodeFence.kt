/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.hrm.latex.renderer.Latex
import com.hrm.latex.renderer.model.LatexConfig
import com.mikepenz.markdown.compose.LocalMarkdownTypography
import com.mikepenz.markdown.compose.components.MarkdownComponentModel
import com.mikepenz.markdown.compose.elements.highlightedCodeFence
import com.storyteller.alphaheart.client.composecore.markdown.MermaidBlock
import com.storyteller_f.alphaheart.client.compose_core.Res
import com.storyteller_f.alphaheart.client.compose_core.binary_file
import com.storyteller_f.alphaheart.client.compose_core.icons.ActionGlyph
import com.storyteller_f.alphaheart.client.compose_core.markdown.CodeFenceKind
import com.storyteller_f.alphaheart.client.compose_core.markdown.codeFenceKind
import com.storyteller_f.alphaheart.client.compose_core.media.LocalMediaPreviewAction
import com.storyteller_f.alphaheart.client.compose_core.media.LocalResourceDownloadFile
import com.storyteller_f.alphaheart.client.compose_core.media.RemoteMediaItem
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.shared.commonJson
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.utils.MarkdownObject
import com.storyteller_f.shared.utils.getLang
import com.storyteller_f.shared.utils.readCodeFence
import nl.jacobras.humanreadable.HumanReadable
import org.jetbrains.compose.resources.stringResource
import kotlin.collections.get

@Composable
fun CustomCodeFence(
    modal: MarkdownComponentModel,
    mediaList: Map<String, FileInfo>,
    refBlock: @Composable (MarkdownComponentModel) -> Unit,
    onClick: (FileInfo) -> Unit,
) {
    val lang =
        remember(modal.node, modal.content) {
            getLang(modal.node, modal.content)
        }
    when (codeFenceKind(lang)) {
        CodeFenceKind.REF -> refBlock(modal)
        CodeFenceKind.MATH -> LatexBlock(modal)
        CodeFenceKind.MERMAID -> MermaidBlock(modal)
        CodeFenceKind.ASCIIDOC -> AsciidocPreviewBlock(modal)
        CodeFenceKind.OBJECT -> ObjectBlock(modal, mediaList, onClick)
        CodeFenceKind.CODE -> HighlightCodeBlock(modal)
    }
}

@Composable
fun ObjectBlock(modal: MarkdownComponentModel, mediaList: Map<String, FileInfo>, onClick: (FileInfo) -> Unit) {
    val obj =
        remember(modal.node, modal.content) {
            val c = readCodeFence(modal.node, modal.content)
            commonJson.decodeFromString<MarkdownObject>(c)
        }
    if (mediaList.containsKey(obj.name)) {
        FileObjectBlock(obj, modal, mediaList, onClick)
    } else {
        HighlightCodeBlock(modal)
    }
}

@Composable
private fun FileObjectBlock(
    obj: MarkdownObject,
    modal: MarkdownComponentModel,
    mediaMap: Map<String, FileInfo>,
    onClick: (FileInfo) -> Unit,
) {
    val mediaInfo = mediaMap[obj.name] ?: return HighlightCodeBlock(modal)
    val url = mediaInfo.url
    val contentType = mediaInfo.contentType
    if (contentType.isBlank() || url.isBlank()) {
        HighlightCodeBlock(modal)
        return
    }
    if (!contentType.startsWith("audio") && !contentType.startsWith("video/")) {
        UploadedFileAction(mediaInfo, onClick)
        return
    }
    val coverInfo = mediaMap[obj.cover]
    val obj1 =
        RemoteMediaItem(
            id = mediaInfo.id.toString(),
            url = url,
            contentType = contentType,
            name = obj.name,
            cover = coverInfo,
            title = obj.title,
        )
    CompositionLocalProvider(
        LocalMediaPreviewAction provides { onClick(mediaInfo) },
        LocalResourceDownloadFile provides mediaInfo,
    ) {
        if (contentType.startsWith("video/")) {
            VideoViewEmbed(obj1)
        } else {
            AudioViewEmbed(obj1, loadOnPlay = true)
        }
    }
}

@Composable
private fun UploadedFileAction(info: FileInfo, onClick: (FileInfo) -> Unit) {
    val pdf = info.contentType == FileInfo.PDF_CONTENT_TYPE
    val type =
        when (info.contentType) {
            "application/octet-stream" -> stringResource(Res.string.binary_file)
            else -> info.contentType
        }
    val open = { onClick(info) }
    DocumentMarker(
        title = info.name,
        details = if (pdf) "PDF" else "$type · ${HumanReadable.fileSize(info.size)}",
        glyph = if (pdf) ActionGlyph.FILE_TEXT_RTL else ActionGlyph.FILE,
        modifier = if (pdf) Modifier.appiumSemantics(testTag = "uploaded-pdf-attachment") else Modifier,
        onOpen = open,
        openDescription = "open file ${info.name}",
    )
}

@Composable
fun HighlightCodeBlock(modal: MarkdownComponentModel) {
    highlightedCodeFence(modal)
}

@Composable
private fun LatexBlock(modal: MarkdownComponentModel) {
    val typography = LocalMarkdownTypography.current
    val textStyle = typography.code
    Latex(
        latex = readCodeFence(modal.node, modal.content),
        config = LatexConfig(fontSize = textStyle.fontSize),
    )
}
