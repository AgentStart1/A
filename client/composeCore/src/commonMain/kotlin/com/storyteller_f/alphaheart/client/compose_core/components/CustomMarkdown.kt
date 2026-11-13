/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import com.hrm.latex.renderer.measure.rememberLatexMeasurer
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.compose.components.MarkdownComponent
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.highlightedCodeBlock
import com.mikepenz.markdown.compose.extendedspans.ExtendedSpans
import com.mikepenz.markdown.compose.extendedspans.RoundedCornerSpanPainter
import com.mikepenz.markdown.compose.extendedspans.SquigglyUnderlineSpanPainter
import com.mikepenz.markdown.compose.extendedspans.rememberSquigglyUnderlineAnimator
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.model.ImageData
import com.mikepenz.markdown.model.ImageTransformer
import com.mikepenz.markdown.model.markdownAnnotator
import com.mikepenz.markdown.model.markdownExtendedSpans
import com.mikepenz.markdown.model.markdownInlineContent
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.storyteller_f.alphaheart.client.compose_core.common.LocalClient
import com.storyteller_f.alphaheart.client.compose_core.utils.appiumSemantics
import com.storyteller_f.alphaheart.client.compose_core.utils.imageRequest
import com.storyteller_f.shared.model.Dimension
import com.storyteller_f.shared.model.FileInfo
import kotlinx.collections.immutable.ImmutableMap

@Composable
fun CustomMarkdown(
    plain: String,
    isEmbed: Boolean,
    imageTransformer: ImageTransformer,
    dimensionMap: ImmutableMap<String, Dimension?>,
    codeFence: MarkdownComponent,
) {
    val inlineContentMap = remember { mutableMapOf<String, InlineTextContent>() }
    val density = LocalDensity.current
    val latexMeasurer = rememberLatexMeasurer()
    BoxWithConstraints {
        val maxWidth = this.maxWidth
        CompositionLocalProvider(LocalInspectionMode provides true) {
            val colors = markdownColor()
            val typography = customMarkdownTypography(colors)
            val annotator =
                markdownAnnotator { content, child ->
                    imageAnnotator(
                        child = child,
                        content = content,
                        inlineContentMap = inlineContentMap,
                        dimensionMap = dimensionMap,
                        maxWidth = maxWidth,
                        density = density,
                        isEmbed = isEmbed,
                        imageTransformer = imageTransformer,
                        typography = typography,
                        colors = colors,
                        latexMeasurer = latexMeasurer,
                    )
                }
            Markdown(
                content = plain,
                modifier = Modifier.fillMaxWidth(),
                colors = colors,
                typography = typography,
                imageTransformer = imageTransformer,
                components = markdownComponents(codeFence = codeFence, codeBlock = highlightedCodeBlock),
                annotator = annotator,
                inlineContent = markdownInlineContent(inlineContentMap),
                extendedSpans =
                markdownExtendedSpans {
                    val animator = rememberSquigglyUnderlineAnimator()
                    remember {
                        ExtendedSpans(RoundedCornerSpanPainter(), SquigglyUnderlineSpanPainter(animator = animator))
                    }
                },
            )
        }
    }
}

class CustomCoil3ImageTransformerImpl(private val mediaMap: Map<String, FileInfo>, val onClick: (FileInfo) -> Unit) :
    ImageTransformer {
    @OptIn(ExperimentalRichTextApi::class)
    @Composable
    override fun transform(link: String): ImageData =
        if (link.startsWith("file:///")) {
            val model = link.substring(7)
            val painter = rememberAsyncImagePainter(model = model)
            ImageData(painter)
        } else {
            val info = mediaMap[link]
            val model = imageRequestInMarkdown(info)
            val painter = rememberAsyncImagePainter(model = model)
            ImageData(
                painter,
                modifier =
                Modifier.appiumSemantics(
                    description = info?.let { "open file ${it.name}" },
                    onClick = info?.let { { onClick(it) } },
                ).clip(RoundedCornerShape(10.dp)).clickable(info != null) {
                    info?.let { it1 -> onClick(it1) }
                },
            )
        }
}

@Composable
fun imageRequestInMarkdown(info: FileInfo?): ImageRequest {
    val client = LocalClient.current
    val context = LocalPlatformContext.current
    return imageRequest(context, client, info).build()
}
