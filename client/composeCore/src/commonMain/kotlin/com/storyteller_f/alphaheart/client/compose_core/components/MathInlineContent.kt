/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import com.hrm.latex.renderer.Latex
import com.hrm.latex.renderer.measure.LatexDimensions
import com.hrm.latex.renderer.measure.LatexMeasurerState
import com.hrm.latex.renderer.model.LatexConfig
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.ImageTransformer
import com.mikepenz.markdown.model.MarkdownColors
import com.mikepenz.markdown.model.MarkdownTypography
import com.mikepenz.markdown.utils.getUnescapedTextInNode
import com.storyteller_f.alphaheart.client.compose_core.utils.withDefaultFontFamily
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

@Composable
fun customMarkdownTypography(colors: MarkdownColors): MarkdownTypography {
    val fonts = LocalTopicContentFonts.current
    val typography = MaterialTheme.typography.withDefaultFontFamily(fonts.content)
    return markdownTypography(
        h1 = typography.displayLarge,
        h2 = typography.displayMedium,
        h3 = typography.displaySmall,
        h4 = typography.headlineMedium,
        h5 = typography.headlineSmall,
        h6 = typography.titleLarge,
        text = typography.bodyLarge,
        paragraph = typography.bodyLarge,
        ordered = typography.bodyLarge,
        bullet = typography.bodyLarge,
        list = typography.bodyLarge,
        quote = typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
        table = typography.bodyLarge,
        alertTitle = typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
        textLink =
        TextLinkStyles(
            style =
            typography.bodyLarge.copy(fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)
                .toSpanStyle(),
        ),
        code = typography.bodyMedium.copy(fontFamily = fonts.code ?: FontFamily.Monospace, color = colors.text),
        inlineCode = typography.bodyLarge.copy(fontFamily = fonts.code ?: FontFamily.Monospace, color = colors.text),
    )
}

@Composable
fun LatexInlineContent(tex: String, style: TextStyle, backgroundColor: Color? = null) {
    if (backgroundColor != null) {
        Box(modifier = Modifier.background(backgroundColor)) {
            Latex(
                latex = tex,
                config = LatexConfig(fontSize = style.fontSize),
            )
        }
    } else {
        Latex(
            latex = tex,
            config = LatexConfig(fontSize = style.fontSize),
        )
    }
}

fun AnnotatedString.Builder.imageAnnotator(
    child: ASTNode,
    content: String,
    inlineContentMap: MutableMap<String, InlineTextContent>,
    dimensionMap: ImmutableMap<String, Dimension?>,
    maxWidth: Dp,
    density: Density,
    isEmbed: Boolean,
    imageTransformer: ImageTransformer,
    typography: MarkdownTypography,
    colors: MarkdownColors,
    latexMeasurer: LatexMeasurerState,
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
                        mediaMap = dimensionMap,
                        maxWidth = maxWidth,
                        density = density,
                        isEmbed = isEmbed,
                        transformer = imageTransformer,
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
                typography = typography,
                colors = colors,
                density = density,
                maxWidth = maxWidth,
                inlineContentMap = inlineContentMap,
                latexMeasurer = latexMeasurer,
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
    typography: MarkdownTypography,
    colors: MarkdownColors,
    density: Density,
    maxWidth: Dp,
    inlineContentMap: MutableMap<String, InlineTextContent>,
    latexMeasurer: LatexMeasurerState,
) {
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
