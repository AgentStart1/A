/*
 * This is a private project. All rights reserved.
 */

package com.storyteller_f.alphaheart.client.compose_core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.hrm.latex.renderer.Latex
import com.hrm.latex.renderer.model.LatexConfig
import com.mikepenz.markdown.m3.markdownTypography
import com.mikepenz.markdown.model.MarkdownColors
import com.mikepenz.markdown.model.MarkdownTypography
import com.storyteller_f.alphaheart.client.compose_core.theme.LocalTopicContentFonts
import com.storyteller_f.alphaheart.client.compose_core.utils.withDefaultFontFamily

@Composable
fun customMarkdownTypography(colors: MarkdownColors): MarkdownTypography {
    val fonts = LocalTopicContentFonts.current
    val typography = MaterialTheme.typography.withDefaultFontFamily(fonts.content)
    return markdownTypography(
        h1 = typography.headlineSmall,
        h2 = typography.titleLarge,
        h3 = typography.titleMedium,
        h4 = typography.titleMedium,
        h5 = typography.titleSmall,
        h6 = typography.titleSmall,
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
