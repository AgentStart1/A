/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.hrm.latex.renderer.measure.rememberLatexMeasurer
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.compose.MarkdownElement
import com.mikepenz.markdown.compose.MarkdownSuccess
import com.mikepenz.markdown.compose.components.MarkdownComponent
import com.mikepenz.markdown.compose.components.MarkdownComponentModel
import com.mikepenz.markdown.compose.components.MarkdownComponents
import com.mikepenz.markdown.compose.components.markdownComponents
import com.mikepenz.markdown.compose.elements.MarkdownTable
import com.mikepenz.markdown.compose.elements.MarkdownTableHeader
import com.mikepenz.markdown.compose.elements.MarkdownTableRow
import com.mikepenz.markdown.compose.elements.highlightedCodeBlock
import com.mikepenz.markdown.compose.extendedspans.ExtendedSpans
import com.mikepenz.markdown.compose.extendedspans.RoundedCornerSpanPainter
import com.mikepenz.markdown.compose.extendedspans.SquigglyUnderlineSpanPainter
import com.mikepenz.markdown.compose.extendedspans.rememberSquigglyUnderlineAnimator
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.model.ImageTransformer
import com.mikepenz.markdown.model.State
import com.mikepenz.markdown.model.markdownAnimations
import com.mikepenz.markdown.model.markdownAnnotator
import com.mikepenz.markdown.model.markdownExtendedSpans
import com.mikepenz.markdown.model.markdownInlineContent
import com.mikepenz.markdown.model.rememberMarkdownState
import com.storyteller_f.alphaheart.client.compose_core.common.LocalClient
import com.storyteller_f.alphaheart.client.compose_core.markdown.MarkdownInlineMath
import com.storyteller_f.alphaheart.client.compose_core.markdown.MarkdownInlineOptions
import com.storyteller_f.alphaheart.client.compose_core.markdown.imageAnnotator
import com.storyteller_f.alphaheart.client.compose_core.utils.imageRequest
import com.storyteller_f.shared.model.Dimension
import com.storyteller_f.shared.model.FileInfo
import com.storyteller_f.shared.utils.normalizeMarkdownLineEndings
import kotlinx.collections.immutable.ImmutableMap

@Composable
fun CustomMarkdown(
    plain: String,
    isEmbed: Boolean,
    imageTransformer: ImageTransformer,
    dimensionMap: ImmutableMap<String, Dimension?>,
    parsed: State? = null,
    blockIndex: Int? = null,
    codeFence: MarkdownComponent,
) {
    // Parsed nodes own embedded controls; a width-only remeasurement must not replace them.
    val state = parsed ?: RememberParsedMarkdown(plain)
    val inlineContentMap = remember { mutableMapOf<String, InlineTextContent>() }
    val density = LocalDensity.current
    val latexMeasurer = rememberLatexMeasurer()
    BoxWithConstraints {
        val maxWidth = this.maxWidth
        CompositionLocalProvider(LocalInspectionMode provides true) {
            val colors = markdownColor()
            val typography = customMarkdownTypography(colors)
            val inlineOptions =
                MarkdownInlineOptions(
                    dimensionMap = dimensionMap,
                    imageTransformer = imageTransformer,
                    maxWidth = maxWidth,
                    density = density,
                    isEmbed = isEmbed,
                    math = MarkdownInlineMath(typography, colors, latexMeasurer),
                )
            val annotator =
                markdownAnnotator { content, child ->
                    imageAnnotator(child, content, inlineContentMap, inlineOptions)
                }
            Markdown(
                state = state,
                modifier = Modifier.fillMaxWidth(),
                colors = colors,
                typography = typography,
                imageTransformer = imageTransformer,
                animations = markdownAnimations(animateTextSize = { this }),
                components =
                markdownComponents(
                    codeFence = codeFence,
                    codeBlock = highlightedCodeBlock,
                    table = { ReadingMarkdownTable(it) },
                ),
                success = { result, components, modifier ->
                    MarkdownDocumentContent(result, components, modifier, blockIndex)
                },
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

@Composable
private fun RememberParsedMarkdown(plain: String): State {
    val source = remember(plain) { normalizeMarkdownLineEndings(plain) }
    return rememberMarkdownState(content = source, immediate = true).state.collectAsState().value
}

@Composable
private fun MarkdownDocumentContent(
    result: State.Success,
    components: MarkdownComponents,
    modifier: Modifier,
    blockIndex: Int?,
) {
    if (blockIndex == null) {
        MarkdownSuccess(result, components, modifier)
    } else {
        Column(modifier) {
            MarkdownElement(result.node.children[blockIndex], components, result.content)
        }
    }
}

@Composable
fun imageRequestInMarkdown(info: FileInfo?): ImageRequest {
    val client = LocalClient.current
    val context = LocalPlatformContext.current
    return imageRequest(context, client, info).build()
}

@Composable
private fun ReadingMarkdownTable(model: MarkdownComponentModel) {
    MarkdownTable(
        content = model.content,
        node = model.node,
        style = model.typography.table,
        headerBlock = { content, header, width, style ->
            MarkdownTableHeader(content, header, width, style, maxLines = Int.MAX_VALUE)
        },
        rowBlock = { content, row, width, style ->
            MarkdownTableRow(content, row, width, style, maxLines = Int.MAX_VALUE)
        },
    )
}
