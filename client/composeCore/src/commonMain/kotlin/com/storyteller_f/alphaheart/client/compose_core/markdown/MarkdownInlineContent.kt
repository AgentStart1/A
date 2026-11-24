/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.markdown

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import com.hrm.latex.renderer.measure.LatexDimensions
import com.hrm.latex.renderer.measure.LatexMeasurerState
import com.hrm.latex.renderer.model.LatexConfig
import com.mikepenz.markdown.model.ImageTransformer
import com.mikepenz.markdown.model.MarkdownColors
import com.mikepenz.markdown.model.MarkdownTypography
import com.mikepenz.markdown.utils.getUnescapedTextInNode
import com.storyteller_f.alphaheart.client.compose_core.components.CustomMarkdownImage
import com.storyteller_f.alphaheart.client.compose_core.components.LatexInlineContent
import com.storyteller_f.alphaheart.client.compose_core.utils.pxToDp
import com.storyteller_f.shared.model.Dimension
import com.storyteller_f.shared.utils.readInlineMath
import kotlinx.collections.immutable.ImmutableMap
import org.intellij.markdown.IElementType
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes

fun ASTNode.findChildOfTypeRecursive(type: IElementType): ASTNode? {
    children.forEach {
        if (it.type == type) {
            return it
        } else {
            val found = it.findChildOfTypeRecursive(type)
            if (found != null) {
                return found
            }
        }
    }
    return null
}

fun imageInlineContent(
    uri: String,
    mediaMap: ImmutableMap<String, Dimension?>,
    maxWidth: Dp,
    density: Density,
    isEmbed: Boolean,
    transformer: ImageTransformer,
): InlineTextContent {
    val dimension = mediaMap[uri]
    if (maxWidth <= 10.dp || dimension == null) {
        return InlineTextContent(Placeholder(0.sp, 0.sp, PlaceholderVerticalAlign.Center)) {}
    }
    val imageWidth = pxToDp(dimension.width, density.density)
    val imageHeight = pxToDp(dimension.height, density.density)
    val width = minOf(maxWidth, imageWidth)
    val height = minOf(width.value / imageWidth.value * imageHeight, if (isEmbed) 300.dp else width * 2)
    val recalculatedWidth = height.value * imageWidth / imageHeight.value
    return InlineTextContent(
        Placeholder(recalculatedWidth.value.sp, height.value.sp, PlaceholderVerticalAlign.Center),
    ) {
        CompositionLocalProvider(LocalInspectionMode provides false) {
            transformer.transform(uri)?.let { imageData ->
                CustomMarkdownImage(imageData)
            }
        }
    }
}

internal data class MarkdownInlineOptions(
    val dimensionMap: ImmutableMap<String, Dimension?>,
    val imageTransformer: ImageTransformer,
    val maxWidth: Dp,
    val density: Density,
    val isEmbed: Boolean,
    val math: MarkdownInlineMath,
)

internal data class MarkdownInlineMath(
    val typography: MarkdownTypography,
    val colors: MarkdownColors,
    val measurer: LatexMeasurerState,
)

internal fun AnnotatedString.Builder.imageAnnotator(
    child: ASTNode,
    content: String,
    inlineContentMap: MutableMap<String, InlineTextContent>,
    options: MarkdownInlineOptions,
): Boolean {
    val childType = child.type
    return when (childType) {
        MarkdownElementTypes.IMAGE -> {
            val id = "image${child.startOffset}-${child.endOffset}"
            val name =
                child.findChildOfTypeRecursive(MarkdownElementTypes.LINK_DESTINATION)
                    ?.getUnescapedTextInNode(content)
            if (name != null) {
                inlineContentMap[id] =
                    imageInlineContent(
                        uri = name,
                        mediaMap = options.dimensionMap,
                        maxWidth = options.maxWidth,
                        density = options.density,
                        isEmbed = options.isEmbed,
                        transformer = options.imageTransformer,
                    )
                appendInlineContent(id, name)
                true
            } else {
                false
            }
        }

        GFMElementTypes.INLINE_MATH, GFMElementTypes.BLOCK_MATH -> {
            addMathContent(
                child = child,
                content = content,
                inlineContentMap = inlineContentMap,
                options = options,
            )
            true
        }

        else -> {
            false
        }
    }
}

private fun AnnotatedString.Builder.addMathContent(
    child: ASTNode,
    content: String,
    inlineContentMap: MutableMap<String, InlineTextContent>,
    options: MarkdownInlineOptions,
) {
    val (typography, colors, latexMeasurer) = options.math
    val density = options.density
    val maxWidth = options.maxWidth
    val tex = readInlineMath(child, content)
    val id = "math${child.startOffset}-${child.endOffset}"
    val style =
        if (child.type == GFMElementTypes.INLINE_MATH) {
            typography.inlineCode.copy(background = colors.inlineCodeBackground)
        } else {
            typography.code
        }

    val config = LatexConfig(fontSize = style.fontSize)
    val dimensions = latexMeasurer.measure(tex, config)
    val fallbackFontSizePx = with(density) { style.fontSize.toPx() }
    val placeholderSize =
        calculateMathPlaceholderSize(
            dimensions = dimensions,
            isInline = child.type == GFMElementTypes.INLINE_MATH,
            maxWidthPx = with(density) { maxWidth.toPx() },
            fallbackFontSizePx = fallbackFontSizePx,
            contentLength = tex.length,
        )

    val bgColor = style.background.takeIf { it.toArgb() != 0 }

    inlineContentMap[id] =
        InlineTextContent(
            Placeholder(
                width = with(density) { placeholderSize.widthPx.toSp() },
                height = with(density) { placeholderSize.heightPx.toSp() },
                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
            ),
        ) {
            LatexInlineContent(tex, style, bgColor)
        }

    if (child.type == GFMElementTypes.INLINE_MATH) {
        appendInlineContent(id, tex)
    } else {
        val paragraphStyle = ParagraphStyle()
        pushStyle(paragraphStyle)
        appendInlineContent(id, tex)
        pop()
    }
}

internal data class MathPlaceholderSize(val widthPx: Float, val heightPx: Float)

internal fun calculateMathPlaceholderSize(
    dimensions: LatexDimensions?,
    isInline: Boolean,
    maxWidthPx: Float,
    fallbackFontSizePx: Float,
    contentLength: Int,
): MathPlaceholderSize {
    val widthPx =
        if (isInline) {
            dimensions?.widthPx ?: fallbackFontSizePx * (1 + contentLength * 0.8f)
        } else {
            maxWidthPx
        }
    val heightPx = dimensions?.heightPx ?: fallbackFontSizePx * 1.5f
    return MathPlaceholderSize(widthPx, heightPx)
}
