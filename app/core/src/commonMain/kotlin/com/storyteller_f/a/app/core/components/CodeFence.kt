package com.storyteller_f.a.app.core.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.eygraber.uri.Uri
import com.hrm.latex.renderer.Latex
import com.hrm.latex.renderer.model.LatexConfig
import com.mikepenz.markdown.compose.LocalMarkdownTypography
import com.mikepenz.markdown.compose.components.MarkdownComponentModel
import com.mikepenz.markdown.compose.elements.highlightedCodeFence
import com.storyteller_f.shared.commonJson
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.utils.MarkdownObject
import com.storyteller_f.shared.utils.getLang
import com.storyteller_f.shared.utils.readCodeFence
import kotlin.collections.get

@Composable
fun CustomCodeFence(
    modal: MarkdownComponentModel,
    mediaList: Map<String, FileInfo>,
    refBlock: @Composable (MarkdownComponentModel) -> Unit,
    onClick: (FileInfo) -> Unit
) {
    val lang = remember(modal.node, modal.content) {
        getLang(modal.node, modal.content)
    }
    when {
        listOf("com.storyteller_f.a", "c.s.a", "csa").contains(lang) -> refBlock(modal)

        lang == "math" -> LatexBlock(modal)

        lang == "object" -> ObjectBlock(modal, mediaList, onClick)

        else -> HighlightCodeBlock(modal)
    }
}

@Composable
fun ObjectBlock(
    modal: MarkdownComponentModel,
    mediaList: Map<String, FileInfo>,
    onClick: (FileInfo) -> Unit
) {
    val obj = remember(modal.node, modal.content) {
        val c = readCodeFence(modal.node, modal.content)
        commonJson.decodeFromString<MarkdownObject>(c)
    }
    if (obj.contentType.isNullOrBlank()) {
        FileObjectBlock(obj, modal, mediaList, onClick)
    } else {
        CustomObjectBlock(obj, modal, mediaList)
    }
}

@Composable
private fun FileObjectBlock(
    obj: MarkdownObject,
    modal: MarkdownComponentModel,
    mediaMap: Map<String, FileInfo>,
    onClick: (FileInfo) -> Unit
) {
    val mediaInfo = mediaMap[obj.name] ?: return HighlightCodeBlock(modal)
    val url = mediaInfo.url
    val contentType = mediaInfo.contentType
    if (contentType.isBlank() || url.isBlank()) {
        HighlightCodeBlock(modal)
        return
    }
    if (contentType == FileInfo.PDF_CONTENT_TYPE) {
        PdfViewBlock(mediaInfo, onClick)
        return
    }
    if (!contentType.startsWith("audio") && !contentType.startsWith("video/")) {
        HighlightCodeBlock(modal)
        return
    }
    val coverInfo = mediaMap[obj.cover]
    val obj1 = RemoteMediaItem(mediaInfo.id.toString(), url, contentType, false, obj.name, coverInfo, obj.title)
    if (contentType.startsWith("video/")) {
        VideoViewEmbed(obj1)
    } else if (contentType.startsWith("audio/")) {
        AudioViewEmbed(obj1)
    }
}

@Composable
private fun CustomObjectBlock(
    obj: MarkdownObject,
    modal: MarkdownComponentModel,
    mediaList: Map<String, FileInfo>
) {
    val name = when (obj.contentType) {
        FileInfo.YOUTUBE_MIMETYPE -> {
            "Youtube:${Uri.parse(obj.url).getQueryParameter("v")}"
        }

        FileInfo.SOUND_CLOUD_MIME_TYPE -> {
            "SoundCloud:${Uri.parse(obj.url).lastPathSegment}"
        }

        FileInfo.M3U8_MIMETYPE -> {
            "M3U:${Uri.parse(obj.url).lastPathSegment}"
        }

        else -> {
            HighlightCodeBlock(modal)
            return
        }
    }
    val mediaItem = RemoteMediaItem(
        obj.url,
        obj.url,
        FileInfo.YOUTUBE_MIMETYPE,
        false,
        name,
        mediaList[obj.cover],
        obj.title
    )
    when (obj.contentType) {
        FileInfo.YOUTUBE_MIMETYPE -> {
            VideoViewEmbed(mediaItem)
        }

        FileInfo.SOUND_CLOUD_MIME_TYPE -> {
            AudioViewEmbed(mediaItem)
        }

        FileInfo.M3U8_MIMETYPE -> {
            VideoViewEmbed(mediaItem)
        }
    }
}

@Composable
fun HighlightCodeBlock(
    modal: MarkdownComponentModel
) {
    highlightedCodeFence(modal)
}

@Composable
private fun LatexBlock(
    modal: MarkdownComponentModel
) {
    val typography = LocalMarkdownTypography.current
    val textStyle = typography.code
    Latex(
        latex = readCodeFence(modal.node, modal.content),
        config = LatexConfig(fontSize = textStyle.fontSize)
    )
}
